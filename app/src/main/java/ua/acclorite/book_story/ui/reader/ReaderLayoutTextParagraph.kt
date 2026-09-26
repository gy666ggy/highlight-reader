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
    paragraphPrefixMode: Boolean = false,
    paragraphPrefixRules: List<ParagraphPrefixRule> = emptyList(),
    onParagraphPrefixClick: (String, Int) -> Unit = { _, _ -> },
    toolbarHidden: Boolean,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    menuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit
) {
    // 修改高亮：只对引号内容着色，非引号内容保持原色
    val effectiveDialogueColor = overrideColor ?: dialogueHighlightColor

    // 追踪 TextLayoutResult 用于点击定位字符
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val anyEditMode = punctuationEditMode || textReplaceMode || paragraphPrefixMode || modifyHighlightMode

    // 编辑/高亮模式下的统一点击处理器：直接放在 StyledText 上，确保事件不被父级拦截
    val editTapModifier = if (anyEditMode) {
        Modifier.pointerInput(
            paragraph,
            modifyHighlightMode,
            punctuationEditMode, punctuationRules,
            textReplaceMode, textReplaceRules,
            paragraphPrefixMode, paragraphPrefixRules
        ) {
            detectTapGestures { offset ->
                val result = layoutResult.value ?: return@detectTapGestures
                val text = paragraph.line.text
                if (text.isEmpty()) return@detectTapGestures

                val charOffset = result.getOffsetForPosition(offset)
                    .coerceIn(0, text.length - 1)
                val clickedChar = text[charOffset].toString()
                var handled = false

                // 1. 段首添加：点击段落开头区域（前10%宽度）→ 应用段首添加规则
                if (!handled && paragraphPrefixMode) {
                    val width = result.size.width
                    val enabledStartRules = paragraphPrefixRules.any { it.enabled && it.type == "start" }
                    if (width > 0 && offset.x <= width * 0.1f && enabledStartRules) {
                        onParagraphPrefixClick("start", -1)
                        handled = true
                    }
                }

                // 2. 段首添加：点击指定字符 → 应用字符后添加规则
                if (!handled && paragraphPrefixMode) {
                    val enabledAfterTriggers = paragraphPrefixRules
                        .filter { it.enabled && it.type == "after" }
                        .map { it.trigger }
                        .toSet()
                    if (clickedChar in enabledAfterTriggers) {
                        onParagraphPrefixClick("after", charOffset)
                        handled = true
                    }
                }

                // 3. 文字替换（更具体的匹配优先）
                if (!handled && textReplaceMode) {
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
                                handled = true
                                break
                            }
                            searchFrom = pos + 1
                        }
                        if (handled) break
                    }
                }

                // 4. 标点编辑
                if (!handled && punctuationEditMode) {
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
                                handled = true
                                break
                            }
                            searchFrom = pos + 1
                        }
                        if (handled) break
                    }
                }

                // 5. 修改高亮：点击段落任意位置 → 切换段落高亮色
                if (!handled && modifyHighlightMode) {
                    onParagraphClick()
                    handled = true
                }

                // 6. 没有匹配到任何规则且不在高亮模式 → 切换功能栏
                if (!handled) {
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
            .padding(horizontal = sidePadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalAlignment
    ) {
        StyledText(
            text = paragraph.line.withDialogueHighlight(effectiveDialogueColor),
            modifier = Modifier.then(
                if (anyEditMode) {
                    // 编辑/高亮模式：使用统一的 pointerInput 处理点击
                    editTapModifier
                } else if (doubleClickTranslation && toolbarHidden) {
                    // 普通模式：双击翻译 + 单击切换功能栏
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
                } else if (toolbarHidden) {
                    // 普通模式：单击切换功能栏
                    Modifier.noRippleClickable {
                        menuVisibility(
                            ReaderEvent.OnMenuVisibility(
                                show = !showMenu,
                                saveCheckpoint = true
                            )
                        )
                    }
                } else {
                    Modifier
                }
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
