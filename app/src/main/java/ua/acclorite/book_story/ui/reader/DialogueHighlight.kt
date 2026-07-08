/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

private val DialogueHighlightColor = Color(0xFF1565C0)

fun AnnotatedString.withDialogueHighlight(): AnnotatedString {
    val original = this
    val ranges = original.text.dialogueInnerRanges()
    if (ranges.isEmpty()) return original

    return buildAnnotatedString {
        append(original)

        original.spanStyles.forEach { range ->
            addStyle(range.item, range.start, range.end)
        }
        original.paragraphStyles.forEach { range ->
            addStyle(range.item, range.start, range.end)
        }
        original.getStringAnnotations(0, original.length).forEach { annotation ->
            addStringAnnotation(
                tag = annotation.tag,
                annotation = annotation.item,
                start = annotation.start,
                end = annotation.end
            )
        }

        ranges.forEach { range ->
            addStyle(
                style = SpanStyle(color = DialogueHighlightColor),
                start = range.first,
                end = range.last + 1
            )
        }
    }
}

private fun String.dialogueInnerRanges(): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var openIndex = -1

    forEachIndexed { index, char ->
        when (char) {
            '“' -> openIndex = index
            '”' -> {
                if (openIndex >= 0 && index > openIndex + 1) {
                    ranges += (openIndex + 1) until index
                }
                openIndex = -1
            }
        }
    }

    return ranges
}
