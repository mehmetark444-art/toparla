package com.toparla.app.medication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.toparla.app.R
import com.toparla.app.reminder.GroupLabel
import com.toparla.app.reminder.TIME
import com.toparla.domain.medication.DoseRow
import com.toparla.domain.medication.Medication
import com.toparla.ui.components.EmptyShape
import com.toparla.ui.components.EmptyState
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme
import java.time.DayOfWeek
import java.time.ZoneId

/*
 * İlaçlar ekranı (M9, F2.39; onaylı taslak `docs/tasarim/2026-10-11-ilac/Main.dc.html`): bugünün dozları ve ilaçlar.
 * Her ekran durumsuz bir `…Content` işlevi + ViewModel'e bağlayan ince sarmalayıcıdır.
 */

/** Doz satırlarının eylemleri. */
data class DoseActions(val onTake: (DoseRow) -> Unit, val onLate: (DoseRow) -> Unit, val onSkip: (DoseRow) -> Unit) {
    companion object {
        val None = DoseActions({}, {}, {})
    }
}

/** İlaçlar ekranının gezinme eylemleri. */
data class MedicationNav(
    val onBack: () -> Unit,
    val onHistory: () -> Unit,
    val onAdd: () -> Unit,
    val onEdit: (String) -> Unit,
)

@Composable
fun MedicationsScreen(viewModel: MedicationViewModel, nav: MedicationNav) {
    val ui by viewModel.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.relock() }
    if (ui.locked) {
        LockedGate(viewModel, nav.onBack)
        return
    }
    val actions = DoseActions(onTake = viewModel::take, onLate = viewModel::logLate, onSkip = viewModel::skip)
    MedicationListContent(ui, viewModel.clock.zone(), actions, nav)
    DoubleDoseDialog(ui.prompt, onYes = viewModel::confirmTaken, onNo = viewModel::dismissPrompt)
}

/** "Bugün" ve "İlaçların"; altta tek baskın eylem "İlaç ekle". */
@Composable
fun MedicationListContent(ui: MedicationsUi, zone: ZoneId, actions: DoseActions, nav: MedicationNav) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderWithAction(stringResource(R.string.med_title), stringResource(R.string.med_history), nav.onBack, nav.onHistory)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            if (ui.loaded && ui.medications.isEmpty()) {
                Spacer(Modifier.size(Spacing.xl))
                EmptyState(shape = EmptyShape.LEAF, message = stringResource(R.string.med_empty))
            } else {
                GroupLabel(stringResource(R.string.med_today))
                ListCard {
                    if (ui.today.isEmpty()) {
                        Text(text = stringResource(R.string.med_today_empty), style = ToparlaTheme.type.bodyL, modifier = Modifier.padding(vertical = Spacing.m))
                    }
                    ui.today.forEachIndexed { index, dose ->
                        if (index > 0) RowDivider()
                        DoseLine(dose.row, timeText = TIME.format(dose.time), name = dose.medication.name, detail = dose.medication.doseText, zone, actions)
                    }
                }
                GroupLabel(stringResource(R.string.med_list))
                ListCard {
                    ui.medications.forEachIndexed { index, med ->
                        if (index > 0) RowDivider()
                        MedicationLine(med) { nav.onEdit(med.id) }
                    }
                }
            }
            Text(text = stringResource(R.string.med_note), style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BottomAction(stringResource(R.string.med_add), nav.onAdd)
    }
}

@Composable
private fun MedicationLine(med: Medication, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.settingRowMin).clickable(role = Role.Button, onClick = onClick).padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(text = med.name, style = ToparlaTheme.type.bodyL)
            Text(text = scheduleText(med), style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Sağa bakan ok: geri okunun aynası.
        Icon(
            imageVector = ToparlaIcons.Back,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.rowIcon).graphicsLayer { scaleX = -1f },
        )
    }
}

/** "Her gün · 08:00, 20:00 · 12 kaldı" ya da "Pt, Ça · 09:30". */
@Composable
internal fun scheduleText(med: Medication): String {
    val dayNames = stringArrayResource(R.array.med_day_short)
    val days = if (med.everyDay) stringResource(R.string.med_every_day) else DayOfWeek.entries.filter { it in med.days }.joinToString(", ") { dayNames[it.ordinal] }
    val parts = listOfNotNull(days, med.times.joinToString(", ") { TIME.format(it) }, med.stock?.let { stringResource(R.string.med_stock_left, it) })
    return parts.joinToString(SEPARATOR)
}
