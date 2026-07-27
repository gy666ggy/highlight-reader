/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.ui.theme.model.HorizontalAlignment

@Composable
fun LazyItemScope.ReaderLayoutTextImage(
    entry: ReaderText.Image,
    sidePadding: Dp,
    imagesCornersRoundness: Dp,
    imagesAlignment: HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?
) {
    Box(
        modifier = Modifier
            .animateItem(
                fadeInSpec = null,
                fadeOutSpec = null
            )
            .padding(horizontal = sidePadding)
            .fillMaxWidth(),
        contentAlignment = imagesAlignment.alignment
    ) {
        if (entry.filePath != null) {
            // 用 Coil 加载（支持 GIF 动图）
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(entry.filePath)
                    .crossfade(true)
                    .build(),
                modifier = Modifier
                    .clip(RoundedCornerShape(imagesCornersRoundness))
                    .fillMaxWidth(imagesWidth),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        } else {
            Image(
                modifier = Modifier
                    .clip(RoundedCornerShape(imagesCornersRoundness))
                    .fillMaxWidth(imagesWidth),
                bitmap = entry.imageBitmap!!,
                contentDescription = null,
                colorFilter = imagesColorEffects,
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
