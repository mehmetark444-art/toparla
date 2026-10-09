package com.toparla.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

// Güneş ışığı (karar 0016): halkanın çevresinde çok hafif, durağan, sıcak bir parıltı. Yanıp sönmez.
private const val GLOW_ALPHA = 0.3f
private const val GLOW_REACH = 0.85f
private const val RING_RATIO = 0.05f

/**
 * Güneş avatarı (blueprint C6): soyut, yüzsüz daire + ince `sun` halka. Yüz, maskot, karakter yok.
 * Boyutlar 24 / 40 / 72 dp (`Sizes.avatar*`). Avatarın kendisi TalkBack'te okunmaz; saran düğme okutur.
 */
@Composable
fun GunesAvatar(modifier: Modifier = Modifier, size: Dp = Sizes.avatarMedium) {
    val sun = ToparlaTheme.extended.sun
    val ring = size * RING_RATIO
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(sun.copy(alpha = GLOW_ALPHA), Color.Transparent),
                        radius = this.size.minDimension * GLOW_REACH,
                    ),
                    radius = this.size.minDimension * GLOW_REACH,
                )
            }
            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
            .border(width = ring, color = sun, shape = CircleShape),
    )
}

/** Güneş mesajı (blueprint C7 `GunesBubble`): `tertiaryContainer`, solda 24 dp avatar, kuyruk yok. */
@Composable
fun GunesBubble(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.Top) {
        GunesAvatar(size = Sizes.avatarSmall)
        Surface(
            shape = Shapes.bubble,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ) {
            Text(text = text, style = ToparlaTheme.type.bodyL, modifier = Modifier.padding(Spacing.m))
        }
    }
}
