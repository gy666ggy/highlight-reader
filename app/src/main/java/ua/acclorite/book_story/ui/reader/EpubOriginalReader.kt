/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.util.zip.ZipFile

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubOriginalReader(
    filePath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val startFile = remember(filePath) {
        runCatching { prepareEpubForWebView(filePath, context.cacheDir) }.getOrNull()
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                startFile?.let { loadUrl(it.toURI().toString()) }
            }
        },
        update = { webView ->
            startFile?.let { file ->
                val url = file.toURI().toString()
                if (webView.url != url) webView.loadUrl(url)
            }
        }
    )
}

private fun prepareEpubForWebView(filePath: String, cacheDir: File): File? {
    val source = File(filePath)
    if (!source.exists() || !source.canRead()) return null

    val targetDir = File(cacheDir, "book_story_epub_${source.nameWithoutExtension}_${source.length()}")
    if (!targetDir.exists()) targetDir.mkdirs()

    ZipFile(source).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            val output = File(targetDir, entry.name)
            if (entry.isDirectory) {
                output.mkdirs()
            } else {
                output.parentFile?.mkdirs()
                if (!output.exists() || output.length() != entry.size) {
                    zip.getInputStream(entry).use { input ->
                        output.outputStream().use { outputStream -> input.copyTo(outputStream) }
                    }
                }
            }
        }
    }

    val htmlFiles = targetDir.walkTopDown().filter {
        it.isFile && listOf(".xhtml", ".html", ".htm").any { extension ->
            it.name.endsWith(extension, ignoreCase = true)
        }
    }.toList()

    return htmlFiles.firstOrNull { file ->
        file.readTextSafe().contains("epub:type=\"bodymatter\"", ignoreCase = true)
    } ?: htmlFiles.firstOrNull()
}

private fun File.readTextSafe(): String {
    return runCatching { readText() }.getOrDefault("")
}
