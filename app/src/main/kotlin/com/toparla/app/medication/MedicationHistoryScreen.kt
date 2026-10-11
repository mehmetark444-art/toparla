package com.toparla.app.medication

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.toparla.app.R
import com.toparla.app.reminder.GroupLabel
import com.toparla.app.reminder.TIME
import com.toparla.app.reminder.dayLabel
import com.toparla.ui.components.Chip
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Elevation
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme
import java.time.LocalDate
import java.time.ZoneId

/*
 * Geçmiş ve dışa aktarım (M9, F2.40; onaylı taslak `Gecmis.dc.html`, `DisaAktar.dc.html`): kaçan doz yalnız kayıttır;
 * telafi önerilmez. Dosyanın yerini Kullanıcı seçer (karar 0018).
 */

/** Geçmiş (kaçan doz yalnız kayıt): gün gün dozlar; kaydedilmeyen dozda "Geç aldım" / "Atladım". */
@Composable
fun MedicationHistoryContent(ui: MedicationsUi, zone: ZoneId, today: LocalDate, actions: DoseActions, onBack: () -> Unit, onExport: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderWithAction(stringResource(R.string.med_history), stringResource(R.string.med_export), onBack, onExport)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            InfoNote(stringResource(R.string.med_history_info))
            if (ui.loaded && ui.history.isEmpty()) {
                Text(text = stringResource(R.string.med_history_empty), style = ToparlaTheme.type.bodyL, modifier = Modifier.padding(vertical = Spacing.m))
            }
            ui.history.groupBy { it.row.plannedAt.atZone(zone).toLocalDate() }.forEach { (date, rows) ->
                GroupLabel(historyDay(date, today))
                ListCard {
                    rows.forEachIndexed { index, item ->
                        if (index > 0) RowDivider()
                        DoseLine(item.row, timeText = TIME.format(item.row.plannedAt.atZone(zone)), name = item.name, detail = null, zone, actions)
                    }
                }
            }
            Text(text = stringResource(R.string.med_history_note), style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(Spacing.m))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationHistoryScreen(viewModel: MedicationViewModel, onBack: () -> Unit) {
    val ui by viewModel.state.collectAsState()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.relock() }
    if (ui.locked) {
        LockedGate(viewModel, onBack)
        return
    }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var range by rememberSaveable { mutableStateOf(ExportRange.DAYS_30) }
    var format by rememberSaveable { mutableStateOf(ExportFormat.PDF) }
    val result by viewModel.exportResult.collectAsState()
    // Dosyanın yerini Kullanıcı sistemin ekranında seçer (karar 0018); her biçimin kendi türü vardır.
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType)) { uri ->
        if (uri != null) viewModel.export(range, ExportFormat.CSV, uri)
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType)) { uri ->
        if (uri != null) viewModel.export(range, ExportFormat.PDF, uri)
    }
    LaunchedEffect(result) {
        result?.let { done ->
            Toast.makeText(context, if (done) R.string.med_export_done else R.string.med_export_failed, Toast.LENGTH_LONG).show()
            viewModel.exportShown()
        }
    }
    val zone = viewModel.clock.zone()
    MedicationHistoryContent(
        ui = ui,
        zone = zone,
        today = viewModel.clock.now().atZone(zone).toLocalDate(),
        actions = DoseActions(onTake = viewModel::take, onLate = viewModel::logLate, onSkip = viewModel::skip),
        onBack = onBack,
        onExport = { sheetOpen = true },
    )
    DoubleDoseDialog(ui.prompt, onYes = viewModel::confirmTaken, onNo = viewModel::dismissPrompt)
    if (sheetOpen) {
        ModalBottomSheet(onDismissRequest = { sheetOpen = false }, shape = Shapes.sheetTop, containerColor = MaterialTheme.colorScheme.surface) {
            ExportSheetContent(range, format, onRange = { range = it }, onFormat = { format = it }) {
                sheetOpen = false
                val name = viewModel.suggestedName(format)
                if (format == ExportFormat.CSV) csv.launch(name) else pdf.launch(name)
            }
        }
    }
}

/** Dışa aktarım seçimleri: aralık ve biçim; tek baskın eylem "Dosyayı kaydet". */
@Composable
fun ExportSheetContent(range: ExportRange, format: ExportFormat, onRange: (ExportRange) -> Unit, onFormat: (ExportFormat) -> Unit, onSave: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.screenEdge, end = Spacing.screenEdge, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Text(text = stringResource(R.string.med_export_title), style = ToparlaTheme.type.headline, modifier = Modifier.semantics { heading() })
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            FieldLabel(stringResource(R.string.med_export_range))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                listOf(ExportRange.DAYS_7 to R.string.med_export_7, ExportRange.DAYS_30 to R.string.med_export_30, ExportRange.DAYS_90 to R.string.med_export_90)
                    .forEach { (option, label) -> Chip(label = stringResource(label), selected = range == option, onClick = { onRange(option) }) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            FieldLabel(stringResource(R.string.med_export_format))
            FormatOption(stringResource(R.string.med_export_pdf), stringResource(R.string.med_export_pdf_hint), format == ExportFormat.PDF) { onFormat(ExportFormat.PDF) }
            FormatOption(stringResource(R.string.med_export_csv), stringResource(R.string.med_export_csv_hint), format == ExportFormat.CSV) { onFormat(ExportFormat.CSV) }
        }
        Text(text = stringResource(R.string.med_export_note), style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton(text = stringResource(R.string.med_export_save), onClick = onSave)
    }
}

@Composable
private fun historyDay(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.med_today)
    today.minusDays(1) -> stringResource(R.string.med_yesterday)
    else -> dayLabel(date, today)
}

@Composable
private fun InfoNote(text: String) {
    val ext = ToparlaTheme.extended
    Row(
        modifier = Modifier.fillMaxWidth().background(ext.infoContainer, Shapes.button).padding(Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Icon(imageVector = ToparlaIcons.InfoCircle, contentDescription = null, tint = ext.info, modifier = Modifier.size(Sizes.rowIcon))
        Text(text = text, style = ToparlaTheme.type.bodyL, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FormatOption(title: String, hint: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.settingRowMin)
            .background(if (selected) ToparlaTheme.extended.primarySoft else colors.surface, Shapes.button)
            .border(width = Elevation.border, color = if (selected) colors.primary else colors.outline, shape = Shapes.button)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(text = title, style = ToparlaTheme.type.bodyL, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        Text(text = hint, style = ToparlaTheme.type.caption, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text = text, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
