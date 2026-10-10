package com.toparla.app.reminder

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toparla.app.R
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.SetupProgress
import com.toparla.domain.reminder.SetupStep
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.components.QuietAction
import com.toparla.ui.components.TextAction
import com.toparla.ui.components.ToparlaCard
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Elevation
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

private class StepCopy(@StringRes val title: Int, @StringRes val text: Int, @StringRes val listLabel: Int, val instructions: List<Int>)

private fun stepCopy(step: SetupStep): StepCopy {
    val back = R.string.wizard_step_back
    val onPage = R.string.wizard_on_page
    return when (step) {
        SetupStep.NOTIFICATIONS -> StepCopy(R.string.wizard_notifications_title, R.string.wizard_notifications_text, onPage, listOf(R.string.wizard_notifications_step, back))
        SetupStep.CHANNELS -> StepCopy(R.string.wizard_channels_title, R.string.wizard_channels_text, onPage, listOf(R.string.wizard_channels_step, back))
        SetupStep.EXACT_ALARMS -> StepCopy(R.string.wizard_exact_title, R.string.wizard_exact_text, onPage, listOf(R.string.wizard_permission_step, back))
        SetupStep.AUTO_START ->
            StepCopy(R.string.wizard_auto_start_title, R.string.wizard_auto_start_text, onPage, listOf(R.string.wizard_step_find, R.string.wizard_step_switch, back))
        SetupStep.BATTERY -> StepCopy(R.string.wizard_battery_title, R.string.wizard_battery_text, onPage, listOf(R.string.wizard_battery_step, back))
        SetupStep.FULL_SCREEN -> StepCopy(R.string.wizard_full_screen_title, R.string.wizard_full_screen_text, onPage, listOf(R.string.wizard_permission_step, back))
        SetupStep.RECENTS_LOCK -> StepCopy(
            R.string.wizard_lock_title, R.string.wizard_lock_text, R.string.wizard_how,
            listOf(R.string.wizard_lock_step_recents, R.string.wizard_lock_step_hold, R.string.wizard_lock_step_tap),
        )
        SetupStep.SELF_TEST -> StepCopy(R.string.wizard_test_title, R.string.wizard_test_text, onPage, emptyList())
    }
}

@Composable
internal fun ColumnScope.StepView(
    step: SetupStep,
    progress: SetupProgress?,
    ui: SetupUi,
    onClose: () -> Unit,
    onOpenPage: (HealthCheck) -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onSelfTest: () -> Unit,
) {
    val copy = stepCopy(step)
    val check = step.check
    // Okunamayan ayarda sayfa açıldıktan sonra eylem "Açtım"a döner; sayfa yeniden açılabilsin diye ikincil eylem kalır.
    val awaitingConfirm = step == SetupStep.AUTO_START && ui.pageOpened
    SetupHeader(progress = progress, current = step, onClose = onClose)
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(text = stringResource(copy.title), style = ToparlaTheme.type.displayNow, modifier = Modifier.semantics { heading() })
            Text(text = stepText(step, copy, ui.report?.weakenedChannels.orEmpty()), style = ToparlaTheme.type.bodyL, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (copy.instructions.isNotEmpty()) {
            ToparlaCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(copy.listLabel), style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                copy.instructions.forEachIndexed { index, instruction -> InstructionRow(index + 1, stringResource(instruction), hint = hintFor(step, instruction)) }
                if (step == SetupStep.RECENTS_LOCK) RecentsPicture()
            }
        }
        if (awaitingConfirm && check != null) TextAction(text = stringResource(R.string.wizard_reopen), onClick = { onOpenPage(check) })
    }
    ScreenFooter {
        when {
            step == SetupStep.SELF_TEST -> PrimaryButton(text = stringResource(R.string.wizard_test_start), onClick = onSelfTest)
            step == SetupStep.RECENTS_LOCK -> PrimaryButton(text = stringResource(R.string.wizard_lock_confirm), onClick = onConfirm)
            awaitingConfirm -> PrimaryButton(text = stringResource(R.string.wizard_done), onClick = onConfirm)
            check != null -> PrimaryButton(text = stringResource(R.string.wizard_open), onClick = { onOpenPage(check) })
        }
        QuietAction(text = stringResource(R.string.wizard_later), onClick = onSkip)
    }
}

@Composable
private fun stepText(step: SetupStep, copy: StepCopy, weakened: Set<ReminderClass>): String =
    if (step == SetupStep.CHANNELS) stringResource(copy.text, classList(weakened)) else stringResource(copy.text)

private enum class StepHint { SWITCH, LOCK }

/** Yönergenin yanındaki küçük resim: açık anahtar ya da kilit simgesi (blueprint G3 "görsel açıklama"). */
private fun hintFor(step: SetupStep, @StringRes instruction: Int): StepHint? = when {
    step == SetupStep.AUTO_START && instruction == R.string.wizard_step_switch -> StepHint.SWITCH
    step == SetupStep.RECENTS_LOCK && instruction == R.string.wizard_lock_step_tap -> StepHint.LOCK
    else -> null
}

@Composable
private fun InstructionRow(number: Int, text: String, hint: StepHint?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        StepBadge(number)
        Text(text = text, style = ToparlaTheme.type.bodyL, modifier = Modifier.weight(1f))
        when (hint) {
            StepHint.SWITCH -> SwitchPicture()
            StepHint.LOCK -> LockBadge(Sizes.lockBadge)
            null -> Unit
        }
    }
}

/** Açık konumda anahtar resmi; dokunulmaz, TalkBack'e görünmez (anlamı yönerge metni taşır). */
@Composable
private fun SwitchPicture() {
    Box(
        modifier = Modifier
            .size(width = SWITCH_WIDTH, height = Sizes.stepBadge)
            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            .padding(Spacing.xxs)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(modifier = Modifier.size(Sizes.rowIcon).background(color = MaterialTheme.colorScheme.onPrimary, shape = CircleShape))
    }
}

@Composable
private fun LockBadge(size: Dp) {
    Box(
        modifier = Modifier.size(size).background(color = MaterialTheme.colorScheme.primary, shape = CircleShape).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = ToparlaIcons.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(size / 2))
    }
}

/** Son uygulamalar ekranının sadeleştirilmiş resmi: ortada, köşesinde kilit işareti olan Toparla kartı. */
@Composable
private fun RecentsPicture() {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.wizard_lock_picture)
    val density = LocalDensity.current
    // Resim sabit ölçülüdür: içindeki yazı, yazı ölçeğiyle büyüyüp kartından taşmasın (anlamı TalkBack açıklaması taşır).
    CompositionLocalProvider(LocalDensity provides Density(density = density.density, fontScale = 1f)) {
        RecentsCards(description)
    }
}

@Composable
private fun RecentsCards(description: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colors.background, shape = Shapes.button)
            .padding(vertical = Spacing.m)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(width = SIDE_CARD_WIDTH, height = SIDE_CARD_HEIGHT).background(color = colors.surfaceVariant, shape = PICTURE_CARD))
        Box {
            Column(
                modifier = Modifier
                    .size(width = MAIN_CARD_WIDTH, height = MAIN_CARD_HEIGHT)
                    .background(color = colors.surfaceVariant, shape = PICTURE_CARD)
                    .border(width = RING_STROKE, color = colors.primary, shape = PICTURE_CARD)
                    .padding(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(text = stringResource(R.string.app_name), style = ToparlaTheme.type.caption, fontWeight = FontWeight.SemiBold)
                Box(modifier = Modifier.fillMaxWidth().height(PICTURE_LINE).background(color = colors.outline, shape = CircleShape))
                Box(modifier = Modifier.fillMaxWidth(PICTURE_SHORT_LINE).height(PICTURE_LINE).background(color = colors.outline, shape = CircleShape))
            }
            Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = Sizes.rowIcon / 2, y = -(Sizes.rowIcon / 2))) { LockBadge(Sizes.rowIcon) }
        }
        Box(modifier = Modifier.size(width = SIDE_CARD_WIDTH, height = SIDE_CARD_HEIGHT).background(color = colors.surfaceVariant, shape = PICTURE_CARD))
    }
}

// Sihirbazın küçük çizimlerinin ölçüleri: yalnız bu ekranlar için; jeton değil.
internal val RING_STROKE = Elevation.border * 2
internal val RING_INSET = 1.dp
private val SWITCH_WIDTH = 52.dp
private val SIDE_CARD_WIDTH = 56.dp
private val SIDE_CARD_HEIGHT = 80.dp
private val MAIN_CARD_WIDTH = 84.dp
private val MAIN_CARD_HEIGHT = 104.dp
private val PICTURE_LINE = 6.dp
private const val PICTURE_SHORT_LINE = 0.7f
private val PICTURE_CARD = RoundedCornerShape(12.dp)
