package com.toparla.app.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.toparla.app.R
import com.toparla.domain.reminder.DeliveryTiming
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.HealthReport
import com.toparla.domain.reminder.SelfTestOutcome
import com.toparla.domain.reminder.SelfTestRecord
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.components.ToparlaCard
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme
import java.time.LocalDate
import java.time.ZoneId

/**
 * Hatırlatma Sağlığı (F2.35, blueprint D23). Otomatik başlatma okunamadığı için "Düzelt"i ayar sayfasına değil,
 * anlatımlı sihirbaz adımına götürür ([onAutoStart]); ötekiler doğrudan ilgili ayar sayfasını açar.
 */
@Composable
fun HealthScreen(viewModel: HealthViewModel, onBack: () -> Unit, onAutoStart: () -> Unit, onSelfTest: () -> Unit) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val report by viewModel.state.collectAsState()
    val context = LocalContext.current
    val zone = viewModel.clock.zone()
    HealthContent(
        report = report,
        zone = zone,
        today = viewModel.clock.now().atZone(zone).toLocalDate(),
        onBack = onBack,
        onFix = { check -> if (check == HealthCheck.AUTO_START) onAutoStart() else SettingsLinks.open(context, check, report?.weakenedChannels.orEmpty()) },
        onSelfTest = onSelfTest,
    )
}

/**
 * Durumsuz içerik (onaylı taslaklar `Saglik.dc.html` ve 10 Ekim `Main.dc.html`). Eksik ayar hata değil, yapılacak
 * küçük iştir: amber ve "Düzelt"; kırmızı yok. Rapor henüz okunmadıysa yalnız başlık ve eylem görünür.
 */
@Composable
fun HealthContent(
    report: HealthReport?,
    zone: ZoneId,
    today: LocalDate,
    onBack: () -> Unit,
    onFix: (HealthCheck) -> Unit,
    onSelfTest: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FlowHeader(stringResource(R.string.health_title), ToparlaIcons.Back, stringResource(R.string.back), onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            if (report != null) {
                HealthSummary(report.toFix.size)
                if (report.toFix.isNotEmpty()) {
                    GroupLabel(stringResource(R.string.health_to_fix))
                    report.toFix.forEach { check ->
                        FixRow(
                            name = stringResource(checkName(check)),
                            why = checkWhy(check, report.weakenedChannels),
                            actionLabel = stringResource(R.string.health_fix),
                            onAction = { onFix(check) },
                        )
                    }
                }
                if (report.ok.isNotEmpty()) {
                    GroupLabel(stringResource(R.string.health_ok))
                    report.ok.forEach { check -> OkRow(name = stringResource(checkName(check)), value = okValue(check, report)) }
                }
                GroupLabel(stringResource(R.string.health_recent))
                RecentRows(report, zone, today)
            }
        }
        ScreenFooter {
            Text(
                text = stringResource(R.string.health_test_hint),
                style = ToparlaTheme.type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(text = stringResource(R.string.health_test), onClick = onSelfTest)
        }
    }
}

@Composable
private fun HealthSummary(missing: Int) {
    if (missing > 0) {
        AttentionCard(title = pluralStringResource(R.plurals.health_missing, missing, missing), text = stringResource(R.string.health_missing_text))
        return
    }
    ToparlaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Icon(imageVector = ToparlaIcons.Shield, contentDescription = null, tint = ToparlaTheme.extended.success, modifier = Modifier.size(Sizes.fabIcon))
            Text(text = stringResource(R.string.health_all_good), style = ToparlaTheme.type.title, modifier = Modifier.semantics { heading() })
        }
        Text(text = stringResource(R.string.health_all_good_text), style = ToparlaTheme.type.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Yerinde olan ayarın sağında görünen değer: çoğu "Açık"; okunamayan ayarda kimin onayladığı, kovada düzeyi. */
@Composable
private fun okValue(check: HealthCheck, report: HealthReport): String = when (check) {
    HealthCheck.AUTO_START -> stringResource(R.string.value_confirmed)
    HealthCheck.BATTERY -> stringResource(R.string.value_none)
    HealthCheck.STANDBY_BUCKET -> bucketLabel(report.standbyBucket)
    else -> stringResource(R.string.value_on)
}

@Composable
private fun RecentRows(report: HealthReport, zone: ZoneId, today: LocalDate) {
    InfoRow(label = stringResource(R.string.health_last), value = lastDeliveryText(report.lastDelivery, zone, today))
    InfoRow(label = stringResource(R.string.health_pending), value = report.upcomingReminders.toString())
    val missed = report.missedLastWeek
    InfoRow(
        label = stringResource(R.string.health_missed),
        value = if (missed == 0) stringResource(R.string.value_none) else missed.toString(),
        note = stringResource(if (missed == 0) R.string.health_missed_note else R.string.health_missed_note_some),
        attention = missed > 0,
    )
    InfoRow(label = stringResource(R.string.health_last_test), value = lastTestText(report.lastSelfTest, zone, today))
}

@Composable
private fun lastDeliveryText(delivery: DeliveryTiming?, zone: ZoneId, today: LocalDate): String {
    if (delivery == null) return stringResource(R.string.health_last_none)
    val late = delivery.lateBy
    val status = if (late.isZero) {
        stringResource(R.string.health_on_time)
    } else {
        // En yakın dakikaya yuvarlanır; tolerans dışı gecikme en az bir dakikadır.
        stringResource(R.string.health_late_min, ((late.seconds + HALF_MINUTE_SEC) / MINUTE_SEC).coerceAtLeast(1))
    }
    return stringResource(R.string.health_when_status, whenLabel(delivery.plannedAt, zone, today), status)
}

@Composable
private fun lastTestText(record: SelfTestRecord?, zone: ZoneId, today: LocalDate): String {
    if (record == null) return stringResource(R.string.health_last_test_none)
    val outcome = stringResource(
        when (record.outcome) {
            SelfTestOutcome.ARRIVED_CLOSED -> R.string.outcome_arrived
            SelfTestOutcome.ARRIVED_OPEN -> R.string.outcome_arrived_open
            SelfTestOutcome.LATE -> R.string.outcome_late
            SelfTestOutcome.NOT_ARRIVED -> R.string.outcome_not_arrived
        },
    )
    return stringResource(R.string.health_when_status, whenLabel(record.at, zone, today), outcome)
}

private const val MINUTE_SEC = 60L
private const val HALF_MINUTE_SEC = 30L
