/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.data.model.file.CachedFileCompat
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
    val globalPrefs = remember {
        context.getSharedPreferences("reader_global_tools", Context.MODE_PRIVATE)
    }
    var baseText by remember(text) { mutableStateOf(text) }
    var replacementRules by remember {
        mutableStateOf(globalPrefs.getString("replacement_rules", "").orEmpty())
    }
    var bookmarks by remember(book.id) {
        mutableStateOf(
            extraPrefs.getString("bookmarks", "").orEmpty()
                .split(",")
                .mapNotNull { BookmarkPoint.fromStorage(it) }
                .toSet()
        )
    }
    var dialogueHighlightColor by remember {
        mutableStateOf(globalPrefs.getInt("dialogue_highlight_color", Color(0xFF1565C0).toArgb()))
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
    var searchResults by remember { mutableStateOf(emptyList<SearchResult>()) }
    var replaceDialogVisible by remember { mutableStateOf(false) }
    var replaceValue by remember { mutableStateOf(replacementRules) }
    var replaceRuleName by remember { mutableStateOf("默认替换规则") }
    var replaceSource by remember { mutableStateOf("") }
    var replaceTarget by remember { mutableStateOf("") }
    var replaceUseRegex by remember { mutableStateOf(false) }
    var replaceScope by remember { mutableStateOf("内容") }
    var bookmarkDialogVisible by remember { mutableStateOf(false) }
    var highlightColorDialogVisible by remember { mutableStateOf(false) }

    fun loadReplaceFormFromRules() {
        val firstRule = replacementRules.lines().firstOrNull { it.contains("=>") }.orEmpty()
        val source = firstRule.substringBefore("=>", "")
        replaceUseRegex = source.startsWith("正则:") || source.startsWith("regex:")
        replaceSource = source
            .removePrefix("正则:")
            .removePrefix("regex:")
        replaceTarget = firstRule.substringAfter("=>", "")
        if (replaceRuleName.isBlank()) replaceRuleName = "默认替换规则"
    }

    fun saveReplaceForm() {
        val source = replaceSource.trim()
        if (source.isBlank()) {
            editingError = "匹配规则不能为空"
            return
        }
        val rule = "${if (replaceUseRegex) "正则:" else ""}$source=>$replaceTarget"
        replacementRules = rule
        replaceValue = rule
        globalPrefs.edit()
            .putString("replacement_rules", replacementRules)
            .putString("replacement_rule_name", replaceRuleName)
            .putString("replacement_rule_scope", replaceScope)
            .apply()
        replaceDialogVisible = false
    }

    fun persistBookmarks(value: Set<BookmarkPoint>) {
        bookmarks = value
        extraPrefs.edit()
            .putString("bookmarks", value.sortedWith(compareBy({ it.index }, { it.offset })).joinToString(",") { it.toStorage() })
            .apply()
    }

    fun currentPageToChapterEndRange(): IntRange? {
        if (baseText.isEmpty()) return null
        val currentIndex = listState.firstVisibleItemIndex.coerceIn(0, (baseText.size - 1).coerceAtLeast(0))
        val start = (currentIndex until baseText.size).firstOrNull { baseText[it] is ReaderText.Text }
            ?: return null
        val endExclusive = ((start + 1) until baseText.size).firstOrNull {
            baseText[it] is ReaderText.Chapter
        } ?: baseText.size
        return if (start < endExclusive) start until endExclusive else null
    }

    fun openChapterEditor() {
        val range = currentPageToChapterEndRange() ?: return
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
        val replacement = buildList {
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
                writeOriginalTxtFile(context, book.filePath, output)
                editingError = "已保存并替换手机里的原 TXT 文件。"
            }.onFailure {
                editingError = "TXT 原文件保存失败：${it.message ?: "未知错误"}。如果这本书是旧导入的，请重新从手机文件夹导入一次，让 App 获取写入权限。"
            }
        } else {
            editingError = "本章内容已在当前阅读界面更新；直接写回原文件目前只支持 TXT。"
        }
    }

    fun buildSearchResults() {
        val query = searchValue.trim()
        if (displayedText.isEmpty() || query.isBlank()) {
            searchResults = emptyList()
            return
        }
        var chapter = "未命名章节"
        searchResults = displayedText.flatMapIndexed { index, entry ->
            when (entry) {
                is ReaderText.Chapter -> {
                    chapter = entry.title
                    emptyList()
                }
                is ReaderText.Text -> entry.line.text.findAllPlain(query).map { charIndex ->
                    SearchResult(
                        index = index,
                        charIndex = charIndex,
                        chapter = chapter,
                        preview = entry.line.text.makePreview(charIndex, query.length)
                    )
                }
                else -> emptyList()
            }
        }
        if (searchResults.isEmpty()) {
            editingError = "没有找到：$query"
        }
    }

    fun jumpToSearchResult(result: SearchResult) {
        coroutineScope.launch {
            listState.animateScrollToItem(result.index)
        }
        searchDialogVisible = false
    }

    fun currentBookmarkPoint(): BookmarkPoint {
        return BookmarkPoint(
            index = listState.firstVisibleItemIndex,
            offset = listState.firstVisibleItemScrollOffset
        )
    }

    fun jumpToBookmark(point: BookmarkPoint) {
        coroutineScope.launch {
            listState.animateScrollToItem(point.index, point.offset)
        }
    }

    fun nextBookmarkPoint(): BookmarkPoint? {
        val current = currentBookmarkPoint()
        return bookmarks.sortedWith(compareBy({ it.index }, { it.offset }))
            .firstOrNull { it.index > current.index || (it.index == current.index && it.offset > current.offset) }
            ?: bookmarks.sortedWith(compareBy({ it.index }, { it.offset })).firstOrNull()
    }

    fun chapterTitleFor(index: Int): String {
        return (index downTo 0).firstNotNullOfOrNull { position ->
            (displayedText.getOrNull(position) as? ReaderText.Chapter)?.title
        } ?: book.title
    }

    fun previewFor(index: Int): String {
        val textLine = (displayedText.getOrNull(index) as? ReaderText.Text)?.line?.text
            ?: ((index + 1) until displayedText.size).firstNotNullOfOrNull { position ->
                (displayedText.getOrNull(position) as? ReaderText.Text)?.line?.text
            }
            ?: ""
        return textLine.trim().take(80).ifBlank { "当前位置" }
    }

    fun progressFor(index: Int): String {
        if (displayedText.isEmpty()) return "0%"
        val progressValue = ((index + 1).toFloat() / displayedText.size * 100).coerceIn(0f, 100f)
        return "${"%.1f".format(progressValue)}%"
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
                        replaceRuleName = globalPrefs.getString("replacement_rule_name", "默认替换规则").orEmpty()
                        replaceScope = globalPrefs.getString("replacement_rule_scope", "内容").orEmpty().ifBlank { "内容" }
                        loadReplaceFormFromRules()
                        replaceDialogVisible = true
                    },
                    toggleBookmark = { bookmarkDialogVisible = true },
                    nextBookmark = {
                        val target = nextBookmarkPoint()
                        if (target != null) {
                            jumpToBookmark(target)
                        } else {
                            editingError = "还没有书签"
                        }
                    },
                    highlightColor = { highlightColorDialogVisible = true }
                )
            }
        }
    ) {
        val useOriginalEpubMode = remember(book.filePath) {
            book.filePath.endsWith(".epub", ignoreCase = true) && canUseOriginalEpubMode(book.filePath)
        }
        if (useOriginalEpubMode && !isLoading) {
            EpubOriginalReader(
                filePath = book.filePath,
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
                dialogueHighlightColor = Color(dialogueHighlightColor),
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
        }

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
                    Column {
                        OutlinedTextField(
                            value = searchValue,
                            onValueChange = { searchValue = it },
                            label = { Text("输入要搜索的文字") },
                            singleLine = true
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                        ) {
                            items(searchResults) { result ->
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { jumpToSearchResult(result) }
                                ) {
                                    Text(
                                        text = "${result.chapter}\n第 ${result.charIndex + 1} 字：${result.preview}"
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { buildSearchResults() }) {
                        Text("搜索")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { searchDialogVisible = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (bookmarkDialogVisible) {
            AlertDialog(
                onDismissRequest = { bookmarkDialogVisible = false },
                title = { Text("书签管理") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "目录",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "书签",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (bookmarks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "(๑•́ ₃ •̀๑)",
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                    Text(
                                        text = "暂无书签",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    bookmarks.sortedWith(compareBy({ it.index }, { it.offset }))
                                ) { point ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = chapterTitleFor(point.index),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "位置：${point.index + 1} · 进度：${progressFor(point.index)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = previewFor(point.index),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        jumpToBookmark(point)
                                                        bookmarkDialogVisible = false
                                                    }
                                                ) {
                                                    Text("跳转")
                                                }
                                                TextButton(
                                                    onClick = {
                                                        persistBookmarks(bookmarks - point)
                                                    }
                                                ) {
                                                    Text("删除")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val point = currentBookmarkPoint()
                            persistBookmarks(bookmarks + point)
                        }
                    ) {
                        Text("添加当前页")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bookmarkDialogVisible = false }) {
                        Text("关闭")
                    }
                }
            )
        }

        if (replaceDialogVisible) {
            AlertDialog(
                onDismissRequest = { replaceDialogVisible = false },
                title = { Text("新增替换规则") },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = replaceRuleName,
                                onValueChange = { replaceRuleName = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("规则名称") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = "默认分组",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                singleLine = true,
                                label = { Text("分组") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = replaceSource,
                                onValueChange = { replaceSource = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                label = { Text("匹配规则") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = replaceTarget,
                                onValueChange = { replaceTarget = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                label = { Text("替换为") }
                            )
                        }
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = { replaceScope = "标题" },
                                    label = { Text(if (replaceScope == "标题") "标题 ✓" else "标题") }
                                )
                                AssistChip(
                                    onClick = { replaceScope = "内容" },
                                    label = { Text(if (replaceScope == "内容") "内容 ✓" else "内容") }
                                )
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("使用正则表达式")
                                TextButton(onClick = { replaceUseRegex = !replaceUseRegex }) {
                                    Text(if (replaceUseRegex) "已开启" else "已关闭")
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("特定范围") },
                                placeholder = { Text("全部书籍") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("排除范围") },
                                placeholder = { Text("无") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = "3000",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("超时时间（毫秒）") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { saveReplaceForm() }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            replaceRuleName = "默认替换规则"
                            replaceSource = ""
                            replaceTarget = ""
                            replaceUseRegex = false
                            replaceValue = ""
                            replacementRules = ""
                            globalPrefs.edit()
                                .remove("replacement_rules")
                                .remove("replacement_rule_name")
                                .remove("replacement_rule_scope")
                                .apply()
                            replaceDialogVisible = false
                        }
                    ) {
                        Text("清空")
                    }
                }
            )
        }

        if (highlightColorDialogVisible) {
            AlertDialog(
                onDismissRequest = { highlightColorDialogVisible = false },
                title = { Text("对话高亮颜色") },
                text = {
                    Column {
                        listOf(
                            "蓝色" to Color(0xFF1565C0),
                            "红色" to Color(0xFFC62828),
                            "绿色" to Color(0xFF2E7D32),
                            "紫色" to Color(0xFF6A1B9A),
                            "橙色" to Color(0xFFEF6C00),
                        ).forEach { (name, color) ->
                            TextButton(
                                onClick = {
                                    dialogueHighlightColor = color.toArgb()
                                    globalPrefs.edit().putInt("dialogue_highlight_color", dialogueHighlightColor).apply()
                                    highlightColorDialogVisible = false
                                }
                            ) {
                                Text(name, color = color)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { highlightColorDialogVisible = false }) {
                        Text("关闭")
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
        if (source.isBlank()) null else ReplacementRule(source, target)
    }
    if (rules.isEmpty()) return this

    return map { entry ->
        if (entry !is ReaderText.Text) return@map entry
        val replaced = rules.fold(entry.line.text) { current, rule ->
            rule.apply(current)
        }
        if (replaced == entry.line.text) entry else ReaderText.Text(AnnotatedString(replaced))
    }
}

private data class ReplacementRule(
    val source: String,
    val target: String
) {
    fun apply(text: String): String {
        return if (source.startsWith("正则:") || source.startsWith("regex:")) {
            val pattern = source.substringAfter(":")
            runCatching { Regex(pattern).replace(text, target) }.getOrDefault(text)
        } else {
            text.replace(source, target)
        }
    }
}

private data class SearchResult(
    val index: Int,
    val charIndex: Int,
    val chapter: String,
    val preview: String
)

private data class BookmarkPoint(
    val index: Int,
    val offset: Int
) {
    fun toStorage(): String = "$index:$offset"

    companion object {
        fun fromStorage(value: String): BookmarkPoint? {
            val index = value.substringBefore(":").toIntOrNull() ?: return null
            val offset = value.substringAfter(":", "0").toIntOrNull() ?: 0
            return BookmarkPoint(index, offset)
        }
    }
}

private fun String.findAllPlain(query: String): List<Int> {
    val results = mutableListOf<Int>()
    var start = 0
    while (start <= length) {
        val index = indexOf(query, startIndex = start, ignoreCase = true)
        if (index < 0) break
        results += index
        start = index + query.length.coerceAtLeast(1)
    }
    return results
}

private fun String.makePreview(charIndex: Int, queryLength: Int): String {
    val start = (charIndex - 18).coerceAtLeast(0)
    val end = (charIndex + queryLength + 28).coerceAtMost(length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < length) "…" else ""
    return prefix + substring(start, end) + suffix
}

private fun writeOriginalTxtFile(context: Context, filePath: String, text: String) {
    val directFile = File(filePath)
    if (directFile.exists() && directFile.canWrite()) {
        directFile.writeText(text)
        return
    }

    if (filePath.startsWith("content://", ignoreCase = true)) {
        context.contentResolver.openOutputStream(filePath.toUri(), "wt")?.use { output ->
            output.write(text.toByteArray())
        } ?: throw IllegalStateException("无法打开原 TXT 文件写入流")
        return
    }

    context.contentResolver.persistedUriPermissions.forEach { permission ->
        val storage = CachedFileCompat.fromUri(context, permission.uri)
        if (!storage.isDirectory) return@forEach
        if (!filePath.startsWith(storage.path, ignoreCase = true)) return@forEach

        storage.walk().forEach { file ->
            if (file.path.equals(filePath, ignoreCase = true)) {
                context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
                    output.write(text.toByteArray())
                } ?: throw IllegalStateException("无法打开原 TXT 文件写入流")
                return
            }
        }
    }

    throw IllegalStateException("找不到可写入的原 TXT 文件")
}
