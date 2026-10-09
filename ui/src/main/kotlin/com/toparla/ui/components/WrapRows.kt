package com.toparla.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toparla.ui.theme.Spacing

/**
 * Chip satırı (blueprint C7 `ChipRow`): sığmayan chip alt satıra geçer; %200 yazı ölçeğinde harfler alt alta
 * dizilmez (F2.23 görüntüleri). Bir satırda en çok 4 chip kuralını çağıran ekran korur.
 */
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) = WrapRow(modifier, content)

/**
 * Yan yana eylemler (Ertele · Değiştir · Bitti). Öğeler `Modifier.weight(1f)` ile satırı paylaşır; büyük yazı
 * ölçeğinde sığmayan öğe kelimesi bölünmeden alt satıra geçer.
 */
@Composable
fun ActionRow(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) = WrapRow(modifier, content)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrapRow(modifier: Modifier, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        content = content,
    )
}
