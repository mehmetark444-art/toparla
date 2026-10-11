package com.toparla.app.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.toparla.app.R
import com.toparla.app.reminder.FlowHeader
import com.toparla.app.reminder.TIME
import com.toparla.domain.medication.DoseRow
import com.toparla.domain.medication.DoseState
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.components.SecondaryButton
import com.toparla.ui.components.TextAction
import com.toparla.ui.components.ToparlaCard
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Elevation
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme
import java.time.ZoneId

/*
 * İlaç ekranlarının ortak parçaları. "Kaydedilmedi" amber ve metinle söylenir; kırmızı yok, "kaçırdın" yok.
 */

@Composable
internal fun DoseLine(row: DoseRow, timeText: String, name: String, detail: String?, zone: ZoneId, actions: DoseActions) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text(text = timeText, style = ToparlaTheme.type.bodyL, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(Sizes.minTouch))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(text = name, style = ToparlaTheme.type.bodyL)
                if (!detail.isNullOrBlank()) Text(text = detail, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DoseStatus(row, zone)
        }
        if (row.actionable) {
            Row(modifier = Modifier.padding(start = Sizes.minTouch + Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (row.state == DoseState.NOT_LOGGED) {
                    TextAction(text = stringResource(R.string.med_log_late), onClick = { actions.onLate(row) })
                } else {
                    TextAction(text = stringResource(R.string.med_taken), onClick = { actions.onTake(row) })
                }
                SecondaryButton(text = stringResource(R.string.med_log_skip), onClick = { actions.onSkip(row) })
            }
        }
    }
}

/** Durum yalnız renkle değil, ikon ve metinle de söylenir. */
@Composable
private fun DoseStatus(row: DoseRow, zone: ZoneId) {
    val ext = ToparlaTheme.extended
    val logged = row.loggedAt?.let { TIME.format(it.atZone(zone)) }.orEmpty()
    when (row.state) {
        DoseState.TAKEN, DoseState.LATE -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Icon(imageVector = ToparlaIcons.Check, contentDescription = null, tint = ext.success, modifier = Modifier.size(Sizes.badgeIcon + Spacing.xxs))
            Text(
                text = stringResource(if (row.state == DoseState.TAKEN) R.string.med_state_taken else R.string.med_state_late, logged),
                style = ToparlaTheme.type.caption,
                color = ext.success,
            )
        }
        DoseState.NOT_LOGGED -> Row(
            modifier = Modifier.background(ext.carriedContainer, Shapes.button).padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            Icon(imageVector = ToparlaIcons.AlertCircle, contentDescription = null, tint = ext.onCarriedContainer, modifier = Modifier.size(Sizes.badgeIcon + Spacing.xxs))
            Text(text = stringResource(R.string.med_state_not_logged), style = ToparlaTheme.type.caption, color = ext.onCarriedContainer)
        }
        else -> Text(text = stringResource(quietStateLabel(row.state)), style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun quietStateLabel(state: DoseState): Int = when (state) {
    DoseState.SKIPPED -> R.string.med_state_skipped
    DoseState.PENDING -> R.string.med_state_pending
    DoseState.SNOOZED -> R.string.med_state_snoozed
    else -> R.string.med_state_upcoming
}

@Composable
internal fun HeaderWithAction(title: String, action: String, onBack: () -> Unit, onAction: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(end = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) { FlowHeader(title, ToparlaIcons.Back, stringResource(R.string.back), onBack) }
        TextAction(text = action, onClick = onAction, modifier = Modifier.padding(top = Spacing.m))
    }
}

@Composable
internal fun RowDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = Elevation.border)

@Composable
internal fun BottomAction(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = Spacing.screenEdge, vertical = Spacing.m)) { PrimaryButton(text = text, onClick = onClick) }
}

/** Satır listesi kartı: satırlar arası ayırıcı çizgi, kartın kendi aralığı yok. */
@Composable
internal fun ListCard(content: @Composable () -> Unit) {
    ToparlaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = Spacing.cardPadding, vertical = Spacing.xs)) {
        Column { content() }
    }
}

internal const val SEPARATOR = " · "
