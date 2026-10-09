package com.toparla.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.toparla.ui.R
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

/**
 * Sohbet ve yakalama giriş çubuğu (blueprint C7 `VoiceTextBar`): solda metin alanı, sağda büyük düğme. Alan boşken
 * düğme "Konuş" (bas-konuş), yazı varken "Gönder"; ses ve yazı aynı satırda (C8: her ses girişinin yazı eşdeğeri).
 */
@Composable
fun VoiceTextBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSpeak: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = stringResource(R.string.gunes_input_hint),
) {
    val colors = MaterialTheme.colorScheme
    val fieldLabel = stringResource(R.string.cd_message_field)
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = Sizes.minTouch)
                .background(colors.surfaceVariant, CircleShape)
                .padding(horizontal = Spacing.m),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(text = hint, style = ToparlaTheme.type.body, color = colors.onSurfaceVariant)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = ToparlaTheme.type.body.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = fieldLabel },
            )
        }
        val sending = value.isNotBlank()
        FilledIconButton(
            onClick = if (sending) onSend else onSpeak,
            modifier = Modifier.size(Sizes.primaryAction),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
        ) {
            Icon(
                imageVector = if (sending) ToparlaIcons.Send else ToparlaIcons.Mic,
                contentDescription = stringResource(if (sending) R.string.cd_send else R.string.cd_speak),
            )
        }
    }
}
