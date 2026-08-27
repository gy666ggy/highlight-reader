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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ua.acclorite.book_story.domain.model.reader.ReaderText

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LazyItemScope.ReaderLayoutTextHtmlMedia(
    entry: ReaderText.HtmlMedia,
    sidePadding: Dp,
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    // 屏幕宽度（px），减去两侧 padding 后作为媒体宽度
    val screenWidthPx = context.resources.displayMetrics.widthPixels
    val paddingPx = (sidePadding.value * density * 2).toInt()
    val mediaWidthPx = (screenWidthPx - paddingPx).coerceAtLeast(1)

    // 判断是否为纯音频（无 video 标签时用小高度）
    val isAudioOnly = !entry.htmlContent.contains("<video", ignoreCase = true)
    val mediaHeightPx = if (isAudioOnly) 80 else (mediaWidthPx * 9 / 16).coerceAtLeast(1)
    val mediaHeightDp = (mediaHeightPx / density).dp

    Box(
        modifier = Modifier
            .animateItem(fadeInSpec = null, fadeOutSpec = null)
            .padding(horizontal = sidePadding)
            .fillMaxWidth()
            .height(mediaHeightDp)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                }
            },
            update = { webView ->
                // 把相对路径转为缓存目录绝对路径（兼容双引号和单引号）
                val fixedHtml = entry.htmlContent
                    .replace(
                        Regex("""src="([^"]+)"""),
                        { match ->
                            val src = match.groupValues.getOrNull(1) ?: return@replace match.value
                            if (src.startsWith("http") || src.startsWith("/") || src.startsWith("file")) {
                                match.value
                            } else {
                                """src="file://${entry.cacheDir}/${src.substringAfterLast("/")}""""
                            }
                        }
                    )
                    .replace(
                        Regex("""src='([^']+)"""),
                        { match ->
                            val src = match.groupValues.getOrNull(1) ?: return@replace match.value
                            if (src.startsWith("http") || src.startsWith("/") || src.startsWith("file")) {
                                match.value
                            } else {
                                """src='file://${entry.cacheDir}/${src.substringAfterLast("/")}'"""
                            }
                        }
                    )

                // 包装成完整 HTML，确保视频满宽播放
                val bg = if (isAudioOnly) "transparent" else "#000"
                val wrappedHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { background: $bg; overflow: hidden; }
                        video, audio { width: 100% !important; max-width: 100% !important; }
                        video { height: 100% !important; object-fit: contain; }
                        img { width: 100% !important; height: auto !important; }
                    </style>
                    </head>
                    <body>
                    $fixedHtml
                    </body>
                    </html>
                """.trimIndent()

                webView.loadDataWithBaseURL(
                    "file://${entry.cacheDir}/",
                    wrappedHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        )
    }
}
