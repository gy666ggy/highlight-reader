/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.launch
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.presentation.reader.model.Checkpoint
import ua.acclorite.book_story.presentation.reader.model.ReaderFontThickness
import ua.acclorite.book_story.presentation.reader.model.ReaderHorizontalGesture
import ua.acclorite.book_story.presentation.reader.model.ReaderTextAlignment
import ua.acclorite.book_story.presentation.settings.SettingsEvent
import ua.acclorite.book_story.ui.common.components.common.AnimatedVisibility
import ua.acclorite.book_story.ui.reader.model.FontWithName
import ua.acclorite.book_story.ui.theme.model.HorizontalAlignment
import java.io.File

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ReaderScaffold(
    book: Book,
    text: List<ReaderText>,
    listState: LazyListState,
    currentChapter: Chapter?,
    nestedScrollConnection: NestedScrollConnection,
    fastColorPresetChange: Boolean,
    perceptionExpander: Boolean,
    perceptionExpanderPadding: Dp,
    perceptionExpanderThickness: Dp,
    horizontalLimiter: Boolean,
    horizontalLimiterHeight: Dp,
    horizontalLimiterVerticalOffset: Float,
    horizontalLimiterRulerThickness: Dp,
    horizontalLimiterRuler: Boolean,
    horizontalLimiterDimming: Float,
    currentChapterProgress: Float,
    isLoading: Boolean,
    checkpoints: List<Checkpoint>,
    showMenu: Boolean,
    lockMenu: Boolean,
    contentPadding: PaddingValues,
    verticalPadding: Dp,
    horizontalGesture: ReaderHorizontalGesture,
    horizontalGestureScroll: Float,
    horizontalGestureSensitivity: Dp,
    horizontalGestureAlphaAnim: Boolean,
    horizontalGesturePullAnim: Boolean,
    horizontalGestureDisableScrolling: Boolean,
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight,
    progress: String,
    progressBar: Boolean,
    progressBarPadding: Dp,
    progressBarAlignment: HorizontalAlignment,
    progressBarFontSize: TextUnit,
    paragraphHeight: Dp,
    sidePadding: Dp,
    bottomBarPadding: Dp,
    backgroundColor: Color,
    fontColor: Color,
    images: Boolean,
    imagesCaptions: Boolean,
    imagesCornersRoundness: Dp,
    imagesAlignment: HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?,
    fontFamily: FontWithName,
    lineHeight: TextUnit,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    chapterTitleAlignment: ReaderTextAlignment,
    textAlignment: ReaderTextAlignment,
    horizontalAlignment: Alignment.Horizontal,
    fontSize: TextUnit,
    letterSpacing: TextUnit,
    paragraphIndentation: TextUnit,
    doubleClickTranslation: Boolean,
    switchColorPreset: (SettingsEvent.OnSwitchColorPreset) -> Unit,
    menuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit,
    leave: (ReaderEvent.OnLeave) -> Unit,
    restoreCheckpoint: (ReaderEvent.OnRestoreCheckpoint) -> Unit,
    scroll: (ReaderEvent.OnScroll) -> Unit,
    changeProgress: (ReaderEvent.OnChangeProgress) -> Unit,
    openShareApp: (ReaderEvent.OnOpenShareApp) -> Unit,
    openWebBrowser: (ReaderEvent.OnOpenWebBrowser) -> Unit,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    openDictionary: (ReaderEvent.OnOpenDictionary) -> Unit,
    showSettingsBottomSheet: (ReaderEvent.OnShowSettingsBottomSheet) -> Unit,
    showChaptersDrawer: (ReaderEvent.OnShowChaptersDrawer) -> Unit,
    navigateToBookInfo: (ReaderEvent.OnNavigateToBookInfo) -> Unit,
    navigateBack: (ReaderEvent.OnNavigateBack) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val extraPrefs = remember(book.id) {
        context.getSharedPreferences("reader_extra_${book.id}", Context.MODE_PRIVATE)
    }
    var baseText by remember(text) { mutableStateOf(text) }
    var replacementRules by remember(book.id) {
        mutableStateOf(extraPrefs.getString("replacement_rules", "").orEmpty())
    }
    var bookmarks by remember(book.id) {
        mutableStateOf(
            extraPrefs.getString("bookmarks", "").orEmpty()
                .split(",")
                .mapNotNull { it.toIntOrNull() }
                .toSet()
        )
    }
    var displayedText by remember(baseText, replacementRules) {
        mutableStateOf(baseText.applyReplacementRules(replacementRules))
    }
    var editingStartIndex by remember { mutableIntStateOf(-1) }
    var editingEndIndex by remember { mutableIntStateOf(-1) }
    var editingValue by remember { mutableStateOf("") }
    var editingError by remember { mutableStateOf<String?>(null) }
    var searchDialogVisible by remember { mutableStateOf(false) }
    var searchValue by remember { mutableStateOf("") }
    var replaceDialogVisible by remember { mutableStateOf(false) }
    var replaceValue by remember { mutableStateOf(replacementRules) }

    fun persistBookmarks(value: Set<Int>) {
        bookmarks = value
        extraPrefs.edit().putString("bookmarks", value.sorted().joinToString(",")).apply()
    }

    fun currentChapterRange(): IntRange? {
        if (baseText.isEmpty()) return null
        val currentIndex = listState.firstVisibleItemIndex.coerceIn(0, (baseText.size - 1).coerceAtLeast(0))
        val start = (currentIndex downTo 0).firstOrNull { baseText[it] is ReaderText.Chapter } ?: 0
        val endExclusive = ((start + 1) until baseText.size).firstOrNull {
            baseText[it] is ReaderText.Chapter
        } ?: baseText.size
        return if (start < endExclusive) start until endExclusive else null
    }

    fun openChapterEditor() {
        val range = currentChapterRange() ?: return
        editingStartIndex = range.first
        editingEndIndex = range.last + 1
        editingValue = baseText.subList(editingStartIndex, editingEndIndex)
            .filterIsInstance<ReaderText.Text>()
            .joinToString("\n") { it.line.text }
        editingError = null
    }

    fun saveEditedChapter(value: String) {
        if (editingStartIndex < 0 || editingEndIndex <= editingStartIndex) return
        val updatedText = baseText.toMutableList()
        val chapter = updatedText.getOrNull(editingStartIndex) as? ReaderText.Chapter
        val replacement = buildList {
            if (chapter != null) add(chapter)
            value.lines()
                .filter { it.isNotBlank() }
                .forEach { add(ReaderText.Text(AnnotatedString(it))) }
        }
        updatedText.subList(editingStartIndex, editingEndIndex).clear()
        updatedText.addAll(editingStartIndex, replacement)
        baseText = updatedText

        if (book.filePath.endsWith(".txt", ignoreCase = true)) {
            runCatching {
                val output = updatedText.joinToString(separator = "\n") { line ->
                    when (line) {
                        is ReaderText.Chapter -> line.title
                        is ReaderText.Text -> line.line.text
                        is ReaderText.Separator -> "---"
                        is ReaderText.Image -> ""
                    }
                }
                File(book.filePath).writeText(output)
            }.onFailure {
                editingError = "TXT file could not be saved: ${it.message ?: "unknown error"}"
            }
        } else {
            editingError = "本章内容已在当前阅读界面更新；直接写回原文件目前只支持 TXT。"
        }
    }

    fun searchNext() {
        val query = searchValue.trim()
        if (displayedText.isEmpty()) return
        if (query.isBlank()) return
        val start = (listState.firstVisibleItemIndex + 1).coerceAtMost(displayedText.lastIndex)
        val orderedIndexes = (start..displayedText.lastIndex) + (0 until start)
        val target = orderedIndexes.firstOrNull { index ->
            (displayedText[index] as? ReaderText.Text)?.line?.text?.contains(query, ignoreCase = true) == true
        }
        if (target != null) {
            coroutineScope.launch { listState.animateScrollToItem(target) }
            searchDialogVisible = false
        } else {
            editingError = "没有找到：$query"
        }
    }

    Scaffold(
        Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AnimatedVisibility(
                visible = showMenu,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it }
            ) {
                ReaderTopBar(
                    book = book,
                    currentChapter = currentChapter,
                    fastColorPresetChange = fastColorPresetChange,
                    currentChapterProgress = currentChapterProgress,
                    isLoading = isLoading,
                    lockMenu = lockMenu,
                    leave = leave,
                    switchColorPreset = switchColorPreset,
                    navigateBack = navigateBack,
                    navigateToBookInfo = navigateToBookInfo
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = showMenu,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                ReaderBottomBar(
                    book = book,
                    progress = progress,
                    text = displayedText,
                    listState = listState,
                    lockMenu = lockMenu,
                    checkpoints = checkpoints,
                    bottomBarPadding = bottomBarPadding,
                    restoreCheckpoint = restoreCheckpoint,
                    scroll = scroll,
                    changeProgress = changeProgress,
                    showChapters = { showChaptersDrawer(ReaderEvent.OnShowChaptersDrawer) },
                    showSettings = { showSettingsBottomSheet(ReaderEvent.OnShowSettingsBottomSheet) },
                    editChapter = { openChapterEditor() },
                    search = { searchDialogVisible = true },
                    replaceRules = {
                        replaceValue = replacementRules
                        replaceDialogVisible = true
                    },
                    toggleBookmark = {
                        val index = listState.firstVisibleItemIndex
                        persistBookmarks(
                            if (bookmarks.contains(index)) bookmarks - index else bookmarks + index
                        )
                        editingError = if (bookmarks.contains(index)) "已取消当前书签" else "已添加当前书签"
                    },
                    nextBookmark = {
                        val current = listState.firstVisibleItemIndex
                        val target = bookmarks.sorted().firstOrNull { it > current }
                            ?: bookmarks.sorted().firstOrNull()
                        if (target != null) {
                            coroutineScope.launch { listState.animateScrollToItem(target) }
                        } else {
                            editingError = "还没有书签"
                        }
                    }
                )
            }
        }
    ) {
        ReaderLayout(
            text = displayedText,
            listState = listState,
            contentPadding = contentPadding,
            verticalPadding = verticalPadding,
            horizontalGesture = horizontalGesture,
            horizontalGestureScroll = horizontalGestureScroll,
            horizontalGestureSensitivity = horizontalGestureSensitivity,
            horizontalGestureAlphaAnim = horizontalGestureAlphaAnim,
            horizontalGesturePullAnim = horizontalGesturePullAnim,
            horizontalGestureDisableScrolling = horizontalGestureDisableScrolling,
            highlightedReading = highlightedReading,
            highlightedReadingThickness = highlightedReadingThickness,
            progress = progress,
            progressBar = progressBar,
            progressBarPadding = progressBarPadding,
            progressBarAlignment = progressBarAlignment,
            progressBarFontSize = progressBarFontSize,
            paragraphHeight = paragraphHeight,
            sidePadding = sidePadding,
            backgroundColor = backgroundColor,
            fontColor = fontColor,
            images = images,
            imagesCaptions = imagesCaptions,
            imagesCornersRoundness = imagesCornersRoundness,
            imagesAlignment = imagesAlignment,
            imagesWidth = imagesWidth,
            imagesColorEffects = imagesColorEffects,
            fontFamily = fontFamily,
            lineHeight = lineHeight,
            fontThickness = fontThickness,
            fontStyle = fontStyle,
            chapterTitleAlignment = chapterTitleAlignment,
            textAlignment = textAlignment,
            horizontalAlignment = horizontalAlignment,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            paragraphIndentation = paragraphIndentation,
            doubleClickTranslation = doubleClickTranslation,
            isLoading = isLoading,
            showMenu = showMenu,
            menuVisibility = menuVisibility,
            openShareApp = openShareApp,
            openWebBrowser = openWebBrowser,
            openTranslator = openTranslator,
            openDictionary = openDictionary
        )

        ReaderPerceptionExpander(
            perceptionExpander = perceptionExpander,
            perceptionExpanderPadding = perceptionExpanderPadding,
            perceptionExpanderThickness = perceptionExpanderThickness,
            perceptionExpanderColor = fontColor
        )

        ReaderHorizontalLimiter(
            horizontalLimiter = horizontalLimiter,
            horizontalLimiterHeight = horizontalLimiterHeight,
            horizontalLimiterVerticalOffset = horizontalLimiterVerticalOffset,
            horizontalLimiterRulerThickness = horizontalLimiterRulerThickness,
            horizontalLimiterRuler = horizontalLimiterRuler,
            horizontalLimiterDimming = horizontalLimiterDimming,
            horizontalLimiterDimmingColor = backgroundColor,
            horizontalLimiterRulerColor = fontColor
        )

        if (isLoading) {
            ReaderLoadingPlaceholder()
        }

        if (editingStartIndex >= 0) {
            AlertDialog(
                onDismissRequest = { editingStartIndex = -1 },
                title = { Text("编辑当前章节") },
                text = {
                    OutlinedTextField(
                        value = editingValue,
                        onValueChange = { editingValue = it },
                        minLines = 8,
                        maxLines = 18,
                        label = { Text("章节内容") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            saveEditedChapter(editingValue)
                            editingStartIndex = -1
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingStartIndex = -1 }) {
                        Text("取消")
                    }
                }
            )
        }

        if (searchDialogVisible) {
            AlertDialog(
                onDismissRequest = { searchDialogVisible = false },
                title = { Text("搜索内容") },
                text = {
                    OutlinedTextField(
                        value = searchValue,
                        onValueChange = { searchValue = it },
                        label = { Text("输入要搜索的文字") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = { searchNext() }) {
                        Text("搜索下一个")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { searchDialogVisible = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (replaceDialogVisible) {
            AlertDialog(
                onDismissRequest = { replaceDialogVisible = false },
                title = { Text("替换规则") },
                text = {
                    OutlinedTextField(
                        value = replaceValue,
                        onValueChange = { replaceValue = it },
                        minLines = 5,
                        maxLines = 10,
                        label = { Text("每行一条：原文=>替换后") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            replacementRules = replaceValue
                            extraPrefs.edit().putString("replacement_rules", replacementRules).apply()
                            replaceDialogVisible = false
                        }
                    ) {
                        Text("应用")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            replaceValue = ""
                            replacementRules = ""
                            extraPrefs.edit().remove("replacement_rules").apply()
                            replaceDialogVisible = false
                        }
                    ) {
                        Text("清空")
                    }
                }
            )
        }

        if (editingError != null) {
            AlertDialog(
                onDismissRequest = { editingError = null },
                title = { Text("提示") },
                text = { Text(editingError.orEmpty()) },
                confirmButton = {
                    Button(onClick = { editingError = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

private fun List<ReaderText>.applyReplacementRules(rulesText: String): List<ReaderText> {
    val rules = rulesText.lines().mapNotNull { line ->
        val separator = line.indexOf("=>")
        if (separator <= 0) return@mapNotNull null
        val source = line.substring(0, separator)
        val target = line.substring(separator + 2)
        if (source.isBlank()) null else source to target
    }
    if (rules.isEmpty()) return this

    return map { entry ->
        if (entry !is ReaderText.Text) return@map entry
        val replaced = rules.fold(entry.line.text) { current, rule ->
            current.replace(rule.first, rule.second)
        }
        if (replaced == entry.line.text) entry else ReaderText.Text(AnnotatedString(replaced))
    }
}
