package com.toparla.app.reminder

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toparla.app.R
import com.toparla.data.db.CreatedBy
import com.toparla.data.db.DeliveryInsights
import com.toparla.data.db.ReminderStore
import com.toparla.data.settings.SettingsStore
import com.toparla.domain.core.Clock
import com.toparla.domain.reminder.AlarmKey
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderDraft
import com.toparla.domain.reminder.SelfTest
import com.toparla.domain.reminder.SelfTestRecord
import com.toparla.domain.reminder.SelfTestResult
import com.toparla.domain.reminder.outcome
import com.toparla.reminders.HealthProbe
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.components.ProgressRing
import com.toparla.ui.components.QuietAction
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Shapes
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
import java.time.LocalDateTime
import javax.inject.Inject

private const val ANNOUNCE_STEP_SEC = 5L

/** [causes]: sınama başarısızsa sırayla bakılacak ayarlar ([SelfTest.likelyCauses]). */
data class SelfTestUi(val result: SelfTestResult? = null, val remainingSec: Long = 0, val causes: List<HealthCheck> = emptyList())

/**
 * "Hatırlatmaları sına" (F2.36): 20 sn sonraya gerçek bir deneme hatırlatması kurar ve teslim kaydını yoklar.
 * Deneme anahtarı ayarlarda tutulur: Kullanıcı uygulamayı kapatıp açınca sonuç kaldığı yerden okunur. Biten
 * sınamanın sonucu kaydedilir (blueprint G3); Hatırlatma Sağlığı "Son deneme" satırında gösterir.
 */
@HiltViewModel
class SelfTestViewModel @Inject constructor(
    private val creator: ReminderCreator,
    private val store: ReminderStore,
    private val insights: DeliveryInsights,
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
        val plannedAt = AlarmKey.plannedAtOf(key)
        if (plannedAt == null) {
            resetTest()
            return
        }
        while (true) {
            val now = clock.now()
            val result = SelfTest.evaluate(plannedAt, store.occurrence(key)?.deliveredAt, now, wokeApp = insights.wokeApp(key))
            val failed = result is SelfTestResult.Late || result == SelfTestResult.NotArrived
            val causes = if (failed) SelfTest.likelyCauses(probe.report()) else emptyList()
            mutable.value = SelfTestUi(result, Duration.between(now, plannedAt).seconds.coerceAtLeast(0), causes)
            val outcome = result.outcome()
            if (outcome != null) {
                // Sonuç kalıcıdır; deneme hatırlatmasının kendisi iz bırakmaz. Ulaşması otomatik başlatmayı
                // kanıtlamaz (alarm teslimi o izne bağlı değil; proje beyni H38): onu yalnız Kullanıcı onaylar.
                settings.setLastSelfTest(SelfTestRecord(now, outcome))
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

/**
 * Sınama ekranı. Başarısız sınamada okunamayan iki ayar (otomatik başlatma, uygulama kilidi) anlatımlı sihirbaz
 * adımına gider ([onAutoStart], [onLock]); okunabilen ayarlar doğrudan ayar sayfasını açar.
 */
@Composable
fun SelfTestScreen(viewModel: SelfTestViewModel, onClose: () -> Unit, onAutoStart: () -> Unit, onLock: () -> Unit) {
    val title = stringResource(R.string.selftest_reminder_title)
    val body = stringResource(R.string.selftest_reminder_body)
    LaunchedEffect(Unit) { viewModel.begin(title, body) }
    val ui by viewModel.state.collectAsState()
    val context = LocalContext.current
    SelfTestContent(
        ui = ui,
        onClose = {
            viewModel.close()
            onClose()
        },
        onRetry = { viewModel.retry(title, body) },
        onFix = { check -> if (check == HealthCheck.AUTO_START) onAutoStart() else SettingsLinks.open(context, check) },
        onLock = onLock,
    )
}

/**
 * Durumsuz içerik (onaylı taslaklar `Sina.dc.html`, `SinaSonuc.dc.html`, 10 Ekim `SinaAcikti.dc.html`,
 * `SinaGec.dc.html`): geri sayım, sonra dört sonuçtan biri. Başarısız sonuç amber kart ve sıralı nedenlerle
 * gösterilir; kırmızı yok, suçlayan ifade yok.
 */
@Composable
fun SelfTestContent(ui: SelfTestUi, onClose: () -> Unit, onRetry: () -> Unit, onFix: (HealthCheck) -> Unit, onLock: () -> Unit) {
    val result = ui.result
    val failed = result is SelfTestResult.Late || result == SelfTestResult.NotArrived
    Column(modifier = Modifier.fillMaxSize()) {
        FlowHeader(stringResource(R.string.selftest_title), ToparlaIcons.Close, stringResource(R.string.close), onClose)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.m),
            // Başarısız sonuç yukarıdan okunan bir listedir; ötekiler ekranın ortasında tek odaktır.
            verticalArrangement = Arrangement.spacedBy(Spacing.l, if (failed) Alignment.Top else Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (result) {
                is SelfTestResult.Arrived ->
                    if (result.appWasClosed) {
                        ResultBlock(ToparlaIcons.Check, stringResource(R.string.selftest_arrived), stringResource(R.string.selftest_arrived_closed_text, result.delay.seconds))
                    } else {
                        ResultBlock(ToparlaIcons.Check, stringResource(R.string.selftest_arrived), stringResource(R.string.selftest_arrived_open_text))
                        TipCard(ToparlaIcons.InfoCircle, stringResource(R.string.selftest_open_tip))
                    }
                is SelfTestResult.Late ->
                    FailedBlock(stringResource(R.string.selftest_late), stringResource(R.string.selftest_late_text, durationLabel(result.delay)), ui.causes, onFix, onLock)
                SelfTestResult.NotArrived ->
                    FailedBlock(stringResource(R.string.selftest_not_arrived), stringResource(R.string.selftest_not_arrived_text), ui.causes, onFix, onLock)
                SelfTestResult.Waiting, null -> WaitingBlock(started = result != null, remainingSec = ui.remainingSec)
            }
        }
        ScreenFooter {
            when (result) {
                is SelfTestResult.Arrived ->
                    if (result.appWasClosed) {
                        PrimaryButton(text = stringResource(R.string.selftest_ok), onClick = onClose)
                    } else {
                        PrimaryButton(text = stringResource(R.string.selftest_retry), onClick = onRetry)
                        QuietAction(text = stringResource(R.string.selftest_enough), onClick = onClose)
                    }
                is SelfTestResult.Late, SelfTestResult.NotArrived -> {
                    PrimaryButton(text = stringResource(R.string.selftest_retry), onClick = onRetry)
                    QuietAction(text = stringResource(R.string.selftest_cancel), onClick = onClose)
                }
                SelfTestResult.Waiting, null -> QuietAction(text = stringResource(R.string.selftest_cancel), onClick = onClose)
            }
        }
    }
}

@Composable
private fun ResultBlock(icon: ImageVector, title: String, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        HeroIcon(icon = icon, tint = ToparlaTheme.extended.success)
        Text(text = title, style = ToparlaTheme.type.title, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(text = text, style = ToparlaTheme.type.bodyL, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** İkincil kart zemininde tek cümlelik ipucu; ikon yalnız süs, anlamı metin taşır. */
@Composable
private fun TipCard(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.card)
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Sizes.rowIcon))
        Text(text = text, style = ToparlaTheme.type.bodyL)
    }
}

/**
 * Başarısız sınamanın rehberi (blueprint G3: "başarısızsa olası ayar listesi"): amber özet, sonra sıra numaralı
 * nedenler. Uygulama kilidi okunamadığı için her zaman son sırada durur.
 */
@Composable
private fun FailedBlock(title: String, text: String, causes: List<HealthCheck>, onFix: (HealthCheck) -> Unit, onLock: () -> Unit) {
    AttentionCard(title = title, text = text)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        GroupLabel(stringResource(R.string.selftest_likely))
        causes.forEachIndexed { index, check ->
            FixRow(
                name = stringResource(checkName(check)),
                why = causeWhy(check),
                actionLabel = stringResource(R.string.health_fix),
                onAction = { onFix(check) },
                leading = { StepBadge(index + 1) },
            )
        }
        FixRow(
            name = stringResource(R.string.setup_lock_done),
            why = stringResource(R.string.cause_lock),
            actionLabel = stringResource(R.string.health_show),
            onAction = onLock,
            leading = { StepBadge(causes.size + 1) },
        )
    }
}

/** Başarısız sınamada nedenin anlatımı: kesin değil, olasılık diliyle. */
@Composable
private fun causeWhy(check: HealthCheck): String = when (check) {
    HealthCheck.AUTO_START -> stringResource(R.string.cause_auto_start)
    HealthCheck.BATTERY -> stringResource(R.string.cause_battery)
    else -> checkWhy(check, emptySet())
}

@Composable
private fun WaitingBlock(started: Boolean, remainingSec: Long) {
    val fraction = if (started) (remainingSec.toFloat() / SelfTest.DELAY.seconds).coerceIn(0f, 1f) else 1f
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        ProgressRing(
            remainingFraction = fraction,
            centerText = remainingSec.toString(),
            size = Sizes.testRing,
            strokeWidth = Sizes.testRingStroke,
            // TalkBack her saniye konuşmasın: kalan süre beşer saniyelik adımlarla duyurulur.
            announcement = stringResource(R.string.selftest_remaining, (remainingSec + ANNOUNCE_STEP_SEC - 1) / ANNOUNCE_STEP_SEC * ANNOUNCE_STEP_SEC),
            textStyle = ToparlaTheme.type.displayNow,
        )
        Text(text = stringResource(R.string.selftest_seconds), style = ToparlaTheme.type.caption, color = muted)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(text = stringResource(R.string.selftest_close_app), style = ToparlaTheme.type.title, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(text = stringResource(R.string.selftest_close_app_text), style = ToparlaTheme.type.bodyL, color = muted, textAlign = TextAlign.Center)
    }
    TipCard(ToparlaIcons.Bell, stringResource(R.string.selftest_return))
}
