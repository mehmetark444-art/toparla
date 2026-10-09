package com.toparla.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.ToparlaTheme

private const val FULL_SWEEP = 360f
private const val START_AT_TOP = -90f

/**
 * Azalan dolgu halkası, ortada kalan süre (blueprint C7). Durumsuzdur: kalan oran ve metin çağırandan gelir
 * (zaman UI'da okunmaz). Halka titremez, yanıp sönmez (C5).
 *
 * @param announcement TalkBack'in canlı bölgede okuyacağı metin; çağıran dakikada bir değiştirir.
 *   `null`: halka TalkBack'ten gizlenir (ör. `UndoBar`, kendi metnini zaten okutur).
 */
@Composable
fun ProgressRing(
    remainingFraction: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.avatarLarge,
    strokeWidth: Dp = Sizes.ringStroke,
    announcement: String? = centerText,
    textStyle: TextStyle = ToparlaTheme.type.title,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val fraction = remainingFraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(size).clearAndSetSemantics {
            if (announcement != null) {
                contentDescription = announcement
                liveRegion = LiveRegionMode.Polite
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = this.size.copy(width = this.size.width - stroke.width, height = this.size.height - stroke.width)
            val topLeft = Offset(inset, inset)
            drawArc(color = track, startAngle = 0f, sweepAngle = FULL_SWEEP, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            drawArc(color = fill, startAngle = START_AT_TOP, sweepAngle = FULL_SWEEP * fraction, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        }
        Text(text = centerText, style = textStyle, color = MaterialTheme.colorScheme.onSurface)
    }
}
