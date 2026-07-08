/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubOriginalReader(
    filePath: String,
    modifier: Modifier = Modifier,
    onCenterTap: () -> Unit
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
                setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val centerX = event.x in (view.width * 0.30f)..(view.width * 0.70f)
                        val centerY = event.y in (view.height * 0.25f)..(view.height * 0.75f)
                        if (centerX && centerY) {
                            onCenterTap()
                            return@setOnTouchListener true
                        }
                    }
                    false
                }
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

fun canUseOriginalEpubMode(filePath: String): Boolean {
    return runCatching {
        val source = File(filePath)
        if (!source.exists() || !source.canRead()) return false
        ZipFile(source).use { zip ->
            zip.entries().asSequence().any { entry ->
                !entry.isDirectory && listOf(".xhtml", ".html", ".htm").any { extension ->
                    entry.name.endsWith(extension, ignoreCase = true)
                }
            }
        }
    }.getOrDefault(false)
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

    return buildCombinedSpineDocument(targetDir)
        ?: findFirstHtmlFile(targetDir)
}

private fun buildCombinedSpineDocument(targetDir: File): File? {
    val opfFile = findOpfFile(targetDir) ?: return null
    val opfBaseDir = opfFile.parentFile ?: targetDir
    val opfDocument = Jsoup.parse(opfFile.readTextSafe(), Parser.xmlParser())

    val manifest = opfDocument.select("manifest > item").associate { item ->
        item.attr("id") to item.attr("href")
    }
    val spineFiles = opfDocument.select("spine > itemref").mapNotNull { itemRef ->
        val href = manifest[itemRef.attr("idref")] ?: return@mapNotNull null
        File(opfBaseDir, URLDecoder.decode(href.substringBefore("#"), StandardCharsets.UTF_8.name()))
            .normalize()
            .takeIf { it.exists() && it.isFile }
    }.filter { file ->
        listOf(".xhtml", ".html", ".htm").any { extension ->
            file.name.endsWith(extension, ignoreCase = true)
        }
    }
    if (spineFiles.isEmpty()) return null

    val cssLinks = linkedSetOf<String>()
    val bodySections = spineFiles.mapIndexed { index, htmlFile ->
        val document = Jsoup.parse(htmlFile.readTextSafe(), htmlFile.parentFile?.toURI()?.toString().orEmpty())
        document.select("link[rel=stylesheet][href]").forEach { link ->
            val cssFile = File(htmlFile.parentFile, link.attr("href").substringBefore("#")).normalize()
            if (cssFile.exists()) cssLinks += cssFile.toURI().toString()
        }
        document.select("[src]").forEach { element ->
            val src = element.attr("src").trim()
            if (!src.startsWith("http", ignoreCase = true) && !src.startsWith("data:", ignoreCase = true)) {
                element.attr("src", File(htmlFile.parentFile, src.substringBefore("#")).normalize().toURI().toString())
            }
        }
        document.select("[poster]").forEach { element ->
            val poster = element.attr("poster").trim()
            if (!poster.startsWith("http", ignoreCase = true) && !poster.startsWith("data:", ignoreCase = true)) {
                element.attr("poster", File(htmlFile.parentFile, poster.substringBefore("#")).normalize().toURI().toString())
            }
        }
        document.select("a[href]").forEach { element ->
            val href = element.attr("href").trim()
            if (href.startsWith("#")) {
                element.attr("href", "#chapter_$index")
            }
        }
        """<section id="chapter_$index">${document.body()?.html().orEmpty()}</section>"""
    }

    val combinedFile = File(targetDir, "book_story_combined_spine.html")
    combinedFile.writeText(
        """
        <!doctype html>
        <html>
        <head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            ${cssLinks.joinToString("\n") { """<link rel="stylesheet" href="$it"/>""" }}
            <style>
                html, body { margin: 0; padding: 0; }
                body { overflow-wrap: break-word; }
                img, video, svg { max-width: 100%; height: auto; }
                video { display: block; }
                section { margin: 0; padding: 0; }
            </style>
        </head>
        <body>
            ${bodySections.joinToString("\n")}
        </body>
        </html>
        """.trimIndent()
    )
    return combinedFile
}

private fun findOpfFile(targetDir: File): File? {
    val container = File(targetDir, "META-INF/container.xml")
    if (container.exists()) {
        val document = Jsoup.parse(container.readTextSafe(), Parser.xmlParser())
        val fullPath = document.selectFirst("rootfile")?.attr("full-path")
        if (!fullPath.isNullOrBlank()) {
            File(targetDir, fullPath).takeIf { it.exists() }?.let { return it }
        }
    }
    return targetDir.walkTopDown().firstOrNull {
        it.isFile && it.name.endsWith(".opf", ignoreCase = true)
    }
}

private fun findFirstHtmlFile(targetDir: File): File? {
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
