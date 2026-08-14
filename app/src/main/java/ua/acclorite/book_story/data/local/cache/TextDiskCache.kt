/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.cache

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.domain.model.reader.ReaderText
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextDiskCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CACHE_DIR_NAME = "text_cache"
        private const val CACHE_VERSION = 1
        private const val TYPE_CHAPTER = 0
        private const val TYPE_TEXT = 1
        private const val TYPE_SEPARATOR = 2
        private const val TYPE_IMAGE = 3
        private const val TYPE_HTML_MEDIA = 4
    }

    private val cacheDir by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).also { if (!it.exists()) it.mkdirs() }
    }

    private fun cacheFile(bookId: Int) = File(cacheDir, "$bookId.bin")

    suspend fun save(bookId: Int, text: List<ReaderText>) {
        withContext(Dispatchers.IO) {
            try {
                val file = cacheFile(bookId)
                DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { out ->
                    out.writeByte(CACHE_VERSION)
                    out.writeInt(text.size)
                    text.forEach { item ->
                        when (item) {
                            is ReaderText.Chapter -> {
                                out.writeByte(TYPE_CHAPTER)
                                writeString(out, item.title)
                                out.writeBoolean(item.nested)
                            }
                            is ReaderText.Text -> {
                                out.writeByte(TYPE_TEXT)
                                writeString(out, item.line.text)
                            }
                            is ReaderText.Separator -> {
                                out.writeByte(TYPE_SEPARATOR)
                            }
                            is ReaderText.Image -> {
                                out.writeByte(TYPE_IMAGE)
                                writeString(out, item.filePath ?: "")
                            }
                            is ReaderText.HtmlMedia -> {
                                out.writeByte(TYPE_HTML_MEDIA)
                                writeString(out, item.htmlContent)
                                writeString(out, item.cacheDir)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail - cache is best-effort
            }
        }
    }

    suspend fun load(bookId: Int): List<ReaderText>? {
        return withContext(Dispatchers.IO) {
            val file = cacheFile(bookId)
            if (!file.exists()) return@withContext null
            try {
                DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                    val version = input.readByte().toInt()
                    if (version != CACHE_VERSION) {
                        file.delete()
                        return@withContext null
                    }
                    val size = input.readInt()
                    val result = ArrayList<ReaderText>(size)
                    repeat(size) {
                        when (input.readByte().toInt()) {
                            TYPE_CHAPTER -> {
                                result.add(ReaderText.Chapter(
                                    title = readString(input),
                                    nested = input.readBoolean()
                                ))
                            }
                            TYPE_TEXT -> {
                                val text = readString(input)
                                result.add(ReaderText.Text(
                                    line = buildAnnotatedString { append(text) }
                                ))
                            }
                            TYPE_SEPARATOR -> {
                                result.add(ReaderText.Separator)
                            }
                            TYPE_IMAGE -> {
                                val path = readString(input)
                                result.add(ReaderText.Image(
                                    filePath = if (path.isEmpty()) null else path
                                ))
                            }
                            TYPE_HTML_MEDIA -> {
                                result.add(ReaderText.HtmlMedia(
                                    htmlContent = readString(input),
                                    cacheDir = readString(input)
                                ))
                            }
                        }
                    }
                    result
                }
            } catch (e: Exception) {
                file.delete()
                null
            }
        }
    }

    fun hasCache(bookId: Int): Boolean {
        return cacheFile(bookId).exists()
    }

    fun remove(bookId: Int) {
        cacheFile(bookId).delete()
    }

    private fun writeString(out: DataOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        out.writeInt(bytes.size)
        out.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val size = input.readInt()
        val bytes = ByteArray(size)
        input.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}
