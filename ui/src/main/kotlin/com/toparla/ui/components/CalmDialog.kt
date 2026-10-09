package com.toparla.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

/**
 * Sakin diyalog (blueprint C7): kısa başlık (ya da yok), iki düğme, ikisi de nötr stil.
 * Yıkıcı eylem varsa **solda** durur (`leftLabel`); vurgu yok, kırmızı yok.
 */
@Composable
fun CalmDialog(
    message: String,
    leftLabel: String,
    onLeft: () -> Unit,
    rightLabel: String,
    onRight: () -> Unit,
    onDismissRequest: () -> Unit,
    title: String? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        CalmDialogContent(message, leftLabel, onLeft, rightLabel, onRight, title = title)
    }
}

/** Diyalog penceresi olmadan içerik: ekran görüntüsü testi ve önizleme bunu çizer. */
@Composable
internal fun CalmDialogContent(
    message: String,
    leftLabel: String,
    onLeft: () -> Unit,
    rightLabel: String,
    onRight: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = Shapes.nowCard,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            if (title != null) {
                Text(text = title, style = ToparlaTheme.type.title, modifier = Modifier.semantics { heading() })
            }
            Text(text = message, style = ToparlaTheme.type.bodyL)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SecondaryButton(text = leftLabel, onClick = onLeft, modifier = Modifier.weight(1f))
                SecondaryButton(text = rightLabel, onClick = onRight, modifier = Modifier.weight(1f))
            }
        }
    }
}
