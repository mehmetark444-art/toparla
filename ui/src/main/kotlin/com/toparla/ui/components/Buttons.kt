package com.toparla.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

private const val PROGRESS_TRACK_ALPHA = 0.3f

/**
 * Birincil eylem (blueprint C7): 56 dp, tam genişlik, basılınca `CONFIRM` titreşimi. Yükleniyorken metnin
 * yerinde ince ilerleme çubuğu durur; TalkBack metni okumaya devam eder.
 *
 * `critical = true` yalnız kritik hatırlatma teslimi ve kriz ekranında kullanılır (C2 renk kuralları).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    critical: Boolean = false,
) {
    val haptics = ToparlaTheme.haptics
    val container = if (critical) ToparlaTheme.extended.critical else MaterialTheme.colorScheme.primary
    val content = if (critical) ToparlaTheme.extended.onCritical else MaterialTheme.colorScheme.onPrimary
    Button(
        onClick = {
            if (!loading) {
                haptics.confirm()
                onClick()
            }
        },
        modifier = modifier.fillMaxWidth().heightIn(min = Sizes.primaryAction),
        enabled = enabled,
        shape = Shapes.buttonLarge,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        elevation = null,
        contentPadding = PaddingValues(horizontal = Spacing.l),
    ) {
        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = text },
                color = content,
                trackColor = content.copy(alpha = PROGRESS_TRACK_ALPHA),
            )
        } else {
            Text(text = text, style = ToparlaTheme.type.label)
        }
    }
}

/** İkincil düğme (blueprint C7, karar 0016): 48 dp, ikincil kart zemini, nötr yazı. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FlatButton(
        text = text,
        onClick = onClick,
        container = MaterialTheme.colorScheme.surfaceVariant,
        content = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
        enabled = enabled,
    )
}

/**
 * Metin eylemi (Ertele · Değiştir · Bitti): 48 dp kutu, vurgunun hafif zemini, vurgu renginde yazı (karar 0016).
 * Yalnız `surface` ya da `background` üstünde kullanılır: `surfaceVariant` üstünde açık temada 4,5:1'in altına düşer.
 */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FlatButton(
        text = text,
        onClick = onClick,
        container = ToparlaTheme.extended.primarySoft,
        content = MaterialTheme.colorScheme.primary,
        modifier = modifier,
        enabled = enabled,
    )
}

/**
 * Sessiz eylem ("Şimdi değil", "Vazgeç"): zeminsiz, ikincil metin renginde, 48 dp. Birincil eylemin altında durur
 * ve onunla yarışmaz (taslak onayı 9 Ekim 2026).
 */
@Composable
fun QuietAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlatButton(
        text = text,
        onClick = onClick,
        container = Color.Transparent,
        content = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        enabled = true,
        textStyle = ToparlaTheme.type.bodyL,
    )
}

/** "Düzelt" gibi Taşınan ailesinden eylem: amber zemin, koyu amber yazı (asla kırmızı). */
@Composable
internal fun CarriedAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FlatButton(
        text = text,
        onClick = onClick,
        container = ToparlaTheme.extended.carriedContainer,
        content = ToparlaTheme.extended.onCarriedContainer,
        modifier = modifier,
        enabled = true,
    )
}

@Composable
private fun FlatButton(
    text: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier,
    enabled: Boolean,
    textStyle: TextStyle = ToparlaTheme.type.label,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.minTouch),
        enabled = enabled,
        shape = Shapes.button,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        elevation = null,
        contentPadding = PaddingValues(horizontal = Spacing.m, vertical = 0.dp),
    ) {
        Text(text = text, style = textStyle)
    }
}
