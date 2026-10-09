package com.toparla.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.toparla.ui.theme.Spacing

/** Bu yazı ölçeğinden itibaren yan yana eylemler alt alta dizilir (Ayarlar'daki "En büyük" + sistem ölçeği). */
private const val STACK_FONT_SCALE = 1.3f

/**
 * Chip satırı (blueprint C7 `ChipRow`): sığmayan chip alt satıra geçer; %200 yazı ölçeğinde harfler alt alta
 * dizilmez (F2.23 görüntüleri). Bir satırda en çok 4 chip kuralını çağıran ekran korur.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        content = content,
    )
}

/** `ActionRow` öğesi. */
@Immutable
data class ActionItem(val label: String, val onClick: () -> Unit)

/**
 * Yan yana eşit genişlikte metin eylemleri (Ertele · Değiştir · Bitti). Büyük yazı ölçeğinde kelimeler bölünmesin
 * diye eylemler tam genişlikte alt alta dizilir (F2.23 görüntüleri: %200'de "Değiştir" ortadan bölünüyordu).
 */
@Composable
fun ActionRow(actions: List<ActionItem>, modifier: Modifier = Modifier) {
    if (LocalDensity.current.fontScale >= STACK_FONT_SCALE) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            actions.forEach { TextAction(text = it.label, onClick = it.onClick, modifier = Modifier.fillMaxWidth()) }
        }
    } else {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            actions.forEach { TextAction(text = it.label, onClick = it.onClick, modifier = Modifier.weight(1f)) }
        }
    }
}
