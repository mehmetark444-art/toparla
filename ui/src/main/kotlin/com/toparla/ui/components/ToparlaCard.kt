package com.toparla.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import com.toparla.ui.theme.Elevation
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

/**
 * Kart yüzeyi (karar 0016): `surface` zemin, sıcak tonlu yumuşak gölge, neredeyse görünmez sınır.
 * Şimdi kartı için `shape = Shapes.nowCard`.
 */
@Composable
fun ToparlaCard(
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.card,
    contentPadding: PaddingValues = PaddingValues(Spacing.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    val ext = ToparlaTheme.extended
    Column(
        modifier = modifier
            .shadow(elevation = Elevation.card, shape = shape, ambientColor = ext.cardShadow, spotColor = ext.cardShadow)
            .border(width = Elevation.border, color = ext.cardBorder, shape = shape)
            .background(color = MaterialTheme.colorScheme.surface, shape = shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        content = content,
    )
}
