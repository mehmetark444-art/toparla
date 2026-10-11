package com.toparla.app.medication

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.toparla.app.R
import com.toparla.app.reminder.TIME
import com.toparla.domain.medication.MedicationForm
import com.toparla.domain.medication.MedicationInput
import com.toparla.ui.components.Chip
import com.toparla.ui.components.QuietAction
import com.toparla.ui.theme.Elevation
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme
import java.time.DayOfWeek

/*
 * İlaç formunun bölümleri ve alanları (onaylı taslak `IlacEkle.dc.html`). Eksik alan amber metinle söylenir.
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimesSection(input: MedicationInput, problems: Set<MedicationForm.Problem>, actions: MedicationFormActions) {
    Section(stringResource(R.string.med_times), MedicationForm.Problem.TIMES.takeIf { it in problems }?.let { stringResource(R.string.med_problem_times) }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            input.times.forEach { time ->
                val label = TIME.format(time)
                val remove = stringResource(R.string.med_time_remove, label)
                Chip(label = label, selected = true, onClick = { actions.onRemoveTime(time) }, modifier = Modifier.semantics { contentDescription = remove })
            }
            QuietAction(
                text = stringResource(R.string.med_time_add),
                onClick = actions.onAddTime,
                modifier = Modifier.border(width = Elevation.border, color = MaterialTheme.colorScheme.outline, shape = Shapes.button),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DaysSection(ui: MedicationFormUi, problems: Set<MedicationForm.Problem>, actions: MedicationFormActions) {
    Section(stringResource(R.string.med_days), MedicationForm.Problem.DAYS.takeIf { it in problems }?.let { stringResource(R.string.med_problem_days) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Chip(label = stringResource(R.string.med_days_every), selected = ui.everyDay, onClick = { actions.onEveryDay(true) })
            Chip(label = stringResource(R.string.med_days_some), selected = !ui.everyDay, onClick = { actions.onEveryDay(false) })
        }
        if (!ui.everyDay) {
            val names = stringArrayResource(R.array.med_day_short)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                DayOfWeek.entries.forEach { day ->
                    Chip(label = names[day.ordinal], selected = day in ui.input.days, onClick = { actions.onToggleDay(day) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GapSection(input: MedicationInput, actions: MedicationFormActions) {
    Section(stringResource(R.string.med_gap), null) {
        Text(text = stringResource(R.string.med_gap_lead), style = ToparlaTheme.type.bodyL)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            MedicationForm.GAP_OPTIONS_HOURS.forEach { hours ->
                val label = if (hours == 0) stringResource(R.string.med_gap_off) else stringResource(R.string.med_gap_hours, hours)
                Chip(label = label, selected = input.minGapHours == hours, onClick = { actions.onEdit { it.copy(minGapHours = hours) } })
            }
        }
        Hint(stringResource(R.string.med_gap_hint))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StockSection(input: MedicationInput, problems: Set<MedicationForm.Problem>, actions: MedicationFormActions) {
    Section(stringResource(R.string.med_stock), MedicationForm.Problem.STOCK.takeIf { it in problems }?.let { stringResource(R.string.med_problem_stock) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            NumberField(stringResource(R.string.med_stock_count), input.stock, Modifier.weight(1f)) { text -> actions.onEdit { it.copy(stock = text) } }
            NumberField(stringResource(R.string.med_stock_per_dose), input.perDose, Modifier.weight(1f)) { text -> actions.onEdit { it.copy(perDose = text) } }
        }
        NumberField(stringResource(R.string.med_stock_low), input.lowStock, Modifier.fillMaxWidth()) { text -> actions.onEdit { it.copy(lowStock = text) } }
    }
}

@Composable
internal fun Field(label: String, value: String, problem: String? = null, singleLine: Boolean = true, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 2,
            isError = problem != null,
            textStyle = ToparlaTheme.type.bodyL,
            shape = Shapes.button,
            modifier = Modifier.fillMaxWidth(),
        )
        if (problem != null) Problem(problem)
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter(Char::isDigit).take(MAX_DIGITS)) },
        label = { Text(label) },
        singleLine = true,
        textStyle = ToparlaTheme.type.bodyL,
        shape = Shapes.button,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun Section(title: String, problem: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(text = title, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
        if (problem != null) Problem(problem)
    }
}

/** Eksik alan amber metinle söylenir (kırmızı yok; hata değil, yapılacak küçük iş). */
@Composable
private fun Problem(text: String) {
    Text(text = text, style = ToparlaTheme.type.caption, color = ToparlaTheme.extended.onCarriedContainer)
}

@Composable
internal fun Hint(text: String) {
    Text(text = text, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun HideToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.primaryAction).clickable(role = Role.Switch) { onChange(!checked) },
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(text = stringResource(R.string.med_hide), style = ToparlaTheme.type.bodyL, fontWeight = FontWeight.SemiBold)
            Hint(stringResource(R.string.med_hide_hint))
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private const val MAX_DIGITS = 4
