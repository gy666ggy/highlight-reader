/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.text

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import ua.acclorite.book_story.core.helpers.clearAllMarkdown
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.document.MarkdownParser
import ua.acclorite.book_story.domain.model.reader.ReaderText
import javax.inject.Inject

private const val TAG = "TxtTextParser"

class TxtTextParser @Inject constructor(
    private val markdownParser: MarkdownParser
) : TextParser {

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        logI(TAG, "Started TXT parsing: ${cachedFile.name}.")

        return try {
            val readerText = mutableListOf<ReaderText>()
            var chapterAdded = false
            // 章节标题正则：支持 第X章/节/卷/回/部/集/篇/幕、Chapter X、数字. 、以及楔子/序章/引子/尾声/后记/番外/前言 等特殊标题
            // \s 补充全角空格 \u3000 和零宽字符，避免标题行带不可见字符时匹配失败
            val chapterTitleRegex = Regex(
                pattern = """^[\s\u3000\u200B\u200C\u200D\uFEFF]*""" +
                    """(""" +
                    """(第\s*[零〇一二三四五六七八九十百千万\d０-９]+\s*[章节卷回部集篇幕][^，。！？]*)""" +
                    """|(Chapter\s+\d+[^，。！？]*)""" +
                    """|(\d+\s*[.、]\s*[^，。！？]+)""" +
                    """|(楔子|引子|序章|序言|前言|引言|尾声|后记|番外|外传|终章|大结局)\s*""" +
                    """)""" +
                    """[\s\u3000\u200B\u200C\u200D\uFEFF]*$""",
                options = setOf(RegexOption.IGNORE_CASE)
            )

            withContext(Dispatchers.IO) {
                cachedFile.openInputStream()?.bufferedReader()?.use { reader ->
                    reader.forEachLine { line ->
                        if (line.isNotBlank()) {
                            when (line) {
                                "***", "---" -> readerText.add(
                                    ReaderText.Separator
                                )

                                else -> {
                                    // 清理 markdown 标记 + 全角空格 + 零宽字符 + BOM
                                    val cleanLine = line.clearAllMarkdown()
                                        .replace(Regex("[\u3000\u200B\u200C\u200D\uFEFF]"), "")
                                        .trim()
                                    if (cleanLine.isNotBlank() && chapterTitleRegex.matches(cleanLine)) {
                                        readerText.add(
                                            ReaderText.Chapter(
                                                title = cleanLine,
                                                nested = false
                                            )
                                        )
                                        chapterAdded = true
                                    } else {
                                        if (!chapterAdded && cleanLine.isNotBlank()) {
                                            readerText.add(
                                                0, ReaderText.Chapter(
                                                    title = cleanLine,
                                                    nested = false
                                                )
                                            )
                                            chapterAdded = true
                                        }
                                        readerText.add(
                                        ReaderText.Text(
                                            line = markdownParser.parse(line)
                                        )
                                    )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            yield()

            if (
                readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
                readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
            ) {
                logE(TAG, "Could not extract text from TXT.")
                return emptyList()
            }

            logI(TAG, "Successfully finished TXT parsing.")
            readerText
        } catch (e: Exception) {
            logE(TAG, "Could not parse text with message: ${e.message}.")
            emptyList()
        }
    }
}
