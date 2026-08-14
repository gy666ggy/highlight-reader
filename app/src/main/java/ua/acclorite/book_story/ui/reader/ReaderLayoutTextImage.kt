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
import androidx.compose.ui.Alignment
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
    // If both filePath and imageBitmap are null, render nothing
    if (entry.filePath == null && entry.imageBitmap == null) {
        return
    }

    Box(
        modifier = Modifier
            .animateItem(
                fadeInSpec = null,
                fadeOutSpec = null
            )
            .padding(horizontal = sidePadding)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (entry.filePath != null) {
            // Use Coil for file-based images (supports GIF/WebP animation)
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(android.net.Uri.fromFile(java.io.File(entry.filePath)))
                    .build(),
                modifier = Modifier
                    .clip(RoundedCornerShape(imagesCornersRoundness))
                    .fillMaxWidth(imagesWidth),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        } else {
            // Use in-memory bitmap for regular images
            entry.imageBitmap?.let { bitmap ->
                Image(
                    modifier = Modifier
                        .clip(RoundedCornerShape(imagesCornersRoundness))
                        .fillMaxWidth(imagesWidth),
                    bitmap = bitmap,
                    contentDescription = null,
                    colorFilter = imagesColorEffects,
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}
