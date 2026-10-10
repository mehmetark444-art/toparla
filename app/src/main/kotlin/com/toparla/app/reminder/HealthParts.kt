package com.toparla.app.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.toparla.app.R
import com.toparla.ui.components.TextAction
import com.toparla.ui.components.ToparlaCard
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

/*
 * Hatırlatma Sağlığı, sınama ve kurulum sihirbazının ortak parçaları (onaylı taslaklar
 * `docs/tasarim/2026-10-09-hatirlatma-ekranlari/` ve `docs/tasarim/2026-10-10-kurulum-sihirbazi/`).
 * Amber metin ve ikon `onCarriedContainer` jetonuyla çizilir: `carried` açık temada metin için yeterince koyu değildir.
 */

@Composable
internal fun GroupLabel(text: String) {
    Text(
        text = text,
        style = ToparlaTheme.type.caption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
    )
}

/** Amber kart: dikkat isteyen durumun tek cümlelik özeti. Hata değil, yapılacak küçük iş; kırmızı kullanılmaz. */
@Composable
internal fun AttentionCard(title: String, text: String) {
    val ext = ToparlaTheme.extended
    Column(
        modifier = Modifier.fillMaxWidth().background(color = ext.carriedContainer, shape = Shapes.card).padding(Spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(text = title, style = ToparlaTheme.type.title, color = ext.onCarriedContainer, modifier = Modifier.semantics { heading() })
        Text(text = text, style = ToparlaTheme.type.bodyL, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Düzeltilecek satır: solda işaret (uyarı ikonu ya da sıra numarası), ortada ad ve neden, sağda tek eylem. */
@Composable
internal fun FixRow(name: String, why: String, actionLabel: String, onAction: () -> Unit, leading: @Composable () -> Unit = { AttentionIcon() }) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.settingRowMin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        leading()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(text = name, style = ToparlaTheme.type.label)
            Text(text = why, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextAction(text = actionLabel, onClick = onAction)
    }
}

/** Yerinde olan ayar: onay ikonu, ad, sağda değeri. Durum yalnız renkle anlatılmaz (ikon + metin). */
@Composable
internal fun OkRow(name: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.compactRowMin).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Icon(imageVector = ToparlaIcons.CheckCircle, contentDescription = null, tint = ToparlaTheme.extended.success, modifier = Modifier.size(Sizes.rowIcon))
        Text(text = name, style = ToparlaTheme.type.bodyL, modifier = Modifier.weight(1f))
        Text(text = value, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
    }
}

/** Bilgi satırı: etiket solda, asıl bilgi (değer) sağda ve daha belirgin. [attention]: değer amber ve ikonlu. */
@Composable
internal fun InfoRow(label: String, value: String, note: String? = null, attention: Boolean = false) {
    val valueColor = if (attention) ToparlaTheme.extended.onCarriedContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.compactRowMin).padding(vertical = Spacing.xxs).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(text = label, style = ToparlaTheme.type.bodyL)
            if (note != null) Text(text = note, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Değer kendi genişliğini alır ve sağa yaslanır; uzun değer (büyük yazı) satırın yarısını geçmeden sarar.
        Row(
            modifier = Modifier.widthIn(max = Sizes.infoValueMax),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (attention) AttentionIcon()
            Text(text = value, style = ToparlaTheme.type.label, color = valueColor, textAlign = TextAlign.End)
        }
    }
}

@Composable
internal fun AttentionIcon() {
    Icon(
        imageVector = ToparlaIcons.AlertCircle,
        contentDescription = null,
        tint = ToparlaTheme.extended.onCarriedContainer,
        modifier = Modifier.size(Sizes.rowIcon),
    )
}

/** Sıra numarası: yumuşak vurgu zeminli daire. */
@Composable
internal fun StepBadge(number: Int) {
    Box(
        modifier = Modifier.size(Sizes.stepBadge).background(color = ToparlaTheme.extended.primarySoft, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = number.toString(), style = ToparlaTheme.type.label, color = MaterialTheme.colorScheme.primary)
    }
}

/** Sonuç ekranlarının büyük, sakin işareti: yumuşak zeminli dairede ikon. Kutlama abartılmaz (konfeti, parlama yok). */
@Composable
internal fun HeroIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.size(Sizes.emptyShape).background(color = tint.copy(alpha = HERO_BACKGROUND_ALPHA), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(Sizes.heroIcon))
    }
}

private const val HERO_BACKGROUND_ALPHA = 0.16f

/** Ekranın altındaki eylem alanı: tek baskın eylem burada, başparmak bölgesinde durur. */
@Composable
internal fun ScreenFooter(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenEdge, vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/**
 * Şimdi ekranındaki sakin kurulum kartı (blueprint D1-4: "Atlanırsa Şimdi ekranında sakin bir kart kalır"; onaylı
 * taslak `SimdiKart.dc.html`). Uyarı değil, kaldığı yeri gösteren bir yer imidir: nötr renk, tek eylem.
 */
@Composable
fun SetupCard(remaining: Int, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    ToparlaCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Icon(imageVector = ToparlaIcons.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Sizes.rowIcon))
                Text(text = pluralStringResource(R.plurals.setup_card_title, remaining, remaining), style = ToparlaTheme.type.title, modifier = Modifier.semantics { heading() })
            }
            Text(text = stringResource(R.string.setup_card_text), style = ToparlaTheme.type.bodyL, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextAction(text = stringResource(R.string.setup_continue), onClick = onContinue)
    }
}
