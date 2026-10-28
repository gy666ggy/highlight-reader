/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package ua.acclorite.book_story.ui.settings.general

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.acclorite.book_story.R
import ua.acclorite.book_story.data.backup.BackupManager
import ua.acclorite.book_story.ui.common.helpers.showToast
import ua.acclorite.book_story.ui.settings.components.SettingsSubcategory
import ua.acclorite.book_story.ui.settings.general.components.AppLanguageOption
import ua.acclorite.book_story.ui.settings.general.components.DoublePressExitOption

fun LazyListScope.GeneralSettingsCategory(
    topPadding: Dp = 16.dp,
    bottomPadding: Dp = 16.dp
) {
    item {
        Spacer(modifier = Modifier.height((topPadding - 8.dp).coerceAtLeast(0.dp)))
    }

    item {
        AppLanguageOption()
    }

    item {
        DoublePressExitOption()
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
    }

    BackupRestoreSubcategory()

    item {
        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

fun LazyListScope.BackupRestoreSubcategory() {
    SettingsSubcategory(
        titleColor = { MaterialTheme.colorScheme.primary },
        title = { stringResource(id = R.string.backup_restore) },
        showTitle = true,
        showDivider = true
    ) {
        item {
            BackupExportOption()
        }
        item {
            BackupImportOption()
        }
    }
}

@Composable
private fun BackupExportOption() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = BackupManager.exportToUri(context, uri)
            val msg = if (result.isSuccess) "备份成功！" else "备份失败：${result.exceptionOrNull()?.message}"
            msg.showToast(context)
        }
    }

    Row(
        modifier = Modifier
            .clickable {
                val ts = System.currentTimeMillis()
                val filename = "book_story_backup_${ts}.zip"
                exportLauncher.launch(filename)
            }
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "导出备份",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BackupImportOption() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = BackupManager.importFromUri(context, uri)
            result.message.showToast(context)
        }
    }

    Row(
        modifier = Modifier
            .clickable {
                importLauncher.launch(arrayOf("application/zip", "*/*"))
            }
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "导入备份",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}