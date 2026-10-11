package com.toparla.app.medication

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.toparla.app.R
import com.toparla.app.reminder.FlowHeader
import com.toparla.domain.medication.MedicationForm
import com.toparla.ui.components.CalmDialog
import com.toparla.ui.components.QuietAction
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Spacing
import java.time.LocalTime

@Composable
fun MedicationFormScreen(viewModel: MedicationFormViewModel, medicationId: String?, onClose: () -> Unit) {
    LaunchedEffect(medicationId) { viewModel.open(medicationId) }
    val ui by viewModel.state.collectAsState()
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val actions = MedicationFormActions(
        onClose = onClose,
        onEdit = viewModel::edit,
        onAddTime = {
            val last = ui.input.times.lastOrNull() ?: DEFAULT_TIME
            TimePickerDialog(context, { _, hour, minute -> viewModel.addTime(LocalTime.of(hour, minute)) }, last.hour, last.minute, true).show()
        },
        onRemoveTime = viewModel::removeTime,
        onEveryDay = viewModel::everyDay,
        onToggleDay = viewModel::toggleDay,
        onDelete = { confirmDelete = true },
        onSave = { viewModel.save(onClose) },
    )
    if (ui.loaded) MedicationFormContent(ui, actions)
    if (confirmDelete) {
        CalmDialog(
            message = stringResource(R.string.med_delete_confirm),
            leftLabel = stringResource(R.string.med_delete_yes),
            onLeft = {
                confirmDelete = false
                viewModel.delete(onClose)
            },
            rightLabel = stringResource(R.string.med_double_dose_no),
            onRight = { confirmDelete = false },
            onDismissRequest = { confirmDelete = false },
        )
    }
}

/**
 * İlaç ekle / düzenle (onaylı taslak `IlacEkle.dc.html`): ad, doz (serbest metin), saatler, günler, çift doz koruması,
 * stok, not, "adını gizle". Uygulama hiçbir değer önermez; eksik alan adıyla söylenir. Tek baskın eylem "Kaydet".
 */
@Composable
fun MedicationFormContent(ui: MedicationFormUi, actions: MedicationFormActions) {
    val input = ui.input
    val problems = ui.problems
    Column(modifier = Modifier.fillMaxSize()) {
        FlowHeader(stringResource(if (ui.editing) R.string.med_edit_title else R.string.med_add_title), ToparlaIcons.Close, stringResource(R.string.close), actions.onClose)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            val nameProblem = if (MedicationForm.Problem.NAME in problems) stringResource(R.string.med_problem_name) else null
            Field(stringResource(R.string.med_name), input.name, problem = nameProblem) { text -> actions.onEdit { it.copy(name = text.take(MedicationForm.MAX_NAME)) } }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Field(stringResource(R.string.med_dose), input.doseText) { text -> actions.onEdit { it.copy(doseText = text.take(MedicationForm.MAX_DOSE)) } }
                Hint(stringResource(R.string.med_dose_hint))
            }
            TimesSection(input, problems, actions)
            DaysSection(ui, problems, actions)
            GapSection(input, actions)
            StockSection(input, problems, actions)
            Field(stringResource(R.string.med_notes), input.notes, singleLine = false) { text -> actions.onEdit { it.copy(notes = text.take(MedicationForm.MAX_NOTES)) } }
            HideToggle(input.hideName) { on -> actions.onEdit { it.copy(hideName = on) } }
            if (ui.editing) QuietAction(text = stringResource(R.string.med_delete), onClick = actions.onDelete)
        }
        BottomAction(stringResource(R.string.med_save), actions.onSave)
    }
}

private val DEFAULT_TIME: LocalTime = LocalTime.of(8, 0)
