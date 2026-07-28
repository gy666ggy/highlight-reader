/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.yield
import org.jsoup.nodes.Document
import ua.acclorite.book_story.core.helpers.clearAllMarkdown
import ua.acclorite.book_story.core.helpers.clearMarkdown
import ua.acclorite.book_story.core.helpers.containsVisibleText
import ua.acclorite.book_story.domain.model.reader.ReaderText
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject

class DocumentParser @Inject constructor(
    private val markdownParser: MarkdownParser
) {
    /**
     * Parses document to get it's text.
     * Fixes issues such as manual line breaking in <p>.
     * Applies Markdown to the text: Bold(**), Italic(_), Section separator(---), and Links(a > href).
     *
     * @return Parsed text line by line with Markdown(all lines are not blank).
     */
    suspend fun parseDocument(
        document: Document,
        zipFile: ZipFile? = null,
        imageEntries: List<ZipEntry>? = null,
        includeChapter: Boolean = true,
        cacheDir: String? = null,
        currentHtmlFile: String? = null
    ): List<ReaderText> {
        yield()

        val readerText = mutableListOf<ReaderText>()
        var chapterAdded = false

        document.selectFirst("body")
            .run { this ?: document.body() }
            .apply {
                // Remove manual line breaks from all <p>, <a>
                select("p").forEach { element ->
                    yield()
                    element.html(element.html().replace(Regex("\\n+"), " "))
                    element.append("\n")
                }
                select("a").forEach { element ->
                    yield()
                    element.html(element.html().replace(Regex("\\n+"), ""))
                }

                // Remove <head>'s title
                select("title").remove()

                // Remove dangerous tags
                select("script, style").remove()

                // Markdown
                select("hr").append("\n---\n")
                select("b").append("**").prepend("**")
                select("h1").append("**").prepend("**")
                select("h2").append("**").prepend("**")
                select("h3").append("**").prepend("**")
                select("strong").append("**").prepend("**")
                select("em").append("_").prepend("_")
                select("a").forEach { element ->
                    var link = element.attr("href")
                    if (!link.startsWith("http") || element.wholeText().isBlank()) return@forEach

                    if (link.startsWith("http://")) {
                        link = link.replaceFirst("http://", "https://")
                    }

                    element.prepend("[")
                    element.append("]($link)")
                }

                // Image (<img>)
                select("img").forEach { element ->
                    val rawSrc = element.attr("src").trim()
                    if (rawSrc.isBlank()) return@forEach

                    val decodedSrc = URLDecoder.decode(rawSrc, StandardCharsets.UTF_8.name())

                    // 查找匹配的 ZIP 条目
                    val matchedEntry = imageEntries?.find { image ->
                        val imageName = image.name.substringAfterLast(File.separator).lowercase()
                        decodedSrc.substringAfterLast("/").lowercase().contains(imageName) ||
                        imageName.contains(decodedSrc.substringAfterLast("/").lowercase())
                    }

                    if (matchedEntry == null) return@forEach

                    val isAnimated = matchedEntry.name.endsWith(".gif", ignoreCase = true) ||
                            matchedEntry.name.endsWith(".webp", ignoreCase = true)

                    if (isAnimated && cacheDir != null && zipFile != null) {
                        // GIF/WebP：提取到缓存，后续用 Coil 加载（支持动画）
                        val targetFile = File(cacheDir, matchedEntry.name.substringAfterLast(File.separator))
                        if (!targetFile.exists()) {
                            zipFile.getInputStream(matchedEntry).use { input ->
                                targetFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                        element.append("\n[[GIF_FILE:${targetFile.absolutePath}|Image]]\n")
                    } else {
                        // 普通图片：保持原有逻辑
                        val src = decodedSrc.substringAfterLast(File.separator).lowercase()
                            .takeIf {
                                it.containsVisibleText() && imageEntries.any { image ->
                                    it == image.name.substringAfterLast(File.separator).lowercase()
                                }
                            } ?: return@forEach
                        val alt = element.attr("alt").trim().takeIf {
                            it.clearMarkdown().containsVisibleText()
                        } ?: "Image"
                        element.append("\n[[$src|$alt]]\n")
                    }
                }

                // Image (<image>)
                select("image").forEach { element ->
                    val src = element.attr("xlink:href")
                        .trim()
                        .substringAfterLast(File.separator)
                        .lowercase()
                        .let { src -> URLDecoder.decode(src, StandardCharsets.UTF_8.name()) }
                        .takeIf {
                            it.containsVisibleText() && imageEntries?.any { image ->
                                it == image.name.substringAfterLast(File.separator).lowercase()
                            } == true
                        } ?: return@forEach

                    val alt = "Image"

                    element.append("\n[[$src|$alt]]\n")
                }

                // Video/Audio 媒体保留
                val mediaElements = mutableListOf<String>()

                select("video, audio").forEach { element ->
                    val clonedElement = element.clone()
                    // 清理安全属性
                    clonedElement.select("script, style, link[rel=stylesheet]").remove()
                    mediaElements.add(clonedElement.outerHtml())
                    element.remove()  // 从文档中移除，避免 wholeText() 处理
                }

                // 如果有媒体元素，生成 HtmlMedia
                if (mediaElements.isNotEmpty() && cacheDir != null) {
                    // 提取资源文件到缓存
                    if (zipFile != null && imageEntries != null) {
                        extractMediaResources(zipFile, imageEntries, cacheDir, currentHtmlFile)
                    }
                    val mediaHtml = mediaElements.joinToString("\n")
                    readerText.add(ReaderText.HtmlMedia(
                        htmlContent = mediaHtml,
                        cacheDir = cacheDir
                    ))
                }
            }.wholeText().lines().forEach { line ->
                yield()

                val formattedLine = line.replace(
                    Regex("""\*\*\*\s*(.*?)\s*\*\*\*"""), "_**$1**_"
                ).replace(
                    Regex("""\*\*\s*(.*?)\s*\*\*"""), "**$1**"
                ).replace(
                    Regex("""_\s*(.*?)\s*_"""), "_$1_"
                ).trim()

                val imageRegex = Regex("""\[\[(.*?)\|(.*?)]]""")

                if (line.containsVisibleText()) {
                    when {
                        imageRegex.matches(line) -> {
                            val trimmedLine = line.removeSurrounding("[[", "]]")
                            val src = trimmedLine.substringBefore("|")
                            val alt = "_${trimmedLine.substringAfter("|")}_"

                            if (src.startsWith("GIF_FILE:")) {
                                // GIF/WebP 文件：用 Coil 加载
                                val gifPath = src.substringAfter("GIF_FILE:")
                                readerText.add(ReaderText.Image(imageBitmap = null, filePath = gifPath))
                            } else {
                                // 普通图片：原有逻辑
                                val image = try {
                                    val imageEntry = imageEntries?.find { image ->
                                        src == image.name.substringAfterLast(File.separator).lowercase()
                                    } ?: return@forEach
                                    zipFile?.getImage(imageEntry)?.asImageBitmap()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    null
                                } ?: return@forEach

                                image.prepareToDraw()
                                readerText.add( // Adding image
                                    ReaderText.Image(
                                        imageBitmap = image
                                    )
                                )
                                readerText.add( // Adding alternative text (caption) for image
                                    ReaderText.Text(
                                        markdownParser.parse(alt)
                                    )
                                )
                            }
                        }

                        line == "---" || line == "***" -> readerText.add(ReaderText.Separator)

                        else -> {
                            if (
                                !chapterAdded &&
                                formattedLine.clearAllMarkdown().containsVisibleText() &&
                                includeChapter
                            ) {
                                readerText.add(
                                    0, ReaderText.Chapter(
                                        title = formattedLine.clearAllMarkdown(),
                                        nested = false
                                    )
                                )
                                chapterAdded = true
                            } else if (
                                formattedLine.clearMarkdown().containsVisibleText()
                            ) {
                                readerText.add(
                                    ReaderText.Text(
                                        line = markdownParser.parse(formattedLine)
                                    )
                                )
                            }
                        }
                    }
                }
            }

        yield()

        if (
            readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
            (includeChapter && readerText.filterIsInstance<ReaderText.Chapter>().isEmpty())
        ) {
            return emptyList()
        }

        return readerText
    }

    /**
     * Getting bitmap from [ZipFile] with compression
     * that depends on the [imageEntry] size.
     */
    private fun ZipFile.getImage(imageEntry: ZipEntry): Bitmap? {
        fun getBitmapFromInputStream(compressionLevel: Int = 1): Bitmap? {
            return getInputStream(imageEntry).use { inputStream ->
                BitmapFactory.decodeStream(
                    inputStream,
                    null,
                    BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                        inSampleSize = compressionLevel
                    }
                )
            }
        }


        val uncompressedBitmap = getBitmapFromInputStream() ?: return null
        return uncompressedBitmap
    }

    /**
     * Extracts media resource files from ZIP to the cache directory.
     */
    private fun extractMediaResources(
        zipFile: ZipFile,
        mediaEntries: List<ZipEntry>,
        cacheDir: String,
        currentHtmlFile: String?
    ) {
        mediaEntries.forEach { entry ->
            val targetFile = File(cacheDir, entry.name.substringAfterLast(File.separator))
            if (!targetFile.exists()) {
                try {
                    zipFile.getInputStream(entry).use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}