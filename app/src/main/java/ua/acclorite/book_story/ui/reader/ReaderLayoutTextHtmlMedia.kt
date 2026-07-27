/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import ua.acclorite.book_story.domain.model.reader.ReaderText

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LazyItemScope.ReaderLayoutTextHtmlMedia(
    entry: ReaderText.HtmlMedia,
    sidePadding: Dp,
) {
    Box(
        modifier = Modifier
            .animateItem(fadeInSpec = null, fadeOutSpec = null)
            .padding(horizontal = sidePadding)
            .fillMaxWidth()
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = false
                    settings.mediaPlaybackRequiresUserGesture = false
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    // 溢出保护
                    setInitialScale(100)
                }
            },
            update = { webView ->
                // 把相对路径转为缓存目录绝对路径
                val fixedHtml = entry.htmlContent.replace(
                    Regex("""src="([^"]+)"""),
                    { match ->
                        val src = match.groupValues?.get(1) ?: match.value
                        if (src.startsWith("http") || src.startsWith("/") || src.startsWith("file")) {
                            match.value
                        } else {
                            """src="file://${entry.cacheDir}/${src.substringAfterLast("/")}""""
                        }
                    }
                )
                webView.loadDataWithBaseURL(
                    "file://${entry.cacheDir}/",
                    fixedHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        )
    }
}
