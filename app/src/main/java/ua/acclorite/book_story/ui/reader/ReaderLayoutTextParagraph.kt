/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import ua.acclorite.book_story.domain.model.reader.ReaderText.Text
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.presentation.reader.model.ReaderFontThickness
import ua.acclorite.book_story.presentation.reader.model.ReaderTextAlignment
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.helpers.noRippleClickable
import ua.acclorite.book_story.ui.reader.model.FontWithName

@Composable
fun LazyItemScope.ReaderLayoutTextParagraph(
    paragraph: Text,
    showMenu: Boolean,
    fontFamily: FontWithName,
    fontColor: Color,
    lineHeight: TextUnit,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    textAlignment: ReaderTextAlignment,
    horizontalAlignment: Alignment.Horizontal,
    fontSize: TextUnit,
    letterSpacing: TextUnit,
    sidePadding: Dp,
    paragraphIndentation: TextUnit,
    doubleClickTranslation: Boolean,
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight,
    dialogueHighlightColor: Color,
    overrideColor: Color? = null,
    modifyHighlightMode: Boolean = false,
    punctuationEditMode: Boolean = false,
    punctuationRules: List<Triple<String, String, Boolean>> = emptyList(),
    onParagraphClick: () -> Unit = {},
    onPunctuationClick: (Int) -> Unit = {},
    textReplaceMode: Boolean = false,
    textReplaceRules: List<Triple<String, String, Boolean>> = emptyList(),
    onTextReplaceClick: (Int) -> Unit = {},
    toolbarHidden: Boolean,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    menuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit
) {
    // 修改高亮：只对引号内容着色，非引号内容保持原色
    val effectiveDialogueColor = overrideColor ?: dialogueHighlightColor

    // 在标点编辑或文字替换模式下追踪 TextLayoutResult
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val tapModifier = if (punctuationEditMode || textReplaceMode) {
        Modifier.pointerInput(paragraph, punctuationEditMode, punctuationRules, textReplaceMode, textReplaceRules) {
            detectTapGestures { offset ->
                val result = layoutResult.value ?: return@detectTapGestures
                val text = paragraph.line.text
                if (text.isEmpty()) return@detectTapGestures
                val charOffset = result.getOffsetForPosition(offset)
                    .coerceIn(0, text.length - 1)

                var matched = false

                // 文字替换优先（更具体的匹配）
                if (textReplaceMode) {
                    for (rule in textReplaceRules) {
                        val fromText = rule.first
                        if (fromText.isEmpty()) continue
                        var searchFrom = 0
                        while (true) {
                            val pos = text.indexOf(fromText, searchFrom)
                            if (pos < 0) break
                            val end = pos + fromText.length
                            if (charOffset in pos until end) {
                                onTextReplaceClick(pos)
                                matched = true
                                break
                            }
                            searchFrom = pos + 1
                        }
                        if (matched) break
                    }
                }

                // 标点编辑（如果文字替换没匹配上）
                if (!matched && punctuationEditMode) {
                    for (rule in punctuationRules) {
                        val fromPunc = rule.first
                        if (fromPunc.isEmpty()) continue
                        var searchFrom = 0
                        while (true) {
                            val pos = text.indexOf(fromPunc, searchFrom)
                            if (pos < 0) break
                            val end = pos + fromPunc.length
                            if (charOffset in pos until end) {
                                onPunctuationClick(pos)
                                matched = true
                                break
                            }
                            searchFrom = pos + 1
                        }
                        if (matched) break
                    }
                }

                // 没有匹配到任何标点/文字 → 切换功能栏
                if (!matched) {
                    menuVisibility(
                        ReaderEvent.OnMenuVisibility(
                            show = !showMenu,
                            saveCheckpoint = true
                        )
                    )
                }
            }
        }
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .animateItem(fadeInSpec = null, fadeOutSpec = null)
            .fillMaxWidth()
            .padding(horizontal = sidePadding)
            .then(
                if (modifyHighlightMode && !punctuationEditMode && !textReplaceMode) {
                    Modifier.noRippleClickable { onParagraphClick() }
                } else Modifier
            )
            .then(tapModifier),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalAlignment
    ) {
        StyledText(
            text = paragraph.line.withDialogueHighlight(effectiveDialogueColor),
            modifier = Modifier.then(
                if (!modifyHighlightMode && doubleClickTranslation && toolbarHidden) {
                    Modifier.noRippleClickable(
                        onDoubleClick = {
                            openTranslator(
                                ReaderEvent.OnOpenTranslator(
                                    textToTranslate = paragraph.line.text,
                                    translateWholeParagraph = true
                                )
                            )
                        },
                        onClick = {
                            menuVisibility(
                                ReaderEvent.OnMenuVisibility(
                                    show = !showMenu,
                                    saveCheckpoint = true
                                )
                            )
                        }
                    )
                } else if (!modifyHighlightMode && !doubleClickTranslation && toolbarHidden) {
                    Modifier.noRippleClickable {
                        menuVisibility(
                            ReaderEvent.OnMenuVisibility(
                                show = !showMenu,
                                saveCheckpoint = true
                            )
                        )
                    }
                } else Modifier
            ),
            style = TextStyle(
                fontFamily = fontFamily.font,
                fontWeight = fontThickness.thickness,
                textAlign = textAlignment.textAlignment,
                textIndent = TextIndent(firstLine = paragraphIndentation),
                fontStyle = fontStyle,
                letterSpacing = letterSpacing,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = fontColor,
                lineBreak = LineBreak.Paragraph
            ),
            highlightText = highlightedReading,
            highlightThickness = highlightedReadingThickness,
            onTextLayout = { result -> layoutResult.value = result }
        )
    }
}
