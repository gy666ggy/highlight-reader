/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.presentation.settings.SettingsEvent
import ua.acclorite.book_story.ui.common.components.common.IconButton
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.helpers.LocalActivity
import ua.acclorite.book_story.ui.theme.readerBarsColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReaderTopBar(
    book: Book,
    currentChapter: Chapter?,
    fastColorPresetChange: Boolean,
    currentChapterProgress: Float,
    isLoading: Boolean,
    lockMenu: Boolean,
    leave: (ReaderEvent.OnLeave) -> Unit,
    switchColorPreset: (SettingsEvent.OnSwitchColorPreset) -> Unit,
    navigateToBookInfo: (ReaderEvent.OnNavigateToBookInfo) -> Unit,
    navigateBack: (ReaderEvent.OnNavigateBack) -> Unit
) {
    val activity = LocalActivity.current
    val animatedChapterProgress = animateFloatAsState(
        targetValue = currentChapterProgress
    )

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.readerBarsColor)
            .readerColorPresetChange(
                colorPresetChangeEnabled = fastColorPresetChange,
                isLoading = isLoading,
                switchColorPreset = switchColorPreset
            )
    ) {
        TopAppBar(
            navigationIcon = {
                IconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = R.string.go_back_content_desc,
                    disableOnClick = true
                ) {
                    leave(
                        ReaderEvent.OnLeave(
                            navigate = {
                                navigateBack(ReaderEvent.OnNavigateBack)
                            }
                        )
                    )
                }
            },
            title = {
                StyledText(
                    text = book.title,
                    modifier = Modifier.padding(end = 8.dp),
                    style = LocalTextStyle.current.copy(
                        fontSize = 20.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                )
            },
            subtitle = {
                StyledText(
                    text = currentChapter?.title
                        ?: activity.getString(R.string.no_chapters),
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (currentChapter != null) {
            LinearProgressIndicator(
                progress = { animatedChapterProgress.value },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
