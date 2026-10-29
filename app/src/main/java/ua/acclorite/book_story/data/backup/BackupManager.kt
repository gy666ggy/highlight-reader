/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.data.model.file.CachedFileCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份管理器：导出/导入 app 内部数据（不含 TXT 书籍文件）
 *
 * 导出内容：
 *   - Room 数据库（书籍、历史、颜色预设、分类）
 *   - SharedPreferences（全局设置、每本书的书签、高亮色备份）
 *   - paragraph_colors/ 目录（段落高亮色主存）
 *   - covers/ 目录（封面图片）
 *   - datastore/ 目录（阅读器设置：字体、字号、行高等）
 *
 * 不包含：
 *   - TXT 书籍文件（需用户手动复制）
 *   - SAF 持久化权限（需用户在新手机重新授权）
 *
 * 导入策略：整体覆盖。
 * 导入不会直接改动正在运行的进程数据，而是把备份解压到 pending_restore/，
 * 由 [Application] 在下次启动、Room/DataStore 初始化之前一次性应用，
 * 之后再按文件名把旧手机的 filePath 重映射到新手机上的同名文件。
 */
object BackupManager {

    private const val BACKUP_VERSION = 1
    private const val MANIFEST_FILE = "backup_manifest.json"

    private const val PENDING_DIR = "pending_restore"
    private const val PENDING_MARKER = "restore.ready"

    private val DB_FILES = listOf("book_db", "book_db-journal", "book_db-wal", "book_db-shm")

    /**
     * 导出备份到指定 URI（SAF 选文件后的 content:// URI）
     */
    suspend fun exportToUri(context: Context, uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                exportToStream(context, output)
            } ?: throw IllegalStateException("无法打开输出流")
        }
    }

    private fun exportToStream(context: Context, output: OutputStream) {
        val filesDir = context.filesDir
        val dbDir = File(context.getDatabasePath("book_db").parentFile!!.absolutePath)
        val spDir = File(context.applicationInfo.dataDir, "shared_prefs")

        ZipOutputStream(output.buffered()).use { zos ->
            // 1. 写入 manifest（版本号，供导入时校验）
            val manifest = """{"version":$BACKUP_VERSION,"app":"book_story"}"""
            zos.putNextEntry(ZipEntry(MANIFEST_FILE))
            zos.write(manifest.toByteArray())
            zos.closeEntry()

            // 2. Room 数据库
            for (name in DB_FILES) {
                val f = File(dbDir, name)
                if (f.exists()) addFileToZip(zos, f, "databases/$name")
            }

            // 3. SharedPreferences（全局 + 每本书的）
            if (spDir.exists()) {
                spDir.listFiles()?.forEach { f ->
                    if (f.name.endsWith(".xml")) {
                        addFileToZip(zos, f, "shared_prefs/${f.name}")
                    }
                }
            }

            // 4. paragraph_colors/
            val colorsDir = File(filesDir, "paragraph_colors")
            if (colorsDir.exists()) addDirToZip(zos, colorsDir, "paragraph_colors/")

            // 5. covers/
            val coversDir = File(filesDir, "covers")
            if (coversDir.exists()) addDirToZip(zos, coversDir, "covers/")

            // 6. datastore/
            val datastoreDir = File(filesDir, "datastore")
            if (datastoreDir.exists()) addDirToZip(zos, datastoreDir, "datastore/")
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        zos.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { it.copyTo(zos) }
        zos.closeEntry()
    }

    private fun addDirToZip(zos: ZipOutputStream, dir: File, basePath: String) {
        dir.listFiles()?.forEach { f ->
            val entryName = basePath + f.name
            if (f.isDirectory) {
                addDirToZip(zos, f, "$entryName/")
            } else {
                addFileToZip(zos, f, entryName)
            }
        }
    }

    /**
     * 从指定 URI 导入备份。
     *
     * 只做校验 + 解压到 pending_restore/，不触碰正在运行的数据。
     * 用户确认后需重启 app，由 [applyPendingRestore] 在下次启动时应用。
     */
    suspend fun importFromUri(context: Context, uri: Uri): ImportResult = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                stageRestore(context, input)
            } ?: throw IllegalStateException("无法打开输入流")
        }
    }.getOrElse { ImportResult(success = false, message = it.message ?: "未知错误") }

    private fun stageRestore(context: Context, input: InputStream): ImportResult {
        val tempDir = File(context.cacheDir, "backup_import_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            ZipInputStream(input.buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { zis.copyTo(it) }
                    }
                    entry = zis.nextEntry
                }
            }

            // 校验 manifest
            if (!File(tempDir, MANIFEST_FILE).exists()) {
                return ImportResult(success = false, message = "无效的备份文件：缺少 manifest")
            }
            // 校验数据库
            if (!File(tempDir, "databases/book_db").exists()) {
                return ImportResult(success = false, message = "无效的备份文件：缺少数据库")
            }

            // 在临时副本上按文件名重映射 filePath（此时还没碰运行中的数据）
            runCatching { remapBookPaths(context, File(tempDir, "databases/book_db")) }

            // 把备份内容放进待恢复目录，最后写 marker（保证不完整不影响启动）
            val pendingDir = File(context.filesDir, PENDING_DIR)
            pendingDir.deleteRecursively()
            pendingDir.mkdirs()

            listOf("databases", "shared_prefs", "paragraph_colors", "covers", "datastore")
                .forEach { name ->
                    copyDir(File(tempDir, name), File(pendingDir, name))
                }

            File(pendingDir, PENDING_MARKER).writeText("1")

            return ImportResult(
                success = true,
                message = "备份已读入，重新打开应用后生效。\n\n" +
                        "请先确认：\n" +
                        "1. 旧手机的 TXT 已复制到本机\n" +
                        "2. 已在「浏览」中授权存放 TXT 的目录\n\n" +
                        "同名 TXT 会自动重新关联路径；未找到的书会打不开。"
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * 在 app 启动、Room/DataStore 初始化之前应用待恢复数据。
     * 由 [ua.acclorite.book_story.Application] 调用。
     */
    fun applyPendingRestore(context: Context) {
        runCatching {
            val pendingDir = File(context.filesDir, PENDING_DIR)
            if (!File(pendingDir, PENDING_MARKER).exists()) return

            val dbDir = File(context.getDatabasePath("book_db").parentFile!!.absolutePath)
            val filesDir = context.filesDir
            val spDir = File(context.applicationInfo.dataDir, "shared_prefs")

            // 1. 数据库：先删掉本机旧的 journal/wal/shm，避免和新库拼接成损坏状态
            for (name in DB_FILES) {
                File(dbDir, name).delete()
            }
            copyDir(File(pendingDir, "databases"), dbDir)

            // 2. 按文件名重映射 filePath（旧手机路径 → 新手机同名文件路径）
            runCatching { remapBookPaths(context, File(dbDir, "book_db")) }

            // 3. 其余数据
            copyDir(File(pendingDir, "shared_prefs"), spDir)
            copyDir(File(pendingDir, "paragraph_colors"), File(filesDir, "paragraph_colors"))
            copyDir(File(pendingDir, "covers"), File(filesDir, "covers"))
            copyDir(File(pendingDir, "datastore"), File(filesDir, "datastore"))

            pendingDir.deleteRecursively()
        }
    }

    /**
     * 在当前已授权的目录里按文件名找同名文件，把数据库里的 filePath 改写成新手机的路径。
     * 只改路径，不动 bookId，保证 paragraph_colors/book_{id}.dat 与 reader_extra_{id} 仍然对得上。
     */
    private fun remapBookPaths(context: Context, dbFile: File): Int {
        if (!dbFile.exists()) return 0

        val dirs = context.contentResolver.persistedUriPermissions.mapNotNull { permission ->
            CachedFileCompat.fromUri(context, permission.uri).takeIf { it.isDirectory }
        }
        if (dirs.isEmpty()) return 0

        var updated = 0
        val db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        try {
            val books = mutableListOf<Pair<Int, String>>()
            db.rawQuery("SELECT id, filePath FROM BookEntity", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val path = cursor.getString(1) ?: continue
                    books.add(cursor.getInt(0) to path)
                }
            }

            // 路径仍落在已授权目录下的书不用处理，避免无谓地遍历目录
            val unresolved = books.filter { (_, path) ->
                dirs.none { dir ->
                    dir.path.isNotEmpty() && path.startsWith(dir.path, ignoreCase = true)
                }
            }
            if (unresolved.isEmpty()) return 0

            val filesByName = HashMap<String, String>()
            dirs.forEach { dir ->
                dir.walk().forEach { file ->
                    if (!filesByName.containsKey(file.name)) {
                        filesByName[file.name] = file.path
                    }
                }
            }
            if (filesByName.isEmpty()) return 0

            unresolved.forEach { (id, oldPath) ->
                val newPath = filesByName[oldPath.substringAfterLast('/')] ?: return@forEach
                if (newPath != oldPath) {
                    db.execSQL(
                        "UPDATE BookEntity SET filePath = ? WHERE id = ?",
                        arrayOf(newPath, id)
                    )
                    updated++
                }
            }
        } finally {
            db.close()
        }
        return updated
    }

    private fun copyDir(src: File, dst: File) {
        if (!src.exists()) return
        dst.mkdirs()
        src.listFiles()?.forEach { f ->
            if (f.isDirectory) {
                copyDir(f, File(dst, f.name))
            } else {
                f.copyTo(File(dst, f.name), overwrite = true)
            }
        }
    }

    data class ImportResult(
        val success: Boolean,
        val message: String
    )
}
