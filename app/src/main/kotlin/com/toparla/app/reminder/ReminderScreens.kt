package com.toparla.app.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toparla.app.R
import com.toparla.data.db.AlarmKind
import com.toparla.data.db.ReminderStore
import com.toparla.data.db.UpcomingRow
import com.toparla.data.db.WaitingRow
import com.toparla.domain.core.Clock
import com.toparla.domain.reminder.ReminderAction
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderEngine
import com.toparla.ui.components.EmptyShape
import com.toparla.ui.components.EmptyState
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.components.SecondaryButton
import com.toparla.ui.components.TextAction
import com.toparla.ui.components.ToparlaCard
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Elevation
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

internal val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("tr"))

@Composable
internal fun classLabel(klass: ReminderClass): String = stringResource(
    when (klass) {
        ReminderClass.CRITICAL -> R.string.class_critical
        ReminderClass.IMPORTANT -> R.string.class_important
        ReminderClass.NORMAL -> R.string.class_normal
        ReminderClass.INFO -> R.string.class_info
    },
)

/** "Bugün", "Yarın" ya da "12 Eki". */
@Composable
internal fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.plan_today)
    today.plusDays(1) -> stringResource(R.string.plan_tomorrow)
    else -> DAY.format(date)
}

/** Tam ekran akışların üst satırı: solda kapat ya da geri, yanında başlık (taslak onayı 9 Ekim). */
@Composable
fun FlowHeader(title: String, icon: ImageVector, iconLabel: String, onLeave: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = Spacing.s, end = Spacing.screenEdge, top = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(Sizes.minTouch).clickable(role = Role.Button, onClick = onLeave).semantics { contentDescription = iconLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Spacing.xs))
        Text(text = title, style = ToparlaTheme.type.title, modifier = Modifier.semantics { heading() })
    }
}

data class PlanUi(val waiting: List<WaitingRow> = emptyList(), val upcoming: List<UpcomingRow> = emptyList(), val loaded: Boolean = false)

@HiltViewModel
class PlanViewModel @Inject constructor(
    store: ReminderStore,
    private val engine: ReminderEngine,
    private val creator: ReminderCreator,
    val clock: Clock,
) : ViewModel() {
    val state: StateFlow<PlanUi> = combine(store.observeWaiting(), store.observeUpcoming()) { waiting, upcoming -> PlanUi(waiting, upcoming, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), PlanUi())

    fun act(occurrenceKey: String, action: ReminderAction) {
        viewModelScope.launch { engine.onAction(occurrenceKey, action, clock.now()) }
    }

    fun delete(reminderId: String) {
        viewModelScope.launch { creator.delete(reminderId) }
    }

    private companion object {
        const val STOP_AFTER_MS = 5_000L
    }
}

/**
 * Plan sekmesi (F2.38'in listesi): yanıt bekleyen işler kartta, sıradakiler satırda. Tek baskın eylem sağ alttaki
 * ekleme düğmesi. Geciken iş "Taşınan" ve amber; kırmızı yok.
 */
@Composable
fun PlanScreen(viewModel: PlanViewModel, onAdd: () -> Unit, onHealth: () -> Unit) {
    val ui by viewModel.state.collectAsState()
    val zone = viewModel.clock.zone()
    Box(modifier = Modifier.fillMaxSize()) {
        if (ui.loaded && ui.waiting.isEmpty() && ui.upcoming.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(
                    shape = EmptyShape.PATH,
                    message = stringResource(R.string.plan_empty),
                    actionLabel = stringResource(R.string.plan_empty_action),
                    onAction = onAdd,
                )
                TextAction(text = stringResource(R.string.health_title), onClick = onHealth)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.screenEdge),
                verticalArrangement = Arrangement.spacedBy(Spacing.betweenCards),
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextAction(text = stringResource(R.string.plan_health), onClick = onHealth)
                    }
                }
                if (ui.waiting.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.plan_waiting)) }
                    items(ui.waiting, key = { it.occurrenceKey }) { row -> WaitingCard(row, zone, viewModel::act) }
                }
                if (ui.upcoming.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.plan_upcoming)) }
                    items(ui.upcoming, key = { it.alarmKey }) { row -> UpcomingLine(row, zone, viewModel.clock.now(), viewModel::delete) }
                }
                item { Spacer(Modifier.size(Sizes.captureFab + Spacing.xl)) }
            }
        }
        AddFab(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(end = Spacing.screenEdge, bottom = Spacing.m))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = ToparlaTheme.type.caption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
    )
}

@Composable
private fun WaitingCard(row: WaitingRow, zone: ZoneId, onAction: (String, ReminderAction) -> Unit) {
    val since = row.since?.let { TIME.format(Instant.ofEpochMilli(it).atZone(zone)) }.orEmpty()
    ToparlaCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(text = row.title, style = ToparlaTheme.type.title)
            Text(
                text = stringResource(if (row.persistent) R.string.plan_since_persistent else R.string.plan_since, since),
                style = ToparlaTheme.type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(text = stringResource(R.string.reminder_done), onClick = { onAction(row.occurrenceKey, ReminderAction.DONE) })
        SecondaryButton(
            text = stringResource(R.string.reminder_not_today),
            onClick = { onAction(row.occurrenceKey, ReminderAction.NOT_TODAY) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun UpcomingLine(row: UpcomingRow, zone: ZoneId, now: Instant, onDelete: (String) -> Unit) {
    val at = Instant.ofEpochMilli(row.fireAt).atZone(zone)
    val carried = row.kind == AlarmKind.SNOOZE
    val day = dayLabel(at.toLocalDate(), now.atZone(zone).toLocalDate())
    val tag = if (carried) stringResource(R.string.plan_carried) else classLabel(row.klass)
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.settingRowMin), verticalAlignment = Alignment.CenterVertically) {
        Text(text = TIME.format(at), style = ToparlaTheme.type.bodyL, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(Sizes.settingRowMin))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(text = row.title, style = ToparlaTheme.type.bodyL)
            Text(
                text = stringResource(R.string.row_meta, day, tag),
                style = ToparlaTheme.type.caption,
                // Taşınan iş amber ve metinle belirtilir (yalnız renkle değil); kırmızı kullanılmaz.
                color = if (carried) ToparlaTheme.extended.carried else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextAction(text = stringResource(R.string.reminder_delete), onClick = { onDelete(row.reminderId) })
    }
}

@Composable
private fun AddFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.plan_add)
    val shadow = ToparlaTheme.extended.cardShadow
    Box(
        modifier = modifier
            .size(Sizes.captureFab)
            .shadow(elevation = Elevation.small, shape = CircleShape, ambientColor = shadow, spotColor = shadow)
            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = ToparlaIcons.Plus, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(Sizes.fabIcon))
    }
}
