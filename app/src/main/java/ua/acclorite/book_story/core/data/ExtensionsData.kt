/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.core.data

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

object ExtensionsData {
    val fileExtensions = persistentListOf(
        ".epub",
        ".pdf",
        ".fb2",
        ".txt",
        ".html",
        ".htm",
        ".md"
    )

    val imageExtensions = persistentListOf(
        ".png",
        ".jpg",
        ".jpeg",
        ".gif",
        ".webp",
        ".bmp",
        ".svg",
        ".tiff",
        ".tif",
        ".avif"
    )

    val videoExtensions = persistentListOf(
        ".mp4", ".webm", ".m4v", ".3gp", ".mkv", ".avi"
    )

    val audioExtensions = persistentListOf(
        ".mp3", ".ogg", ".m4a", ".aac", ".wav", ".flac"
    )

    val mediaExtensions = persistentListOf<String>().builder().apply {
        addAll(imageExtensions)
        addAll(videoExtensions)
        addAll(audioExtensions)
    }.build()
}