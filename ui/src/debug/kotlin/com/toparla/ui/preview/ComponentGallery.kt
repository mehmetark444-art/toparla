package com.toparla.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.toparla.ui.R
import com.toparla.ui.components.ActionItem
import com.toparla.ui.components.ActionRow
import com.toparla.ui.components.CalmDialogContent
import com.toparla.ui.components.Chip
import com.toparla.ui.components.ChipRow
import com.toparla.ui.components.EmptyShape
import com.toparla.ui.components.EmptyState
import com.toparla.ui.components.PermissionRow
import com.toparla.ui.components.PermissionStatus
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.components.ProgressRing
import com.toparla.ui.components.SecondaryButton
import com.toparla.ui.components.SectionHeader
import com.toparla.ui.components.SettingRow
import com.toparla.ui.components.ToparlaCard
import com.toparla.ui.components.UndoBar
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ThemeMode
import com.toparla.ui.theme.ToparlaTheme

private const val PREVIEW_RING_FRACTION = 0.6f
private const val PREVIEW_UNDO_LEFT = 7
private const val PREVIEW_UNDO_TOTAL = 10

/**
 * Eylem bileşenleri: düğmeler, chip'ler, halka, geri al şeridi. Önizleme ve ekran görüntüsü testi çizer.
 *
 * @param showEndless Bitmeyen animasyonlu öğeler (yükleniyor çubuğu). Ekran görüntüsü testinde kapalı:
 *   Compose hiç durulmaz ve çekim sonsuza dek bekler.
 */
@Composable
fun ActionGallery(showEndless: Boolean = true) {
    GalleryColumn {
        PrimaryButton(text = stringResource(R.string.preview_start), onClick = {})
        if (showEndless) {
            PrimaryButton(text = stringResource(R.string.preview_start), onClick = {}, loading = true)
        }
        ActionRow(
            actions = listOf(
                ActionItem(stringResource(R.string.preview_snooze)) {},
                ActionItem(stringResource(R.string.preview_change)) {},
                ActionItem(stringResource(R.string.preview_finish)) {},
            ),
        )
        SecondaryButton(text = stringResource(R.string.preview_later_10), onClick = {}, modifier = Modifier.fillMaxWidth())
        ChipRow {
            Chip(label = stringResource(R.string.preview_chip_15), selected = true, onClick = {})
            Chip(label = stringResource(R.string.preview_chip_today_later), selected = false, onClick = {})
            Chip(label = stringResource(R.string.preview_chip_tomorrow), selected = false, onClick = {})
            Chip(label = stringResource(R.string.preview_chip_someday), selected = false, onClick = {})
        }
        ProgressRing(remainingFraction = PREVIEW_RING_FRACTION, centerText = stringResource(R.string.preview_ring_text))
        UndoBar(
            message = stringResource(R.string.preview_undo_message),
            remainingSeconds = PREVIEW_UNDO_LEFT,
            totalSeconds = PREVIEW_UNDO_TOTAL,
            onUndo = {},
        )
        // Kritik renk yalnız kritik teslim ve kriz ekranında; burada yalnız görünümü sınanır.
        PrimaryButton(text = stringResource(R.string.preview_done), onClick = {}, critical = true)
    }
}

/** Satır ve yüzey bileşenleri: bölüm başlığı, ayar ve izin satırları, boş durum, diyalog içeriği. */
@Composable
fun RowGallery() {
    GalleryColumn {
        SectionHeader(
            title = stringResource(R.string.preview_section),
            actionLabel = stringResource(R.string.preview_section_action),
            onAction = {},
        )
        ToparlaCard(contentPadding = PaddingValues(Spacing.xs)) {
            SettingRow(
                title = stringResource(R.string.preview_setting_title),
                description = stringResource(R.string.preview_setting_desc),
                onClick = {},
                trailing = { Switch(checked = true, onCheckedChange = null) },
            )
            PermissionRow(
                name = stringResource(R.string.preview_perm_notifications),
                purpose = stringResource(R.string.preview_perm_notifications_why),
                status = PermissionStatus.GRANTED,
                onFix = {},
            )
            PermissionRow(
                name = stringResource(R.string.preview_perm_battery),
                purpose = stringResource(R.string.preview_perm_battery_why),
                status = PermissionStatus.REQUIRED,
                onFix = {},
            )
            PermissionRow(
                name = stringResource(R.string.preview_perm_dnd),
                purpose = stringResource(R.string.preview_perm_dnd_why),
                status = PermissionStatus.OFF,
                onFix = {},
            )
        }
        EmptyState(
            shape = EmptyShape.RING,
            message = stringResource(R.string.preview_empty),
            actionLabel = stringResource(R.string.preview_empty_action),
            onAction = {},
        )
        CalmDialogContent(
            title = stringResource(R.string.preview_dialog_title),
            message = stringResource(R.string.preview_dialog_message),
            leftLabel = stringResource(R.string.preview_dialog_left),
            onLeft = {},
            rightLabel = stringResource(R.string.preview_dialog_right),
            onRight = {},
        )
    }
}

@Composable
private fun GalleryColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) { content() }
}

@Preview(name = "Eylemler · koyu", widthDp = 412, heightDp = 915)
@Composable
internal fun ActionGalleryDarkPreview() = ToparlaTheme(mode = ThemeMode.DARK) { ActionGallery() }

@Preview(name = "Eylemler · açık", widthDp = 412, heightDp = 915)
@Composable
internal fun ActionGalleryLightPreview() = ToparlaTheme(mode = ThemeMode.LIGHT) { ActionGallery() }

@Preview(name = "Satırlar · koyu", widthDp = 412, heightDp = 915)
@Composable
internal fun RowGalleryDarkPreview() = ToparlaTheme(mode = ThemeMode.DARK) { RowGallery() }

@Preview(name = "Satırlar · AMOLED", widthDp = 412, heightDp = 915)
@Composable
internal fun RowGalleryAmoledPreview() = ToparlaTheme(mode = ThemeMode.AMOLED) { RowGallery() }
