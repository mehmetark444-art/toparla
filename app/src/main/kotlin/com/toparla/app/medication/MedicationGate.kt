package com.toparla.app.medication

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.toparla.app.R
import com.toparla.app.reminder.FlowHeader
import com.toparla.app.reminder.TIME
import com.toparla.reminders.AndroidReminderNotifier
import com.toparla.ui.components.CalmDialog
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.components.QuietAction
import com.toparla.ui.components.ToparlaCard
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Shapes
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

/*
 * İlaç ekranlarının kapıları: biyometrik kilit (karar 0018), Plan sekmesindeki giriş kartı ve çift doz sorusu (M9).
 */

/** Kilitli: tek baskın eylem "Kilidi aç" (sistemin parmak izi / yüz / ekran kilidi penceresi). */
@Composable
fun MedicationLockedContent(onUnlock: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        FlowHeader(stringResource(R.string.med_title), ToparlaIcons.Back, stringResource(R.string.back), onBack)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.screenEdge),
            verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(Sizes.avatarLarge).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = ToparlaIcons.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Sizes.heroIcon))
            }
            Text(text = stringResource(R.string.med_locked_title), style = ToparlaTheme.type.title, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Text(
                text = stringResource(R.string.med_locked_text),
                style = ToparlaTheme.type.bodyL,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Column(modifier = Modifier.padding(Spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            PrimaryButton(text = stringResource(R.string.med_unlock), onClick = onUnlock)
            QuietAction(text = stringResource(R.string.med_lock_back), onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Ekran açılınca sistem penceresi bir kez kendiliğinden gelir; vazgeçilirse "Kilidi aç" ile yeniden. */
@Composable
internal fun LockedGate(viewModel: MedicationViewModel, onBack: () -> Unit) {
    val activity = LocalContext.current as? Activity
    val ask = { activity?.let(viewModel::requestUnlock) }
    LaunchedEffect(Unit) { ask() }
    MedicationLockedContent(onUnlock = { ask() }, onBack = onBack)
}

/** Plan sekmesindeki giriş kartı (yalnız modül açıkken): sıradaki doz ve kaydedilmeyen doz sayısı. */
@Composable
fun MedicationPlanCard(summary: MedicationSummary, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val detail = buildList {
        add(summary.next?.let { stringResource(R.string.med_plan_next, TIME.format(it)) } ?: stringResource(R.string.med_plan_none))
        if (summary.notLogged > 0) add(pluralStringResource(R.plurals.med_plan_not_logged, summary.notLogged, summary.notLogged))
    }.joinToString(SEPARATOR)
    ToparlaCard(modifier = modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Box(
                modifier = Modifier.size(Sizes.avatarMedium).background(MaterialTheme.colorScheme.primaryContainer, Shapes.button),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = ToparlaIcons.Pill, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(Sizes.rowIcon))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(text = stringResource(R.string.med_title), style = ToparlaTheme.type.bodyL, fontWeight = FontWeight.SemiBold)
                Text(text = detail, style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Çift doz sorusu (blueprint M9): sakin diyalog, iki nötr düğme; "Vazgeç" solda. */
@Composable
fun DoubleDoseDialog(prompt: DoubleDosePrompt?, onYes: () -> Unit, onNo: () -> Unit) {
    if (prompt == null) return
    val context = LocalContext.current
    val message = remember(prompt) { AndroidReminderNotifier.doubleDoseText(context, prompt.since) }
    CalmDialog(
        message = message,
        leftLabel = stringResource(R.string.med_double_dose_no),
        onLeft = onNo,
        rightLabel = stringResource(R.string.med_double_dose_yes),
        onRight = onYes,
        onDismissRequest = onNo,
        title = stringResource(R.string.med_double_dose_title),
    )
}

/**
 * Modül kapısı (K18, F2.41): ilaç modülü kapalıyken içerik hiç çizilmez ve ekran kapanır; açıkken içerik görünür.
 * Modül ekran açıkken kapatılırsa da aynısı olur.
 */
@Composable
internal fun ModuleGate(loaded: Boolean, enabled: Boolean, onClosed: () -> Unit, content: @Composable () -> Unit) {
    LaunchedEffect(loaded, enabled) { if (loaded && !enabled) onClosed() }
    if (enabled) content()
}
