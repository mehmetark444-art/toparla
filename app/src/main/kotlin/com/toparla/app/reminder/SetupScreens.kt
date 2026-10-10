package com.toparla.app.reminder

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.toparla.app.R
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.HealthReport
import com.toparla.domain.reminder.SelfTestOutcome
import com.toparla.domain.reminder.SetupProgress
import com.toparla.domain.reminder.SetupStep
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.components.QuietAction
import com.toparla.ui.components.ToparlaCard
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

@Composable
fun SetupScreen(viewModel: SetupViewModel, onClose: () -> Unit, onSelfTest: () -> Unit) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(Unit) { viewModel.effects.collect { onClose() } }
    val ui by viewModel.state.collectAsState()
    val context = LocalContext.current
    // Geri: adımdayken genel bakışa, genel bakıştayken dışarı. Tek adım kipinde doğrudan dışarı.
    BackHandler(enabled = ui.step != null && !ui.single) { viewModel.openOverview() }
    SetupContent(
        ui = ui,
        onClose = onClose,
        onStart = viewModel::start,
        onOpenPage = { check ->
            SettingsLinks.open(context, check, ui.report?.weakenedChannels.orEmpty())
            viewModel.pageOpened()
        },
        onConfirm = viewModel::confirm,
        onSkip = viewModel::skip,
        onSelfTest = onSelfTest,
        onFinish = viewModel::finish,
    )
}

/**
 * Durumsuz içerik (onaylı taslaklar `Sihirbaz.dc.html` ve 10 Ekim `SihirbazGiris`, `SihirbazKilit`, `SihirbazBitis`).
 * Her ekranda tek baskın eylem altta; her adım "Şimdi değil" ile atlanabilir.
 */
@Composable
fun SetupContent(
    ui: SetupUi,
    onClose: () -> Unit,
    onStart: () -> Unit,
    onOpenPage: (HealthCheck) -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onSelfTest: () -> Unit,
    onFinish: () -> Unit,
) {
    val progress = ui.progress
    val step = ui.step
    Column(modifier = Modifier.fillMaxSize()) {
        when {
            progress == null -> SetupHeader(progress = null, current = null, onClose = onClose)
            ui.finished -> DoneView(progress, ui.report, onFinish)
            step == null -> OverviewView(progress, ui.firstTime, onClose, onStart)
            else -> StepView(step, progress.takeUnless { ui.single }, ui, onClose, onOpenPage, onConfirm, onSkip, onSelfTest)
        }
    }
}

/** Üst satır: solda kapat, ortada adım noktaları (tamam: yeşil nokta, şimdiki: geniş hap, sıradaki: soluk nokta). */
@Composable
internal fun SetupHeader(progress: SetupProgress?, current: SetupStep?, onClose: () -> Unit) {
    val closeLabel = stringResource(R.string.close)
    Row(modifier = Modifier.fillMaxWidth().padding(start = Spacing.s, end = Spacing.screenEdge, top = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(Sizes.minTouch).clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = closeLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = ToparlaIcons.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (progress != null) StepDots(progress, current)
        }
        Spacer(Modifier.width(Sizes.minTouch - Spacing.s))
    }
}

@Composable
private fun StepDots(progress: SetupProgress, current: SetupStep?) {
    val position = progress.steps.indexOfFirst { it.step == current }
    val label = if (position >= 0) stringResource(R.string.setup_progress, position + 1, progress.steps.size) else stringResource(R.string.setup_done_title)
    Row(modifier = Modifier.clearAndSetSemantics { contentDescription = label }, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        progress.steps.forEach { state ->
            val isCurrent = state.step == current
            val color = when {
                isCurrent -> MaterialTheme.colorScheme.primary
                state.done -> ToparlaTheme.extended.success
                else -> MaterialTheme.colorScheme.outline
            }
            Box(modifier = Modifier.size(width = if (isCurrent) Sizes.stepDotCurrent else Sizes.stepDot, height = Sizes.stepDot).background(color = color, shape = CircleShape))
        }
    }
}

@StringRes
private fun stepName(step: SetupStep): Int = when (step) {
    SetupStep.RECENTS_LOCK -> R.string.setup_lock
    SetupStep.SELF_TEST -> R.string.setup_test
    else -> step.check?.let(::checkName) ?: R.string.setup_test
}

@Composable
private fun ColumnScope.OverviewView(progress: SetupProgress, firstTime: Boolean, onClose: () -> Unit, onStart: () -> Unit) {
    SetupHeader(progress = null, current = null, onClose = onClose)
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(text = stringResource(R.string.setup_title), style = ToparlaTheme.type.displayNow, modifier = Modifier.semantics { heading() })
            Text(text = stringResource(R.string.setup_text), style = ToparlaTheme.type.bodyL, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ToparlaCard(modifier = Modifier.fillMaxWidth()) {
            progress.steps.forEach { state -> OverviewRow(stringResource(stepName(state.step)), state.done, isNext = state.step == progress.next) }
        }
    }
    ScreenFooter {
        Text(
            text = stringResource(R.string.setup_skip_hint),
            style = ToparlaTheme.type.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(text = stringResource(if (firstTime) R.string.setup_start else R.string.setup_continue), onClick = onStart)
        QuietAction(text = stringResource(R.string.wizard_later), onClick = onClose)
    }
}

/** Genel bakış satırı. Durum yalnız renkle değil, ikon biçimi ve metinle de anlatılır. */
@Composable
private fun OverviewRow(name: String, done: Boolean, isNext: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        if (done) {
            Icon(imageVector = ToparlaIcons.CheckCircle, contentDescription = null, tint = ToparlaTheme.extended.success, modifier = Modifier.size(Sizes.rowIcon))
        } else {
            val ring = if (isNext) colors.primary else colors.outline
            Box(modifier = Modifier.size(Sizes.rowIcon).padding(RING_INSET).border(width = RING_STROKE, color = ring, shape = CircleShape))
        }
        Text(text = name, style = ToparlaTheme.type.bodyL, fontWeight = if (isNext) FontWeight.SemiBold else null, modifier = Modifier.weight(1f))
        when {
            done -> Text(text = stringResource(R.string.setup_step_done), style = ToparlaTheme.type.caption, color = colors.onSurfaceVariant)
            isNext -> Text(text = stringResource(R.string.setup_step_next), style = ToparlaTheme.type.caption, color = colors.primary)
        }
    }
}

@Composable
private fun ColumnScope.DoneView(progress: SetupProgress, report: HealthReport?, onFinish: () -> Unit) {
    val closedTest = report?.lastSelfTest?.outcome == SelfTestOutcome.ARRIVED_CLOSED
    // Kapatmak da bitirmektir: bitiş ekranı bir kez görülür.
    SetupHeader(progress = progress, current = null, onClose = onFinish)
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            HeroIcon(icon = ToparlaIcons.Check, tint = ToparlaTheme.extended.success)
            Text(
                text = stringResource(R.string.setup_done_title),
                style = ToparlaTheme.type.displayNow,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(if (closedTest) R.string.setup_done_text_closed else R.string.setup_done_text_open),
                style = ToparlaTheme.type.bodyL,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        ToparlaCard(modifier = Modifier.fillMaxWidth()) {
            progress.steps.forEach { state -> OkRow(name = stringResource(doneName(state.step)), value = stringResource(doneValue(state.step))) }
        }
        Text(
            text = stringResource(R.string.setup_done_note),
            style = ToparlaTheme.type.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    ScreenFooter { PrimaryButton(text = stringResource(R.string.setup_finish), onClick = onFinish) }
}

@StringRes
private fun doneName(step: SetupStep): Int = if (step == SetupStep.RECENTS_LOCK) R.string.setup_lock_done else stepName(step)

@StringRes
private fun doneValue(step: SetupStep): Int = when (step) {
    SetupStep.AUTO_START, SetupStep.RECENTS_LOCK -> R.string.value_confirmed
    SetupStep.BATTERY -> R.string.value_none
    SetupStep.SELF_TEST -> R.string.value_on_time
    else -> R.string.value_on
}
