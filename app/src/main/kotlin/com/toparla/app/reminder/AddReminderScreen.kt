package com.toparla.app.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toparla.app.R
import com.toparla.domain.core.Clock
import com.toparla.domain.reminder.QuickTime
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderDraft
import com.toparla.ui.components.Chip
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Elevation
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class AddUi(
    val title: String = "",
    val quick: QuickTime? = QuickTime.TONIGHT,
    val custom: LocalDateTime? = null,
    val klass: ReminderClass = ReminderClass.IMPORTANT,
    val persistent: Boolean = true,
    val pastChosen: Boolean = false,
)

@HiltViewModel
class AddReminderViewModel @Inject constructor(private val creator: ReminderCreator, val clock: Clock) : ViewModel() {
    private val mutable = MutableStateFlow(AddUi())
    val state: StateFlow<AddUi> = mutable

    fun reset() = mutable.update { AddUi() }

    fun title(value: String) = mutable.update { it.copy(title = value.take(ReminderDraft.MAX_TITLE)) }

    fun quick(option: QuickTime) = mutable.update { it.copy(quick = option, custom = null, pastChosen = false) }

    fun custom(at: LocalDateTime) = mutable.update { it.copy(quick = null, custom = at, pastChosen = false) }

    fun klass(value: ReminderClass) = mutable.update { it.copy(klass = value) }

    fun persistent(value: Boolean) = mutable.update { it.copy(persistent = value) }

    /** Seçili zaman: hazır seçenek her bakışta şimdiye göre çözülür ("10 dk sonra" ekranda beklerken eskimez). */
    fun startLocal(ui: AddUi): LocalDateTime? = ui.custom ?: ui.quick?.resolve(clock.now(), clock.zone())

    fun save(onSaved: () -> Unit) {
        val ui = mutable.value
        val start = startLocal(ui) ?: return
        val draft = ReminderDraft.validate(ui.title, start, clock.now(), clock.zone())
        if (draft == null) {
            mutable.update { it.copy(pastChosen = it.title.isNotBlank()) }
            return
        }
        viewModelScope.launch {
            creator.create(draft, ui.klass, ui.persistent)
            onSaved()
        }
    }
}

/**
 * Hatırlatma ekle (F2.38; onaylı taslak `docs/tasarim/2026-10-09-hatirlatma-ekranlari/Main.dc.html`).
 * Tek ekranda dört soru; yazmak yerine seçmek (hazır zamanlar, üç önem düzeyi); altta özet ve tek birincil eylem.
 */
@Composable
fun AddReminderScreen(viewModel: AddReminderViewModel, onClose: () -> Unit) {
    LaunchedEffect(Unit) { viewModel.reset() }
    val ui by viewModel.state.collectAsState()
    val zone = viewModel.clock.zone()
    val start = viewModel.startLocal(ui)
    val whenText = start?.let { "${dayLabel(it.toLocalDate(), viewModel.clock.now().atZone(zone).toLocalDate())} ${TIME.format(it)}" }.orEmpty()
    Column(modifier = Modifier.fillMaxSize()) {
        FlowHeader(stringResource(R.string.add_title), ToparlaIcons.Close, stringResource(R.string.close), onClose)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            OutlinedTextField(
                value = ui.title,
                onValueChange = viewModel::title,
                label = { Text(stringResource(R.string.add_what)) },
                singleLine = true,
                textStyle = ToparlaTheme.type.title,
                shape = Shapes.button,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FieldLabel(stringResource(R.string.add_when))
                WhenChips(ui, viewModel)
                Text(text = whenText, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ui.pastChosen) {
                    Text(text = stringResource(R.string.add_past), style = ToparlaTheme.type.caption, color = ToparlaTheme.extended.carried)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FieldLabel(stringResource(R.string.add_importance))
                ImportanceOption(ReminderClass.CRITICAL, R.string.class_critical_hint, ui.klass, viewModel::klass)
                ImportanceOption(ReminderClass.IMPORTANT, R.string.class_important_hint, ui.klass, viewModel::klass)
                ImportanceOption(ReminderClass.NORMAL, R.string.class_normal_hint, ui.klass, viewModel::klass)
            }
            PersistentToggle(ui.persistent, viewModel::persistent)
        }
        Column(
            modifier = Modifier.padding(horizontal = Spacing.screenEdge, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(if (ui.persistent) R.string.add_summary_persistent else R.string.add_summary, classLabel(ui.klass), whenText),
                style = ToparlaTheme.type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(text = stringResource(R.string.add_save), onClick = { viewModel.save(onClose) }, enabled = ui.title.isNotBlank())
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text = text, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WhenChips(ui: AddUi, viewModel: AddReminderViewModel) {
    val context = LocalContext.current
    val options = listOf(
        QuickTime.IN_10_MIN to R.string.quick_10min,
        QuickTime.IN_1_HOUR to R.string.quick_1hour,
        QuickTime.TONIGHT to R.string.quick_tonight,
        QuickTime.TOMORROW_MORNING to R.string.quick_tomorrow,
    )
    // Dört seçenek tek satıra sığmaz; ikişerli iki satır ve altında serbest zaman.
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        options.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                pair.forEach { (option, label) -> Chip(label = stringResource(label), selected = ui.quick == option, onClick = { viewModel.quick(option) }) }
            }
        }
        Chip(
            label = stringResource(R.string.quick_custom),
            selected = ui.custom != null,
            onClick = {
                val base = viewModel.startLocal(ui) ?: LocalDateTime.ofInstant(viewModel.clock.now(), viewModel.clock.zone())
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> viewModel.custom(LocalDateTime.of(year, month + 1, day, hour, minute)) },
                            base.hour,
                            base.minute,
                            true,
                        ).show()
                    },
                    base.year,
                    base.monthValue - 1,
                    base.dayOfMonth,
                ).show()
            },
        )
    }
}

@Composable
private fun ImportanceOption(klass: ReminderClass, hint: Int, selected: ReminderClass, onSelect: (ReminderClass) -> Unit) {
    val isSelected = klass == selected
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.primaryAction)
            .background(color = if (isSelected) ToparlaTheme.extended.primarySoft else colors.surface, shape = Shapes.button)
            .border(width = Elevation.border, color = if (isSelected) colors.primary else ToparlaTheme.extended.cardBorder, shape = Shapes.button)
            .clickable(role = Role.RadioButton) { onSelect(klass) }
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        // Seçim yalnız renkle değil, dolu halkayla da belirtilir.
        Box(
            modifier = Modifier
                .size(Sizes.statusDot * 2)
                .border(width = Elevation.border * 2, color = if (isSelected) colors.primary else colors.onSurfaceVariant, shape = CircleShape)
                .padding(Spacing.xxs)
                .background(color = if (isSelected) colors.primary else colors.surface.copy(alpha = 0f), shape = CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(text = classLabel(klass), style = ToparlaTheme.type.bodyL, fontWeight = FontWeight.SemiBold)
            Text(text = stringResource(hint), style = ToparlaTheme.type.caption, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun PersistentToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.primaryAction).clickable(role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(text = stringResource(R.string.add_persistent), style = ToparlaTheme.type.bodyL, fontWeight = FontWeight.SemiBold)
            Text(text = stringResource(R.string.add_persistent_hint), style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
