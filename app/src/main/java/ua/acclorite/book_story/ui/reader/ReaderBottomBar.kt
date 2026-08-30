/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.presentation.reader.model.Checkpoint
import ua.acclorite.book_story.ui.common.components.common.IconButton
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.helpers.noRippleClickable
import ua.acclorite.book_story.ui.common.model.Direction
import ua.acclorite.book_story.ui.theme.HorizontalExpandingTransition
import ua.acclorite.book_story.ui.theme.readerBarsColor

@Composable
fun ReaderBottomBar(
    book: Book,
    progress: String,
    text: List<ReaderText>,
    listState: LazyListState,
    lockMenu: Boolean,
    checkpoints: List<Checkpoint>,
    bottomBarPadding: Dp,
    restoreCheckpoint: (ReaderEvent.OnRestoreCheckpoint) -> Unit,
    scroll: (ReaderEvent.OnScroll) -> Unit,
    changeProgress: (ReaderEvent.OnChangeProgress) -> Unit,
    showChapters: () -> Unit,
    showSettings: () -> Unit,
    editChapter: () -> Unit,
    search: () -> Unit,
    replaceRules: () -> Unit,
    toggleBookmark: () -> Unit,
    nextBookmark: () -> Unit,
    highlightColor: () -> Unit,
    modifyHighlight: () -> Unit,
    modifyHighlightActive: Boolean = false,
    chapterReplace: () -> Unit,
    sortButtons: () -> Unit,
    buttonOrder: List<String> = emptyList()
) {
    val currentIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex
        }
    }
    val checkpointsProgress = rememberCheckpointsProgress(
        checkpoints = checkpoints,
        text = text,
        currentIndex = currentIndex
    )
    val currentCheckpoint = rememberCurrentCheckpoint(
        checkpoints = checkpoints,
        currentIndex = currentIndex
    )
    val checkpointDirection = rememberCheckpointDirection(
        currentCheckpoint = currentCheckpoint,
        currentIndex = currentIndex
    )
    val restoreCheckpoint = remember(currentCheckpoint) {
        {
            if (currentCheckpoint != null) {
                restoreCheckpoint(
                    ReaderEvent.OnRestoreCheckpoint(
                        currentCheckpoint
                    )
                )
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.readerBarsColor)
            .noRippleClickable(onClick = {})
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 8.dp + bottomBarPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StyledText(
            text = progress,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            ReaderBottomBarCheckpoints(
                checkpointDirection = checkpointDirection,
                startArrow = {
                    IconButton(
                        icon = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = R.string.checkpoint_back_content_desc,
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        disableOnClick = false
                    ) {
                        restoreCheckpoint()
                    }
                },
                endArrow = {
                    IconButton(
                        icon = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = R.string.checkpoint_forward_content_desc,
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        disableOnClick = false
                    ) {
                        restoreCheckpoint()
                    }
                },
                slider = {
                    ReaderBottomBarSlider(
                        book = book,
                        lockMenu = lockMenu,
                        listState = listState,
                        scroll = scroll,
                        changeProgress = changeProgress
                    )
                },
                indicator = {
                    ReaderBottomBarCheckpointsIndicator(
                        checkpointsProgress = checkpointsProgress
                    )
                }
            )
        }

        val defaultOrder = listOf(
            "chapters", "bookmark", "nextBookmark", "search", "replace",
            "chapterReplace", "editChapter", "highlightColor", "modifyHighlight", "sort", "settings"
        )
        val renderOrder = if (buttonOrder.isNotEmpty()) buttonOrder else defaultOrder

        val buttonRenderers: Map<String, @Composable () -> Unit> = mapOf(
            "chapters" to { BottomAction("目录", enabled = !lockMenu, onClick = showChapters) },
            "bookmark" to { BottomAction("书签", enabled = !lockMenu, onClick = toggleBookmark) },
            "nextBookmark" to { BottomAction("去书签", enabled = !lockMenu, onClick = nextBookmark) },
            "search" to { BottomAction("搜索", enabled = !lockMenu, onClick = search) },
            "replace" to { BottomAction("替换", enabled = !lockMenu, onClick = replaceRules) },
            "chapterReplace" to { BottomAction("本章替换", enabled = !lockMenu, onClick = chapterReplace) },
            "editChapter" to { BottomAction("编辑本章", enabled = !lockMenu, onClick = editChapter) },
            "highlightColor" to { BottomAction("高亮色", enabled = !lockMenu, onClick = highlightColor) },
            "modifyHighlight" to { BottomAction("修改高亮", enabled = !lockMenu, active = modifyHighlightActive, onClick = modifyHighlight) },
            "sort" to { BottomAction("排序", enabled = !lockMenu, onClick = sortButtons) },
            "settings" to { BottomAction("设置", enabled = !lockMenu, onClick = showSettings) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            renderOrder.forEach { btnId ->
                buttonRenderers[btnId]?.invoke()
            }
        }
    }
}

@Composable
private fun BottomAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    active: Boolean = false
) {
    TextButton(
        enabled = enabled,
        onClick = onClick
    ) {
        StyledText(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    active -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        )
    }
}

@Composable
private fun rememberCheckpointsProgress(
    checkpoints: List<Checkpoint>,
    text: List<ReaderText>,
    currentIndex: Int
): List<Float> {
    return remember(checkpoints, text, currentIndex) {
        if (text.isEmpty()) return@remember emptyList()

        val progressScale = 0.987f
        checkpoints.mapNotNull { checkpoint ->
            if (checkpoint.index == currentIndex) return@mapNotNull null
            (checkpoint.index / text.lastIndex.toFloat()) * progressScale
        }
    }
}

@Composable
private fun rememberCurrentCheckpoint(
    checkpoints: List<Checkpoint>,
    currentIndex: Int
): Checkpoint? {
    return remember(checkpoints, currentIndex) {
        checkpoints.lastOrNull { checkpoint ->
            when {
                checkpoint.index > currentIndex -> true
                checkpoint.index < currentIndex -> true
                else -> false
            }
        }
    }
}

@Composable
private fun rememberCheckpointDirection(
    currentCheckpoint: Checkpoint?,
    currentIndex: Int
): Direction {
    return remember(currentCheckpoint, currentIndex) {
        when {
            currentCheckpoint == null -> Direction.NEUTRAL
            currentCheckpoint.index > currentIndex -> Direction.END
            currentCheckpoint.index < currentIndex -> Direction.START
            else -> Direction.NEUTRAL
        }
    }
}

@Composable
private fun RowScope.ReaderBottomBarCheckpoints(
    checkpointDirection: Direction,
    startArrow: @Composable () -> Unit,
    endArrow: @Composable () -> Unit,
    slider: @Composable () -> Unit,
    indicator: @Composable () -> Unit
) {
    HorizontalExpandingTransition(
        visible = checkpointDirection == Direction.START,
        startDirection = true
    ) {
        startArrow()
    }

    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.CenterStart
    ) {
        slider()
        indicator()
    }

    HorizontalExpandingTransition(
        visible = checkpointDirection == Direction.END,
        startDirection = false
    ) {
        endArrow()
    }
}

@Composable
private fun ReaderBottomBarSlider(
    book: Book,
    lockMenu: Boolean,
    listState: LazyListState,
    scroll: (ReaderEvent.OnScroll) -> Unit,
    changeProgress: (ReaderEvent.OnChangeProgress) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging = interactionSource.collectIsDraggedAsState()
    val animatedProgress = animateFloatAsState(book.progress)
    val progress = remember(isDragging.value, book.progress, animatedProgress.value) {
        if (isDragging.value) book.progress
        else animatedProgress.value
    }

    Slider(
        value = progress,
        enabled = !lockMenu,
        onValueChange = {
            if (listState.layoutInfo.totalItemsCount > 0) {
                scroll(ReaderEvent.OnScroll(it))
                changeProgress(
                    ReaderEvent.OnChangeProgress(
                        progress = it,
                        firstVisibleItemIndex = listState.firstVisibleItemIndex,
                        firstVisibleItemOffset = 0
                    )
                )
            }
        },
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.15f),
            disabledActiveTrackColor = MaterialTheme.colorScheme.primary,
            disabledThumbColor = MaterialTheme.colorScheme.primary,
            disabledInactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.15f),
        )
    )
}

@Composable
private fun ReaderBottomBarCheckpointsIndicator(checkpointsProgress: List<Float>) {
    checkpointsProgress.forEach { checkpoint ->
        Row {
            Spacer(modifier = Modifier.fillMaxWidth(checkpoint))
            Box(
                Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(0.5.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(0.7f))
            )
        }
    }
}
