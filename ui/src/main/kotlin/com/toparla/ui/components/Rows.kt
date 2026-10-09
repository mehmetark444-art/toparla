package com.toparla.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.toparla.ui.R
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

/** Bölüm başlığı (blueprint C7): `title` + isteğe bağlı sağda tek metin eylem. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth().heightIn(min = Sizes.minTouch), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = ToparlaTheme.type.title, modifier = Modifier.weight(1f).semantics { heading() })
        if (actionLabel != null && onAction != null) {
            TextAction(text = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Ayar satırı (blueprint C7): başlık, açıklama, sağda anahtar ya da değer; en az 64 dp.
 * Anahtarlı satırda dokunma tüm satıra verilsin diye çağıran `onClick` ile anahtarı birlikte değiştirir.
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.settingRowMin)
            .then(clickModifier)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(text = title, style = ToparlaTheme.type.label)
            if (description != null) {
                Text(text = description, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

/** İzin durumu. Renk tek başına anlatmaz: her durumun metni de yazılır (C8). */
enum class PermissionStatus { GRANTED, REQUIRED, OFF }

/**
 * İzin satırı (blueprint C7): izin adı, ne için, durum noktası + durum metni, verilmemişse "Düzelt".
 * "Düzelt" Taşınan ailesinin amber renginde: eksik ayar bir hata değil, yapılacak küçük bir iştir.
 */
@Composable
fun PermissionRow(
    name: String,
    purpose: String,
    status: PermissionStatus,
    onFix: () -> Unit,
    modifier: Modifier = Modifier,
    fixLabel: String = stringResource(R.string.permission_fix),
) {
    val ext = ToparlaTheme.extended
    val (dotColor, statusText) = when (status) {
        PermissionStatus.GRANTED -> ext.success to stringResource(R.string.permission_status_granted)
        PermissionStatus.REQUIRED -> ext.onCarriedContainer to stringResource(R.string.permission_status_required)
        PermissionStatus.OFF -> MaterialTheme.colorScheme.onSurfaceVariant to stringResource(R.string.permission_status_off)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.settingRowMin)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        StatusDot(color = dotColor)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(text = name, style = ToparlaTheme.type.label)
            Text(text = purpose, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = statusText, style = ToparlaTheme.type.caption, color = dotColor)
        }
        if (status != PermissionStatus.GRANTED) {
            CarriedAction(text = fixLabel, onClick = onFix)
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Canvas(modifier = Modifier.size(Sizes.statusDot)) { drawCircle(color = color) }
}
