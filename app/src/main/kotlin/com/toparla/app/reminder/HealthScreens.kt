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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import com.toparla.app.R
import com.toparla.data.db.CreatedBy
import com.toparla.data.db.ReminderStore
import com.toparla.data.settings.SettingsStore
import com.toparla.domain.core.Clock
import com.toparla.domain.reminder.AlarmKey
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.HealthReport
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderDraft
import com.toparla.domain.reminder.SelfTest
import com.toparla.domain.reminder.SelfTestResult
import com.toparla.ui.components.PermissionRow
import com.toparla.ui.components.PermissionStatus
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.components.ProgressRing
import com.toparla.ui.components.SettingRow
import com.toparla.ui.components.TextAction
import com.toparla.ui.components.ToparlaCard
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private val LAST_SEEN: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.forLanguageTag("tr"))

private fun checkName(check: HealthCheck): Int = when (check) {
    HealthCheck.NOTIFICATIONS -> R.string.check_notifications
    HealthCheck.EXACT_ALARMS -> R.string.check_exact_alarms
    HealthCheck.AUTO_START -> R.string.check_auto_start
    HealthCheck.BATTERY -> R.string.check_battery
    HealthCheck.FULL_SCREEN -> R.string.check_full_screen
    HealthCheck.DND_ACCESS -> R.string.check_dnd
}

private fun checkWhy(check: HealthCheck): Int = when (check) {
    HealthCheck.NOTIFICATIONS -> R.string.check_notifications_why
    HealthCheck.EXACT_ALARMS -> R.string.check_exact_alarms_why
    HealthCheck.AUTO_START -> R.string.check_auto_start_why
    HealthCheck.BATTERY -> R.string.check_battery_why
    HealthCheck.FULL_SCREEN -> R.string.check_full_screen_why
    HealthCheck.DND_ACCESS -> R.string.check_dnd_why
}

@HiltViewModel
class HealthViewModel @Inject constructor(private val probe: HealthProbe, private val settings: SettingsStore, val clock: Clock) : ViewModel() {
    private val mutable = MutableStateFlow<HealthReport?>(null)
    val state: StateFlow<HealthReport?> = mutable

    /** Ayar sayfasından her dönüşte yeniden okunur; izinler uygulama dışında değişir. */
    fun refresh() {
        viewModelScope.launch { mutable.value = probe.report() }
    }

    fun confirmAutoStart() {
        viewModelScope.launch {
            settings.setAutoStartConfirmed(true)
            mutable.value = probe.report()
        }
    }
}

/** Eksik ayarlar ve "Düzelt" yolları; otomatik başlatma okunamadığı için kendi anlatımlı adımına gider. */
@Composable
private fun CheckRows(checks: List<HealthCheck>, status: PermissionStatus, onWizard: () -> Unit) {
    val context = LocalContext.current
    checks.forEach { check ->
        PermissionRow(
            name = stringResource(checkName(check)),
            purpose = stringResource(checkWhy(check)),
            status = status,
            onFix = { if (check == HealthCheck.AUTO_START) onWizard() else SettingsLinks.open(context, check) },
        )
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        style = ToparlaTheme.type.caption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
    )
}

/**
 * Hatırlatma Sağlığı (F2.35, blueprint D23; onaylı taslak `Saglik.dc.html`). Eksik ayar hata değil, yapılacak
 * küçük iştir: amber ve "Düzelt"; kırmızı yok.
 */
@Composable
fun HealthScreen(viewModel: HealthViewModel, onBack: () -> Unit, onWizard: () -> Unit, onSelfTest: () -> Unit) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val report by viewModel.state.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        FlowHeader(stringResource(R.string.health_title), ToparlaIcons.Back, stringResource(R.string.back), onBack)
        val current = report
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (current != null) {
                HealthSummary(current.toFix.size)
                if (current.toFix.isNotEmpty()) {
                    GroupLabel(stringResource(R.string.health_to_fix))
                    CheckRows(current.toFix, PermissionStatus.REQUIRED, onWizard)
                }
                if (current.ok.isNotEmpty()) {
                    GroupLabel(stringResource(R.string.health_ok))
                    CheckRows(current.ok, PermissionStatus.GRANTED, onWizard)
                }
                val last = current.lastDeliveredAt?.let { LAST_SEEN.format(it.atZone(viewModel.clock.zone())) } ?: stringResource(R.string.health_last_none)
                SettingRow(title = stringResource(R.string.health_last), trailing = { Text(text = last, style = ToparlaTheme.type.body) })
                SettingRow(
                    title = stringResource(R.string.health_pending),
                    trailing = { Text(text = current.pendingAlarms.toString(), style = ToparlaTheme.type.body) },
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = Spacing.screenEdge, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(R.string.health_test_hint), style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton(text = stringResource(R.string.health_test), onClick = onSelfTest)
        }
    }
}

@Composable
private fun HealthSummary(missing: Int) {
    val good = missing == 0
    ToparlaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Icon(
                imageVector = if (good) ToparlaIcons.Shield else ToparlaIcons.AlertCircle,
                contentDescription = null,
                tint = if (good) ToparlaTheme.extended.success else ToparlaTheme.extended.carried,
                modifier = Modifier.size(Sizes.fabIcon),
            )
            Text(
                text = if (good) stringResource(R.string.health_all_good) else pluralStringResource(R.plurals.health_missing, missing, missing),
                style = ToparlaTheme.type.title,
            )
        }
        Text(
            text = stringResource(if (good) R.string.health_all_good_text else R.string.health_missing_text),
            style = ToparlaTheme.type.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Kurulum sihirbazının otomatik başlatma adımı (F2.36, blueprint G3; onaylı taslak `Sihirbaz.dc.html`). */
@Composable
fun AutoStartWizardScreen(viewModel: HealthViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        FlowHeader(stringResource(R.string.check_auto_start), ToparlaIcons.Back, stringResource(R.string.back), onClose)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(text = stringResource(R.string.wizard_auto_start_title), style = ToparlaTheme.type.title)
            Text(text = stringResource(R.string.wizard_auto_start_text), style = ToparlaTheme.type.bodyL, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ToparlaCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.wizard_on_page), style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(R.string.wizard_step_find, R.string.wizard_step_switch, R.string.wizard_step_back).forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(text = (index + 1).toString(), style = ToparlaTheme.type.bodyL, color = MaterialTheme.colorScheme.primary)
                        Text(text = stringResource(step), style = ToparlaTheme.type.bodyL)
                    }
                }
            }
            TextAction(text = stringResource(R.string.wizard_open), onClick = { SettingsLinks.open(context, HealthCheck.AUTO_START) })
        }
        Column(
            modifier = Modifier.padding(horizontal = Spacing.screenEdge, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryButton(
                text = stringResource(R.string.wizard_done),
                onClick = {
                    viewModel.confirmAutoStart()
                    onClose()
                },
            )
            TextAction(text = stringResource(R.string.wizard_later), onClick = onClose)
        }
    }
}

data class SelfTestUi(val result: SelfTestResult? = null, val remainingSec: Long = 0, val toFix: List<HealthCheck> = emptyList())

/**
 * "Hatırlatmaları sına" (F2.37): 20 sn sonraya gerçek bir deneme hatırlatması kurar ve teslim kaydını yoklar.
 * Deneme anahtarı ayarlarda tutulur: Kullanıcı uygulamayı kapatıp açınca sonuç kaldığı yerden okunur.
 */
@HiltViewModel
class SelfTestViewModel @Inject constructor(
    private val creator: ReminderCreator,
    private val store: ReminderStore,
    private val settings: SettingsStore,
    private val probe: HealthProbe,
    private val clock: Clock,
) : ViewModel() {
    private val mutable = MutableStateFlow(SelfTestUi())
    val state: StateFlow<SelfTestUi> = mutable
    private var job: Job? = null

    /** Uygulama deneme sürerken kapatılıp açıldıysa true: açılışta sonuç ekranına dönülür. */
    suspend fun hasPending(): Boolean = settings.selfTestKey.first() != null

    /** Süren deneme varsa ona bağlanır, yoksa yenisini kurar. Sonuç ekrandayken yeniden çağrılırsa bir şey yapmaz. */
    fun begin(title: String, body: String) {
        if (job?.isActive == true || mutable.value.result != null) return
        job = viewModelScope.launch { watch(settings.selfTestKey.first() ?: start(title, body)) }
    }

    fun retry(title: String, body: String) {
        job?.cancel()
        job = viewModelScope.launch {
            resetTest()
            watch(start(title, body))
        }
    }

    /** Ekrandan çıkış: süren deneme iptal edilir, deneme hatırlatması silinir. */
    fun close() {
        job?.cancel()
        job = viewModelScope.launch { resetTest() }
    }

    private suspend fun start(title: String, body: String): String {
        val at = LocalDateTime.ofInstant(clock.now().plus(SelfTest.DELAY), clock.zone())
        val key = creator.create(ReminderDraft(title, at), ReminderClass.IMPORTANT, persistent = false, body = body, createdBy = CreatedBy.SYSTEM)
        settings.setSelfTestKey(key)
        return key
    }

    private suspend fun watch(key: String) {
        val plannedAt = key.substringAfter('@').toLongOrNull()?.let(Instant::ofEpochMilli)
        if (plannedAt == null) {
            resetTest()
            return
        }
        while (true) {
            val now = clock.now()
            val result = SelfTest.evaluate(plannedAt, store.occurrence(key)?.deliveredAt, now)
            val toFix = if (result == SelfTestResult.NotArrived) probe.report().toFix else emptyList()
            mutable.value = SelfTestUi(result, Duration.between(now, plannedAt).seconds.coerceAtLeast(0), toFix)
            if (result != SelfTestResult.Waiting) {
                // Deneme hatırlatması iz bırakmaz. Ulaşması otomatik başlatmayı kanıtlamaz (uygulama o an açık
                // olabilir; proje beyni H38): o ayarı yalnız Kullanıcı sihirbazda onaylar.
                removeTestReminder()
                return
            }
            delay(POLL_MS)
        }
    }

    private suspend fun resetTest() {
        removeTestReminder()
        mutable.value = SelfTestUi()
    }

    private suspend fun removeTestReminder() {
        val key = settings.selfTestKey.first() ?: return
        creator.delete(AlarmKey.parse(key).reminderId)
        settings.setSelfTestKey(null)
    }

    private companion object {
        const val POLL_MS = 1_000L
    }
}

/** Sınama ekranı (onaylı taslaklar `Sina.dc.html`, `SinaSonuc.dc.html`): geri sayım, sonra ulaştı / ulaşmadı. */
@Composable
fun SelfTestScreen(viewModel: SelfTestViewModel, onClose: () -> Unit, onFix: () -> Unit) {
    val title = stringResource(R.string.selftest_reminder_title)
    val body = stringResource(R.string.selftest_reminder_body)
    LaunchedEffect(Unit) { viewModel.begin(title, body) }
    val ui by viewModel.state.collectAsState()
    val leave = {
        viewModel.close()
        onClose()
    }
    Column(modifier = Modifier.fillMaxSize()) {
        FlowHeader(stringResource(R.string.selftest_title), ToparlaIcons.Close, stringResource(R.string.close), leave)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge),
            verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val result = ui.result) {
                is SelfTestResult.Arrived -> ResultBlock(
                    arrived = true,
                    title = stringResource(R.string.selftest_arrived),
                    text = stringResource(R.string.selftest_arrived_text, result.delay.seconds),
                )
                SelfTestResult.NotArrived -> {
                    ResultBlock(false, stringResource(R.string.selftest_not_arrived), stringResource(R.string.selftest_not_arrived_text))
                    GroupLabel(stringResource(R.string.selftest_likely))
                    CheckRows(ui.toFix, PermissionStatus.REQUIRED, onFix)
                    SettingRow(title = stringResource(R.string.selftest_lock_tip), description = stringResource(R.string.selftest_lock_tip_text))
                }
                else -> WaitingBlock(started = result != null, remainingSec = ui.remainingSec)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenEdge, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (ui.result) {
                is SelfTestResult.Arrived -> PrimaryButton(text = stringResource(R.string.selftest_ok), onClick = leave)
                SelfTestResult.NotArrived -> {
                    PrimaryButton(text = stringResource(R.string.selftest_retry), onClick = { viewModel.retry(title, body) })
                    TextAction(text = stringResource(R.string.selftest_cancel), onClick = leave)
                }
                else -> TextAction(text = stringResource(R.string.selftest_cancel), onClick = leave)
            }
        }
    }
}

@Composable
private fun ResultBlock(arrived: Boolean, title: String, text: String) {
    Icon(
        imageVector = if (arrived) ToparlaIcons.CheckCircle else ToparlaIcons.AlertCircle,
        contentDescription = null,
        tint = if (arrived) ToparlaTheme.extended.success else ToparlaTheme.extended.carried,
        modifier = Modifier.size(Sizes.emptyShape),
    )
    Text(text = title, style = ToparlaTheme.type.title, textAlign = TextAlign.Center)
    Text(text = text, style = ToparlaTheme.type.bodyL, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
private fun WaitingBlock(started: Boolean, remainingSec: Long) {
    val fraction = if (started) (remainingSec.toFloat() / SelfTest.DELAY.seconds).coerceIn(0f, 1f) else 1f
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    ProgressRing(remainingFraction = fraction, centerText = remainingSec.toString())
    Text(text = stringResource(R.string.selftest_seconds), style = ToparlaTheme.type.caption, color = muted)
    Text(text = stringResource(R.string.selftest_close_app), style = ToparlaTheme.type.title, textAlign = TextAlign.Center)
    Text(text = stringResource(R.string.selftest_close_app_text), style = ToparlaTheme.type.bodyL, color = muted, textAlign = TextAlign.Center)
    Text(text = stringResource(R.string.selftest_return), style = ToparlaTheme.type.caption, color = muted, textAlign = TextAlign.Center)
}
