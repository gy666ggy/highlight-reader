/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.backup

import android.content.Context
import android.net.Uri
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 */
object BackupManager {

    private const val BACKUP_VERSION = 1
    private const val MANIFEST_FILE = "backup_manifest.json"

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
            val dbFiles = listOf("book_db", "book_db-journal", "book_db-wal", "book_db-shm")
            for (name in dbFiles) {
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
     * 从指定 URI 导入备份
     * @param conflictStrategy 当新手机已有书时的处理策略
     */
    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        conflictStrategy: ConflictStrategy = ConflictStrategy.REPLACE
    ): ImportResult = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                importFromStream(context, input, conflictStrategy)
            } ?: throw IllegalStateException("无法打开输入流")
        }
    }.getOrElse { ImportResult(success = false, message = it.message ?: "未知错误") }

    private fun importFromStream(
        context: Context,
        input: InputStream,
        conflictStrategy: ConflictStrategy
    ): ImportResult {
        // 解压到临时目录
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
            val manifestFile = File(tempDir, MANIFEST_FILE)
            if (!manifestFile.exists()) {
                return ImportResult(success = false, message = "无效的备份文件：缺少 manifest")
            }

            // 应用数据
            val dbDir = File(context.getDatabasePath("book_db").parentFile!!.absolutePath)
            val filesDir = context.filesDir
            val spDir = File(context.applicationInfo.dataDir, "shared_prefs")

            // 1. 数据库
            val backupDbDir = File(tempDir, "databases")
            if (backupDbDir.exists()) {
                backupDbDir.listFiles()?.forEach { f ->
                    val target = File(dbDir, f.name)
                    f.copyTo(target, overwrite = true)
                }
            }

            // 2. SharedPreferences
            val backupSpDir = File(tempDir, "shared_prefs")
            if (backupSpDir.exists()) {
                backupSpDir.listFiles()?.forEach { f ->
                    val target = File(spDir, f.name)
                    when (conflictStrategy) {
                        ConflictStrategy.REPLACE -> f.copyTo(target, overwrite = true)
                        ConflictStrategy.MERGE -> {
                            // 合并模式：已有则跳过（避免覆盖新手机上不同书的数据）
                            if (!target.exists()) {
                                f.copyTo(target)
                            }
                        }
                    }
                }
            }

            // 3. paragraph_colors
            val backupColorsDir = File(tempDir, "paragraph_colors")
            if (backupColorsDir.exists()) {
                val targetDir = File(filesDir, "paragraph_colors")
                targetDir.mkdirs()
                backupColorsDir.listFiles()?.forEach { f ->
                    f.copyTo(File(targetDir, f.name), overwrite = true)
                }
            }

            // 4. covers
            val backupCoversDir = File(tempDir, "covers")
            if (backupCoversDir.exists()) {
                val targetDir = File(filesDir, "covers")
                targetDir.mkdirs()
                backupCoversDir.listFiles()?.forEach { f ->
                    f.copyTo(File(targetDir, f.name), overwrite = true)
                }
            }

            // 5. datastore
            val backupDatastoreDir = File(tempDir, "datastore")
            if (backupDatastoreDir.exists()) {
                val targetDir = File(filesDir, "datastore")
                targetDir.mkdirs()
                backupDatastoreDir.listFiles()?.forEach { f ->
                    f.copyTo(File(targetDir, f.name), overwrite = true)
                }
            }

            return ImportResult(
                success = true,
                message = "恢复完成！\n\n注意：\n1. 请手动将旧手机的 TXT 文件复制到本机\n2. 进入「浏览」选目录重新授权\n3. 重启 app 即可看到数据"
            )
        } finally {
            // 清理临时目录
            tempDir.deleteRecursively()
        }
    }

    enum class ConflictStrategy {
        /** 覆盖：备份数据完全替换本机数据 */
        REPLACE,
        /** 合并：本机已有数据保留，仅导入缺失部分 */
        MERGE
    }

    data class ImportResult(
        val success: Boolean,
        val message: String
    )
}
