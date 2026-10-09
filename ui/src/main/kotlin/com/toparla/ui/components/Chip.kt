package com.toparla.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

/**
 * Hap biçimli seçim (blueprint C7). Seçili: `primaryContainer`. Görünür yükseklik 36 dp; dokunma alanı
 * Material'in en küçük etkileşim ölçüsüyle 48 dp'ye tamamlanır. Seçince `TICK` titreşimi.
 * Bir satırda en çok 4 chip (C7 `ChipRow`): sınırı çağıran ekran korur.
 */
@Composable
fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = ToparlaTheme.haptics
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = {
            haptics.tick()
            onClick()
        },
        modifier = modifier,
        shape = Shapes.chip,
        color = if (selected) colors.primaryContainer else colors.surfaceVariant,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Box(
            modifier = Modifier.heightIn(min = Sizes.chipVisual).padding(horizontal = Spacing.m),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = ToparlaTheme.type.label)
        }
    }
}
