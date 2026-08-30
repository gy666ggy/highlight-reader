/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import ua.acclorite.book_story.ui.common.helpers.noRippleClickable
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
    var searchReplaceValue by remember { mutableStateOf("") }
    var searchUseRegex by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf(emptyList<SearchResult>()) }
    var replaceDialogVisible by remember { mutableStateOf(false) }
    var replaceValue by remember { mutableStateOf(replacementRules) }
    var replaceRuleName by remember { mutableStateOf("") }
    var replaceGroup by remember { mutableStateOf("") }
    var replaceSource by remember { mutableStateOf("") }
    var replaceTarget by remember { mutableStateOf("") }
    var replaceUseRegex by remember { mutableStateOf(false) }
    var replaceScopeTitle by remember { mutableStateOf(false) }
    var replaceScopeContent by remember { mutableStateOf(true) }
    var replaceRange by remember { mutableStateOf("") }
    var replaceExcludeRange by remember { mutableStateOf("") }
    var replaceTimeout by remember { mutableStateOf("3000") }
    var replaceEditingIndex by remember { mutableIntStateOf(-1) } // -1=新增, >=0=编辑
    var bookmarkDialogVisible by remember { mutableStateOf(false) }
    var highlightColorDialogVisible by remember { mutableStateOf(false) }

    // 修改高亮状态
    var modifyHighlightMode by remember { mutableStateOf(false) }
    var modifyHighlightColorDialogVisible by remember { mutableStateOf(false) }
    var selectedModifyColor by remember {
        val saved = globalPrefs.getInt("last_modify_highlight_color", -1)
        mutableStateOf(if (saved != -1) Color(saved) else null)
    }
    var paragraphHighlightColors by remember(book.id) {
        mutableStateOf<Map<Long, Int>>(loadParagraphColors(context, book.id))
    }

    // 段落唯一键映射：列表索引 -> 段落唯一ID (Long类型)
    // 使用"章节索引 * 2^32 + 章内文本段落序号"生成唯一键
    val paragraphTextKeys: Map<Int, Long> = remember(baseText) {
        var chapterIndex = 0
        var paragraphInChapter = 0
        baseText.mapIndexed { index, entry ->
            val key = when (entry) {
                is ReaderText.Chapter -> {
                    val k = (chapterIndex.toLong() shl 32) or (0xFFFFFFF0L)
                    chapterIndex++
                    paragraphInChapter = 0
                    k
                }
                is ReaderText.Text -> {
                    val k = (chapterIndex.toLong() shl 32) or paragraphInChapter.toLong()
                    paragraphInChapter++
                    k
                }
                else -> {
                    (chapterIndex.toLong() shl 32) or (0xFF000000L or index.toLong().and(0xFFFFFFL))
                }
            }
            index to key
        }.toMap()
    }

    // 本章搜索替换状态
    var chapterReplaceDialogVisible by remember { mutableStateOf(false) }
    var chapterSearchValue by remember { mutableStateOf("") }
    var chapterReplaceValue by remember { mutableStateOf("") }
    var chapterUseRegex by remember { mutableStateOf(false) }
    var chapterSearchResults by remember { mutableStateOf(emptyList<ChapterMatchResult>()) }

    // 排序对话框状态
    var sortDialogVisible by remember { mutableStateOf(false) }
    val defaultButtonOrder = listOf(
        "chapters", "bookmark", "nextBookmark", "search", "replace",
        "chapterReplace", "editChapter", "highlightColor", "modifyHighlight", "sort", "settings"
    )
    var buttonOrder by remember {
        val saved = globalPrefs.getString("bottom_button_order", "").orEmpty()
            .split(",").filter { it.isNotBlank() }
        mutableStateOf(if (saved.isNotEmpty() && saved.size == defaultButtonOrder.size) saved else defaultButtonOrder)
    }

    fun parseReplaceRules(): List<String> {
        return replacementRules.lines().filter { it.isNotBlank() }
    }

    fun loadReplaceFormFromRules() {
        replaceRuleName = ""
        replaceGroup = ""
        replaceSource = ""
        replaceTarget = ""
        replaceUseRegex = false
        replaceScopeTitle = false
        replaceScopeContent = true
        replaceRange = ""
        replaceExcludeRange = ""
        replaceTimeout = "3000"
        replaceEditingIndex = -1
    }

    fun loadReplaceFormForEdit(index: Int) {
        val rules = parseReplaceRules()
        if (index !in rules.indices) return
        val rule = parseRuleLine(rules[index])
        replaceRuleName = rule.name
        replaceGroup = rule.group
        replaceSource = rule.source
        replaceTarget = rule.target
        replaceUseRegex = rule.isRegex
        replaceScopeTitle = rule.scopeTitle
        replaceScopeContent = rule.scopeContent
        replaceRange = rule.range
        replaceExcludeRange = rule.excludeRange
        replaceTimeout = rule.timeout
        replaceEditingIndex = index
    }

    fun saveReplaceForm() {
        val source = replaceSource.trim()
        if (source.isBlank()) {
            editingError = "替换规则不能为空"
            return
        }
        val ruleJson = buildRuleJson(
            name = replaceRuleName.trim(),
            group = replaceGroup.trim(),
            source = source,
            target = replaceTarget,
            isRegex = replaceUseRegex,
            scopeTitle = replaceScopeTitle,
            scopeContent = replaceScopeContent,
            range = replaceRange.trim(),
            excludeRange = replaceExcludeRange.trim(),
            timeout = replaceTimeout.trim().ifBlank { "3000" }
        )
        val existingRules = parseReplaceRules().toMutableList()
        if (replaceEditingIndex >= 0 && replaceEditingIndex < existingRules.size) {
            existingRules[replaceEditingIndex] = ruleJson
        } else if (ruleJson !in existingRules) {
            existingRules.add(ruleJson)
        }
        replacementRules = existingRules.joinToString("\n")
        replaceValue = replacementRules
        globalPrefs.edit()
            .putString("replacement_rules", replacementRules)
            .apply()
        loadReplaceFormFromRules()
    }

    fun deleteReplaceRule(index: Int) {
        val rules = parseReplaceRules().toMutableList()
        if (index in rules.indices) {
            rules.removeAt(index)
            replacementRules = rules.joinToString("\n")
            replaceValue = replacementRules
            globalPrefs.edit()
                .putString("replacement_rules", replacementRules)
                .apply()
        }
    }

    fun clearAllReplaceRules() {
        replacementRules = ""
        replaceValue = ""
        loadReplaceFormFromRules()
        globalPrefs.edit()
            .remove("replacement_rules")
            .apply()
    }

    fun persistBookmarks(value: Set<BookmarkPoint>) {
        bookmarks = value
        extraPrefs.edit()
            .putString("bookmarks", value.sortedWith(compareBy({ it.index }, { it.offset })).joinToString(",") { it.toStorage() })
            .apply()
    }

    fun persistParagraphColors(colors: Map<Long, Int>) {
        paragraphHighlightColors = colors
        saveParagraphColors(context, book.id, colors)
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

    fun chapterSearchInCurrent() {
        val query = chapterSearchValue.trim()
        if (query.isBlank()) {
            chapterSearchResults = emptyList()
            editingError = null
            return
        }
        val range = currentPageToChapterEndRange() ?: run {
            editingError = "未找到当前章节"
            return
        }
        val text = baseText.subList(range.first, range.last + 1)
            .filterIsInstance<ReaderText.Text>()
            .joinToString("\n") { it.line.text }

        fun makePreview(start: Int, end: Int): String {
            val ctxStart = (start - 15).coerceAtLeast(0)
            val ctxEnd = (end + 15).coerceAtMost(text.length)
            val prefix = if (ctxStart > 0) "…" else ""
            val suffix = if (ctxEnd < text.length) "…" else ""
            return prefix + text.substring(ctxStart, ctxEnd) + suffix
        }

        chapterSearchResults = if (chapterUseRegex) {
            runCatching {
                val regex = Regex(query)
                regex.findAll(text).mapIndexed { i, m ->
                    ChapterMatchResult(
                        index = i,
                        start = m.range.first,
                        end = m.range.last + 1,
                        matched = m.value,
                        preview = makePreview(m.range.first, m.range.last + 1)
                    )
                }.toList()
            }.getOrElse {
                editingError = "正则表达式无效：${it.message}"
                emptyList()
            }
        } else {
            val results = mutableListOf<ChapterMatchResult>()
            var pos = 0
            var i = 0
            while (true) {
                val found = text.indexOf(query, pos)
                if (found < 0) break
                results.add(ChapterMatchResult(
                    index = i++,
                    start = found,
                    end = found + query.length,
                    matched = query,
                    preview = makePreview(found, found + query.length)
                ))
                pos = found + query.length
            }
            results
        }
        if (chapterSearchResults.isEmpty()) {
            editingError = "本章未找到：$query"
        } else {
            editingError = null
        }
    }

    fun chapterReplaceAll() {
        val query = chapterSearchValue.trim()
        val replacement = chapterReplaceValue
        if (query.isBlank()) {
            editingError = "请先输入搜索内容"
            return
        }
        val range = currentPageToChapterEndRange() ?: run {
            editingError = "未找到当前章节"
            return
        }

        val chapterTexts = baseText.subList(range.first, range.last + 1)
            .filterIsInstance<ReaderText.Text>()
        val fullText = chapterTexts.joinToString("\n") { it.line.text }

        val replacedText = if (chapterUseRegex) {
            runCatching { Regex(query).replace(fullText, replacement) }.getOrElse {
                editingError = "正则表达式无效：${it.message}"
                return
            }
        } else {
            fullText.replace(query, replacement)
        }

        if (replacedText == fullText) {
            editingError = "没有找到需要替换的内容：$query"
            return
        }

        val newLines = replacedText.lines().filter { it.isNotBlank() }
        val updatedText = baseText.toMutableList()
        val replacementEntries = buildList {
            newLines.forEach { add(ReaderText.Text(AnnotatedString(it))) }
        }
        updatedText.subList(range.first, range.last + 1).clear()
        updatedText.addAll(range.first, replacementEntries)
        baseText = updatedText
        chapterSearchResults = emptyList()

        // 同步保存到原 TXT 文件
        if (book.filePath.endsWith(".txt", ignoreCase = true)) {
            editingError = "正在替换并保存到原 TXT 文件…"
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val output = updatedText.joinToString(separator = "\n") { line ->
                            when (line) {
                                is ReaderText.Chapter -> line.title
                                is ReaderText.Text -> line.line.text
                                is ReaderText.Separator -> "---"
                                is ReaderText.Image -> ""
                                is ReaderText.HtmlMedia -> ""
                            }
                        }
                        writeOriginalTxtFile(context, book.filePath, output)
                    }
                }
                editingError = result.fold(
                    onSuccess = { "已替换并保存到原 TXT 文件。" },
                    onFailure = { "替换保存失败：${it.message ?: "未知错误"}" }
                )
            }
        } else {
            editingError = "已替换（仅 TXT 支持写回原文件）。"
        }
    }

    fun chapterReplaceSingle(result: ChapterMatchResult) {
        val query = chapterSearchValue.trim()
        val replacement = chapterReplaceValue
        if (query.isBlank()) return
        val range = currentPageToChapterEndRange() ?: return

        val chapterTexts = baseText.subList(range.first, range.last + 1)
            .filterIsInstance<ReaderText.Text>()
        val fullText = chapterTexts.joinToString("\n") { it.line.text }

        // 只替换第一处匹配（正则模式用 replaceFirst）
        val replacedText = if (chapterUseRegex) {
            runCatching { Regex(query).replaceFirst(fullText, replacement) }.getOrElse { return }
        } else {
            val idx = fullText.indexOf(query)
            if (idx < 0) return
            fullText.substring(0, idx) + replacement + fullText.substring(idx + query.length)
        }

        if (replacedText == fullText) {
            editingError = "没有找到需要替换的内容"
            return
        }

        val newLines = replacedText.lines().filter { it.isNotBlank() }
        val updatedText = baseText.toMutableList()
        val replacementEntries = buildList {
            newLines.forEach { add(ReaderText.Text(AnnotatedString(it))) }
        }
        updatedText.subList(range.first, range.last + 1).clear()
        updatedText.addAll(range.first, replacementEntries)
        baseText = updatedText

        // 重新搜索
        chapterSearchInCurrent()

        // 同步保存到原 TXT 文件
        if (book.filePath.endsWith(".txt", ignoreCase = true)) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val output = updatedText.joinToString(separator = "\n") { line ->
                            when (line) {
                                is ReaderText.Chapter -> line.title
                                is ReaderText.Text -> line.line.text
                                is ReaderText.Separator -> "---"
                                is ReaderText.Image -> ""
                                is ReaderText.HtmlMedia -> ""
                            }
                        }
                        writeOriginalTxtFile(context, book.filePath, output)
                    }
                }
            }
            editingError = "已替换1处并保存到原 TXT 文件。"
        } else {
            editingError = "已替换1处。"
        }
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
            editingError = "正在保存到手机原 TXT 文件…"
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val output = updatedText.joinToString(separator = "\n") { line ->
                            when (line) {
                                is ReaderText.Chapter -> line.title
                                is ReaderText.Text -> line.line.text
                                is ReaderText.Separator -> "---"
                                is ReaderText.Image -> ""
                                is ReaderText.HtmlMedia -> ""
                            }
                        }
                        writeOriginalTxtFile(context, book.filePath, output)
                    }
                }
                editingError = result.fold(
                    onSuccess = { "已保存并替换手机里的原 TXT 文件。" },
                    onFailure = { "TXT 原文件保存失败：${it.message ?: "未知错误"}。如果这本书是旧导入的，请重新从手机文件夹导入一次，让 App 获取写入权限。" }
                )
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

        if (searchUseRegex) {
            val regex = runCatching { Regex(query) }.getOrElse {
                editingError = "正则表达式无效：${it.message}"
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
                    is ReaderText.Text -> {
                        val text = entry.line.text
                        regex.findAll(text).map { match ->
                            SearchResult(
                                index = index,
                                charIndex = match.range.first,
                                chapter = chapter,
                                preview = text.makePreview(match.range.first, match.value.length)
                            )
                        }.toList()
                    }
                    else -> emptyList()
                }
            }
        } else {
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

    fun replaceAllFromSearch() {
        val query = searchValue.trim()
        val replacement = searchReplaceValue
        if (query.isBlank()) {
            editingError = "请先输入搜索内容"
            return
        }

        // 正则模式需要先验证
        if (searchUseRegex) {
            val testRegex = runCatching { Regex(query) }.getOrElse {
                editingError = "正则表达式无效：${it.message}"
                return
            }
        }

        var totalReplaced = 0
        val updatedText = baseText.map { entry ->
            if (entry is ReaderText.Text) {
                val original = entry.line.text
                val replaced = if (searchUseRegex) {
                    runCatching { Regex(query).replace(original, replacement) }.getOrDefault(original)
                } else {
                    original.replace(query, replacement)
                }
                if (replaced != original) {
                    totalReplaced++
                    ReaderText.Text(AnnotatedString(replaced))
                } else {
                    entry
                }
            } else {
                entry
            }
        }

        if (totalReplaced == 0) {
            editingError = "没有找到需要替换的内容：$query"
            return
        }

        baseText = updatedText
        searchResults = emptyList()

        // 保存到原文件
        if (book.filePath.endsWith(".txt", ignoreCase = true)) {
            editingError = "正在替换并保存到原文件…（共替换 $totalReplaced 处）"
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val output = updatedText.joinToString(separator = "\n") { line ->
                            when (line) {
                                is ReaderText.Chapter -> line.title
                                is ReaderText.Text -> line.line.text
                                is ReaderText.Separator -> "---"
                                is ReaderText.Image -> ""
                                is ReaderText.HtmlMedia -> ""
                            }
                        }
                        writeOriginalTxtFile(context, book.filePath, output)
                    }
                }
                editingError = result.fold(
                    onSuccess = { "已替换 $totalReplaced 处并保存到原文件。" },
                    onFailure = { "替换保存失败：${it.message ?: "未知错误"}" }
                )
            }
        } else {
            editingError = "已替换 $totalReplaced 处（仅 TXT 支持写回原文件）。"
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
                    highlightColor = { highlightColorDialogVisible = true },
                    modifyHighlight = {
                        if (modifyHighlightMode) {
                            modifyHighlightMode = false
                        } else {
                            modifyHighlightMode = true
                        }
                    },
                    modifyHighlightActive = modifyHighlightMode,
                    chapterReplace = {
                        chapterSearchValue = ""
                        chapterReplaceValue = ""
                        chapterSearchResults = emptyList()
                        chapterReplaceDialogVisible = true
                    },
                    sortButtons = { sortDialogVisible = true },
                    buttonOrder = buttonOrder
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
                paragraphHighlightColors = paragraphHighlightColors.mapValues { Color(it.value) },
                modifyHighlightMode = modifyHighlightMode,
                paragraphTextKeys = paragraphTextKeys,
                onParagraphColorChange = { key ->
                    val currentColors = paragraphHighlightColors.toMutableMap()
                    if (selectedModifyColor != null) {
                        currentColors[key] = selectedModifyColor!!.toArgb()
                        persistParagraphColors(currentColors)
                    } else {
                        // 没有选颜色就弹出颜色选择器
                        modifyHighlightColorDialogVisible = true
                    }
                },
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
                title = { Text("搜索与替换") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchValue,
                            onValueChange = { searchValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("搜索内容") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = searchReplaceValue,
                            onValueChange = { searchReplaceValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("替换为（留空则删除）") },
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("正则表达式")
                            TextButton(onClick = { searchUseRegex = !searchUseRegex }) {
                                Text(if (searchUseRegex) "已开启" else "已关闭")
                            }
                        }
                        if (searchResults.isNotEmpty()) {
                            Text(
                                "找到 ${searchResults.size} 处结果",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { buildSearchResults() }) {
                            Text("搜索")
                        }
                        Button(
                            onClick = { replaceAllFromSearch() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("全部替换")
                        }
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
            val existingRules = parseReplaceRules()
            AlertDialog(
                onDismissRequest = { replaceDialogVisible = false },
                title = { Text("替换规则") },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 已有规则列表
                        if (existingRules.isNotEmpty()) {
                            item {
                                Text(
                                    "已有规则（${existingRules.size}条）",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            itemsIndexed(existingRules) { index, ruleLine ->
                                val rule = parseRuleLine(ruleLine)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "${index + 1}. ${rule.name.ifBlank { "未命名" }}${if (rule.isRegex) " [正则]" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "${rule.source} → ${rule.target}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2
                                            )
                                            val scopes = mutableListOf<String>()
                                            if (rule.scopeTitle) scopes.add("标题")
                                            if (rule.scopeContent) scopes.add("正文")
                                            if (scopes.isNotEmpty()) {
                                                Text(
                                                    "作用: ${scopes.joinToString(", ")}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                        Column {
                                            TextButton(
                                                onClick = { loadReplaceFormForEdit(index) },
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("编辑")
                                            }
                                            TextButton(
                                                onClick = { deleteReplaceRule(index) },
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("删除", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        // 规则编辑表单
                        item {
                            Text(
                                if (replaceEditingIndex >= 0) "编辑规则" else "添加新规则",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = replaceRuleName,
                                onValueChange = { replaceRuleName = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("替换规则名称") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = replaceGroup,
                                onValueChange = { replaceGroup = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("分组") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = replaceSource,
                                onValueChange = { replaceSource = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                label = { Text("替换规则") }
                            )
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
                                value = replaceTarget,
                                onValueChange = { replaceTarget = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                label = { Text("替换为") }
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Checkbox(
                                        checked = replaceScopeTitle,
                                        onCheckedChange = { replaceScopeTitle = it }
                                    )
                                    Text("作用于标题")
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Checkbox(
                                        checked = replaceScopeContent,
                                        onCheckedChange = { replaceScopeContent = it }
                                    )
                                    Text("作用于正文")
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = replaceRange,
                                onValueChange = { replaceRange = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("替换范围，选填书名") },
                                placeholder = { Text("全部书籍") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = replaceExcludeRange,
                                onValueChange = { replaceExcludeRange = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("排除范围，选填书名") },
                                placeholder = { Text("无") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = replaceTimeout,
                                onValueChange = { replaceTimeout = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("超时毫秒数") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { saveReplaceForm() }
                    ) {
                        Text(if (replaceEditingIndex >= 0) "保存" else "添加")
                    }
                },
                dismissButton = {
                    Row {
                        if (existingRules.isNotEmpty()) {
                            TextButton(
                                onClick = { clearAllReplaceRules() }
                            ) {
                                Text("清空", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(
                            onClick = { replaceDialogVisible = false }
                        ) {
                            Text("关闭")
                        }
                    }
                }
            )
        }

        if (highlightColorDialogVisible) {
            ColorPickerDialog(
                currentColor = Color(dialogueHighlightColor),
                onColorSelected = { color ->
                    dialogueHighlightColor = color.toArgb()
                    globalPrefs.edit().putInt("dialogue_highlight_color", dialogueHighlightColor).apply()
                    highlightColorDialogVisible = false
                },
                onDismiss = { highlightColorDialogVisible = false }
            )
        }

        if (modifyHighlightColorDialogVisible) {
            ColorPickerDialog(
                currentColor = selectedModifyColor ?: Color(0xFFD32F2F),
                onColorSelected = { color ->
                    selectedModifyColor = color
                    globalPrefs.edit().putInt("last_modify_highlight_color", color.toArgb()).apply()
                    modifyHighlightColorDialogVisible = false
                },
                onDismiss = { modifyHighlightColorDialogVisible = false }
            )
        }

        if (modifyHighlightMode && selectedModifyColor == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "修改高亮模式已开启 - 先选择颜色，然后点击段落（仅引号内容变色）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        } else if (modifyHighlightMode && selectedModifyColor != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "当前颜色：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(8.dp))
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(selectedModifyColor!!)
                    )
                }
                TextButton(
                    onClick = { modifyHighlightColorDialogVisible = true }
                ) {
                    Text("换颜色")
                }
            }
        }

        if (chapterReplaceDialogVisible) {
            AlertDialog(
                onDismissRequest = { chapterReplaceDialogVisible = false },
                title = { Text("本章搜索替换") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chapterSearchValue,
                            onValueChange = { chapterSearchValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("搜索内容（仅本章）") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = chapterReplaceValue,
                            onValueChange = { chapterReplaceValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("替换为（留空则删除）") },
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("正则表达式")
                            TextButton(onClick = { chapterUseRegex = !chapterUseRegex }) {
                                Text(if (chapterUseRegex) "已开启" else "已关闭")
                            }
                        }
                        if (chapterSearchResults.isNotEmpty()) {
                            Text(
                                "找到 ${chapterSearchResults.size} 处结果，点击可替换单个",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(chapterSearchResults) { result ->
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { chapterReplaceSingle(result) }
                                ) {
                                    Text(
                                        text = "#${result.index + 1} ${result.preview}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { chapterSearchInCurrent() }) {
                            Text("搜索")
                        }
                        Button(
                            onClick = { chapterReplaceAll() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("全部替换")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { chapterReplaceDialogVisible = false }) {
                        Text("关闭")
                    }
                }
            )
        }

        if (sortDialogVisible) {
            val buttonLabels = mapOf(
                "chapters" to "目录",
                "bookmark" to "书签",
                "nextBookmark" to "去书签",
                "search" to "搜索",
                "replace" to "替换",
                "chapterReplace" to "本章替换",
                "editChapter" to "编辑本章",
                "highlightColor" to "高亮色",
                "modifyHighlight" to "修改高亮",
                "sort" to "排序",
                "settings" to "设置"
            )
            AlertDialog(
                onDismissRequest = { sortDialogVisible = false },
                title = { Text("按钮排序") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "点击按钮上移一位，长按重置排序",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        buttonOrder.forEachIndexed { idx, btnId ->
                            val label = buttonLabels[btnId] ?: btnId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .noRippleClickable {
                                        if (idx > 0) {
                                            val newOrder = buttonOrder.toMutableList()
                                            val tmp = newOrder[idx]
                                            newOrder[idx] = newOrder[idx - 1]
                                            newOrder[idx - 1] = tmp
                                            buttonOrder = newOrder
                                            globalPrefs.edit()
                                                .putString("bottom_button_order", newOrder.joinToString(","))
                                                .apply()
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (idx == 0)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${idx + 1}.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(
                            onClick = {
                                buttonOrder = defaultButtonOrder
                                globalPrefs.edit()
                                    .putString("bottom_button_order", defaultButtonOrder.joinToString(","))
                                    .apply()
                            }
                        ) {
                            Text("重置")
                        }
                        TextButton(onClick = { sortDialogVisible = false }) {
                            Text("完成")
                        }
                    }
                },
                dismissButton = {}
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
        if (line.isBlank()) return@mapNotNull null
        parseRuleLine(line).takeIf { it.source.isNotBlank() }
    }
    if (rules.isEmpty()) return this

    return map { entry ->
        when (entry) {
            is ReaderText.Text -> {
                if (rules.none { it.scopeContent }) return@map entry
                val replaced = rules.filter { it.scopeContent }.fold(entry.line.text) { current, rule ->
                    rule.apply(current)
                }
                if (replaced == entry.line.text) entry else ReaderText.Text(AnnotatedString(replaced))
            }
            is ReaderText.Chapter -> {
                if (rules.none { it.scopeTitle }) return@map entry
                val replaced = rules.filter { it.scopeTitle }.fold(entry.title) { current, rule ->
                    rule.apply(current)
                }
                if (replaced == entry.title) entry else entry.copy(title = replaced)
            }
            else -> entry
        }
    }
}

private data class ReplacementRule(
    val name: String = "",
    val group: String = "",
    val source: String,
    val target: String,
    val isRegex: Boolean = false,
    val scopeTitle: Boolean = false,
    val scopeContent: Boolean = true,
    val range: String = "",
    val excludeRange: String = "",
    val timeout: String = "3000"
) {
    fun apply(text: String): String {
        return if (isRegex) {
            runCatching { Regex(source).replace(text, target) }.getOrDefault(text)
        } else {
            text.replace(source, target)
        }
    }
}

/**
 * 解析单行规则，兼容旧格式 (source=>target / 正则:source=>target) 和 JSON 格式
 */
private fun parseRuleLine(line: String): ReplacementRule {
    val trimmed = line.trim()
    // 尝试 JSON 格式
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        return parseRuleJson(trimmed)
    }
    // 旧格式兼容: source=>target 或 正则:source=>target
    val separator = trimmed.indexOf("=>")
    if (separator <= 0) return ReplacementRule(source = "", target = "")
    val rawSource = trimmed.substring(0, separator)
    val target = trimmed.substring(separator + 2)
    val isRegex = rawSource.startsWith("正则:") || rawSource.startsWith("regex:")
    val source = rawSource.removePrefix("正则:").removePrefix("regex:")
    return ReplacementRule(
        source = source,
        target = target,
        isRegex = isRegex
    )
}

/**
 * 将规则序列化为 JSON 行
 */
private fun buildRuleJson(
    name: String,
    group: String,
    source: String,
    target: String,
    isRegex: Boolean,
    scopeTitle: Boolean,
    scopeContent: Boolean,
    range: String,
    excludeRange: String,
    timeout: String
): String {
    // 简单 JSON 构建，避免转义问题用 || 分隔字段
    return buildString {
        append("{")
        append("\"name\":\"").append(escapeJson(name)).append("\"")
        append(",\"group\":\"").append(escapeJson(group)).append("\"")
        append(",\"source\":\"").append(escapeJson(source)).append("\"")
        append(",\"target\":\"").append(escapeJson(target)).append("\"")
        append(",\"isRegex\":").append(isRegex)
        append(",\"scopeTitle\":").append(scopeTitle)
        append(",\"scopeContent\":").append(scopeContent)
        append(",\"range\":\"").append(escapeJson(range)).append("\"")
        append(",\"excludeRange\":\"").append(escapeJson(excludeRange)).append("\"")
        append(",\"timeout\":\"").append(escapeJson(timeout)).append("\"")
        append("}")
    }
}

private fun parseRuleJson(json: String): ReplacementRule {
    return try {
        val name = regexExtract(json, "\"name\"\\s*:\\s*\"(.*?)\"".toRegex())
        val group = regexExtract(json, "\"group\"\\s*:\\s*\"(.*?)\"".toRegex())
        val source = regexExtract(json, "\"source\"\\s*:\\s*\"(.*?)\"".toRegex())
        val target = regexExtract(json, "\"target\"\\s*:\\s*\"(.*?)\"".toRegex())
        val isRegex = json.contains("\"isRegex\":true")
        val scopeTitle = json.contains("\"scopeTitle\":true")
        val scopeContent = !json.contains("\"scopeContent\":false")
        val range = regexExtract(json, "\"range\"\\s*:\\s*\"(.*?)\"".toRegex())
        val excludeRange = regexExtract(json, "\"excludeRange\"\\s*:\\s*\"(.*?)\"".toRegex())
        val timeout = regexExtract(json, "\"timeout\"\\s*:\\s*\"(.*?)\"".toRegex()).ifBlank { "3000" }
        ReplacementRule(
            name = name, group = group, source = source, target = target,
            isRegex = isRegex, scopeTitle = scopeTitle, scopeContent = scopeContent,
            range = range, excludeRange = excludeRange, timeout = timeout
        )
    } catch (e: Exception) {
        ReplacementRule(source = "", target = "")
    }
}

private fun regexExtract(text: String, pattern: Regex): String {
    val match = pattern.find(text) ?: return ""
    return match.groupValues.getOrNull(1)?.let { unescapeJson(it) } ?: ""
}

private fun escapeJson(s: String): String {
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

private fun unescapeJson(s: String): String {
    return s.replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\\", "\\")
}

private data class SearchResult(
    val index: Int,
    val charIndex: Int,
    val chapter: String,
    val preview: String
)

private data class ChapterMatchResult(
    val index: Int,
    val start: Int,
    val end: Int,
    val matched: String,
    val preview: String
)

/**
 * 加载段落高亮颜色
 * 存储格式：每行 "key:colorArgb"
 * key 是 Long 类型（章节索引<<32 | 段落序号）
 */
private fun loadParagraphColors(context: Context, bookId: Int): Map<Long, Int> {
    val dir = File(context.filesDir, "paragraph_colors")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "book_${bookId}.dat")
    if (!file.exists() || file.length() <= 0) {
        // 也从 SharedPreferences 备份读
        val prefs = context.getSharedPreferences("reader_extra_$bookId", Context.MODE_PRIVATE)
        val raw = prefs.getString("paragraph_colors", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            val key = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val color = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            key to color
        }.toMap()
    }
    return runCatching {
        file.bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.split(":", limit = 2)
                val key = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                val color = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                key to color
            }.toMap()
        }
    }.getOrElse { emptyMap() }
}

/**
 * 保存段落高亮颜色（双重保险：文件 + SharedPreferences）
 */
private fun saveParagraphColors(context: Context, bookId: Int, colors: Map<Long, Int>) {
    val dir = File(context.filesDir, "paragraph_colors")
    if (!dir.exists()) dir.mkdirs()

    // 写入文件
    runCatching {
        val file = File(dir, "book_${bookId}.dat")
        val tempFile = File(file.parent, file.name + ".tmp")
        tempFile.bufferedWriter().use { writer ->
            colors.forEach { (key, color) ->
                writer.write("$key:$color\n")
            }
            writer.flush()
        }
        tempFile.renameTo(file)
    }

    // SharedPreferences 备份
    val prefs = context.getSharedPreferences("reader_extra_$bookId", Context.MODE_PRIVATE)
    val raw = colors.entries.joinToString(",") { (key, color) -> "$key:$color" }
    prefs.edit().putString("paragraph_colors", raw).apply()
}

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
        val storage = runCatching { CachedFileCompat.fromUri(context, permission.uri) }.getOrNull()
            ?: return@forEach
        if (!storage.isDirectory) return@forEach
        if (!filePath.startsWith(storage.path, ignoreCase = true)) return@forEach

        val targetUri = findFileUriByPath(storage, filePath) ?: return@forEach
        context.contentResolver.openOutputStream(targetUri, "wt")?.use { output ->
            output.write(text.toByteArray())
        } ?: throw IllegalStateException("无法打开原 TXT 文件写入流")
        return
    }

    throw IllegalStateException("找不到可写入的原 TXT 文件")
}

private fun findFileUriByPath(
    root: ua.acclorite.book_story.data.model.file.CachedFile,
    targetPath: String
): Uri? {
    root.listFiles().forEach { child ->
        if (child.path.equals(targetPath, ignoreCase = true)) return child.uri
        if (child.isDirectory && targetPath.startsWith(child.path, ignoreCase = true)) {
            findFileUriByPath(child, targetPath)?.let { return it }
        }
    }
    return null
}
