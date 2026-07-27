/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.reader

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import java.util.UUID

@Immutable
sealed class ReaderText {
    @Immutable
    data class Chapter(
        val id: UUID = UUID.randomUUID(),
        val title: String,
        val nested: Boolean
    ) : ReaderText()

    @Immutable
    data class Text(val line: AnnotatedString) : ReaderText()

    @Immutable
    data object Separator : ReaderText()

    @Immutable
    data class Image(
        val imageBitmap: ImageBitmap? = null,
        val filePath: String? = null  // 缓存中的文件路径，用于 Coil/GIF 加载
    ) : ReaderText()

    @Immutable
    data class HtmlMedia(
        val htmlContent: String,  // HTML片段，包含 <img>/<video>/<audio> 等
        val cacheDir: String      // 缓存目录绝对路径，资源文件解压到此处
    ) : ReaderText()
}