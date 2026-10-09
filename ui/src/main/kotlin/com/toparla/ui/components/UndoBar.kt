package com.toparla.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.toparla.ui.R
import com.toparla.ui.theme.Elevation
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

/**
 * Altta "Geri al" şeridi (blueprint C7): varsayılan 10 sn, sayaç halkasıyla. Durumsuzdur: kalan saniye
 * çağırandan (ViewModel) gelir; süre bitince şeridi kaldırmak da çağıranın işidir.
 *
 * Zemin `surface`: "Geri al" metin eylemi `surfaceVariant` üstünde açık temada 4,5:1'in altına düşüyordu
 * (9 Ekim gece kontrolü, `ContrastTest`).
 */
@Composable
fun UndoBar(
    message: String,
    remainingSeconds: Int,
    totalSeconds: Int,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = stringResource(R.string.undo_action),
) {
    val fraction = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = Shapes.card,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = Elevation.small,
    ) {
        Row(
            modifier = Modifier.heightIn(min = Sizes.settingRowMin).padding(horizontal = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            ProgressRing(
                remainingFraction = fraction,
                centerText = remainingSeconds.toString(),
                size = Sizes.undoRing,
                strokeWidth = Sizes.undoRingStroke,
                announcement = null,
                textStyle = ToparlaTheme.type.caption,
            )
            Text(text = message, style = ToparlaTheme.type.body, modifier = Modifier.weight(1f))
            TextAction(text = actionLabel, onClick = onUndo)
        }
    }
}
