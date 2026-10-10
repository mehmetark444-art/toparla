package com.toparla.app.reminder

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.toparla.domain.reminder.DeliveryTiming
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.HealthReport
import com.toparla.domain.reminder.HealthSnapshot
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.SelfTestOutcome
import com.toparla.domain.reminder.SelfTestRecord
import com.toparla.domain.reminder.SelfTestResult
import com.toparla.domain.reminder.SetupStep
import com.toparla.domain.reminder.SetupWizard
import com.toparla.domain.reminder.StandbyBucket
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ThemeMode
import com.toparla.ui.theme.ToparlaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val ZONE: ZoneId = ZoneId.of("Europe/Istanbul")

private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(ZONE).toInstant()

private val NOW: Instant = at("2026-10-10T22:00")

/** Örnek durumlar: uydurma içerik (gerçek kişisel veri yok). */
private object Samples {
    val healthy = HealthSnapshot(
        notifications = true, exactAlarms = true, fullScreen = true, batteryExempt = true,
        dndAccess = true, autoStartConfirmed = true, weakenedChannels = emptySet(), standbyBucket = StandbyBucket.ACTIVE,
        upcomingReminders = 3, lastDelivery = DeliveryTiming(at("2026-10-10T21:47"), at("2026-10-10T21:47")),
        recentDeliveries = emptyList(), lastSelfTest = SelfTestRecord(at("2026-10-10T08:51"), SelfTestOutcome.ARRIVED_CLOSED),
    )

    fun report(snapshot: HealthSnapshot = healthy): HealthReport = HealthReport.of(snapshot, NOW)

    /** Onaylı taslaktaki durum: önemli kanalın sesi kısılmış, son 7 günde iki teslim vaktinde ulaşmamış. */
    val oneMissing = report(
        healthy.copy(
            weakenedChannels = setOf(ReminderClass.IMPORTANT),
            recentDeliveries = listOf(
                DeliveryTiming(at("2026-10-09T09:00"), at("2026-10-09T09:04")),
                DeliveryTiming(at("2026-10-08T09:00"), at("2026-10-08T09:03")),
            ),
        ),
    )

    /** İlk kurulum: otomatik başlatma, pil ve kilit bekliyor; deneme hiç yapılmamış. */
    val fresh = report(healthy.copy(autoStartConfirmed = false, batteryExempt = false, lastSelfTest = null, lastDelivery = null, upcomingReminders = 0))

    fun setup(step: SetupStep?, report: HealthReport = fresh, lock: Boolean = false, single: Boolean = false, pageOpened: Boolean = false, finished: Boolean = false) =
        SetupUi(SetupWizard.progress(report, lock), report, step, single, pageOpened, finished, firstTime = step == null && !finished)
}

@Composable
private fun Health(report: HealthReport, onFix: (HealthCheck) -> Unit = {}) =
    HealthContent(report, ZONE, NOW.atZone(ZONE).toLocalDate(), onBack = {}, onFix = onFix, onSelfTest = {})

@Composable
private fun Setup(ui: SetupUi, onOpenPage: (HealthCheck) -> Unit = {}, onConfirm: () -> Unit = {}, onSkip: () -> Unit = {}) =
    SetupContent(ui, onClose = {}, onStart = {}, onOpenPage = onOpenPage, onConfirm = onConfirm, onSkip = onSkip, onSelfTest = {}, onFinish = {})

@Composable
private fun SelfTestOf(result: SelfTestResult?, causes: List<HealthCheck> = emptyList(), onFix: (HealthCheck) -> Unit = {}, onLock: () -> Unit = {}) =
    SelfTestContent(SelfTestUi(result, remainingSec = 14, causes = causes), onClose = {}, onRetry = {}, onFix = onFix, onLock = onLock)

/**
 * F2.35–F2.37 ekranlarının ekran görüntüleri (3 tema; kalabalık ekranlar iki kat yazıda da). Onaylı taslaklarla
 * (`docs/tasarim/2026-10-09-hatirlatma-ekranlari/`, `docs/tasarim/2026-10-10-kurulum-sihirbazi/`) karşılaştırılır.
 * Kayıt: `:app:recordRoborazziDebug`, karşılaştırma: `:app:verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xhdpi", application = Application::class)
class ReminderScreenshotTest {

    @Test
    @Config(qualifiers = "w412dp-h1300dp-xhdpi")
    fun saglik_eksikli_koyu() = capture("saglik_eksikli", ThemeMode.DARK) { Health(Samples.oneMissing) }

    @Test
    @Config(qualifiers = "w412dp-h1300dp-xhdpi")
    fun saglik_eksikli_acik() = capture("saglik_eksikli", ThemeMode.LIGHT) { Health(Samples.oneMissing) }

    @Test
    @Config(qualifiers = "w412dp-h2200dp-xhdpi")
    fun saglik_eksikli_amoled_iki_kat() = capture("saglik_eksikli", ThemeMode.AMOLED, DOUBLE) { Health(Samples.oneMissing) }

    @Test
    @Config(qualifiers = "w412dp-h1300dp-xhdpi")
    fun saglik_yerinde_koyu() = capture("saglik_yerinde", ThemeMode.DARK) { Health(Samples.report()) }

    @Test fun kurulum_giris_koyu() = capture("kurulum_giris", ThemeMode.DARK) { Setup(Samples.setup(step = null)) }

    @Test fun kurulum_giris_acik() = capture("kurulum_giris", ThemeMode.LIGHT) { Setup(Samples.setup(step = null)) }

    @Test fun kurulum_otomatik_baslatma_koyu() = capture("kurulum_otomatik_baslatma", ThemeMode.DARK) { Setup(Samples.setup(SetupStep.AUTO_START)) }

    @Test
    fun kurulum_otomatik_baslatma_donus_amoled() =
        capture("kurulum_otomatik_baslatma_donus", ThemeMode.AMOLED) { Setup(Samples.setup(SetupStep.AUTO_START, pageOpened = true)) }

    @Test fun kurulum_kilit_koyu() = capture("kurulum_kilit", ThemeMode.DARK) { Setup(Samples.setup(SetupStep.RECENTS_LOCK)) }

    @Test
    @Config(qualifiers = "w412dp-h1700dp-xhdpi")
    fun kurulum_kilit_acik_iki_kat() = capture("kurulum_kilit", ThemeMode.LIGHT, DOUBLE) { Setup(Samples.setup(SetupStep.RECENTS_LOCK)) }

    @Test fun kurulum_deneme_koyu() = capture("kurulum_deneme", ThemeMode.DARK) { Setup(Samples.setup(SetupStep.SELF_TEST)) }

    @Test fun kurulum_bitis_koyu() = capture("kurulum_bitis", ThemeMode.DARK) { Setup(Samples.setup(step = null, report = Samples.report(), lock = true, finished = true)) }

    @Test fun kurulum_bitis_acik() = capture("kurulum_bitis", ThemeMode.LIGHT) { Setup(Samples.setup(step = null, report = Samples.report(), lock = true, finished = true)) }

    @Test fun sina_bekleme_koyu() = capture("sina_bekleme", ThemeMode.DARK) { SelfTestOf(SelfTestResult.Waiting) }

    @Test fun sina_ulasti_koyu() = capture("sina_ulasti", ThemeMode.DARK) { SelfTestOf(SelfTestResult.Arrived(Duration.ZERO, appWasClosed = true)) }

    @Test fun sina_acikti_koyu() = capture("sina_acikti", ThemeMode.DARK) { SelfTestOf(SelfTestResult.Arrived(Duration.ZERO, appWasClosed = false)) }

    @Test
    fun sina_gec_koyu() = capture("sina_gec", ThemeMode.DARK) {
        SelfTestOf(SelfTestResult.Late(Duration.ofSeconds(95)), causes = listOf(HealthCheck.AUTO_START, HealthCheck.BATTERY))
    }

    @Test
    fun sina_ulasmadi_acik() = capture("sina_ulasmadi", ThemeMode.LIGHT) {
        SelfTestOf(SelfTestResult.NotArrived, causes = listOf(HealthCheck.AUTO_START, HealthCheck.BATTERY))
    }

    @Test
    @Config(qualifiers = "w412dp-h1700dp-xhdpi")
    fun sina_gec_amoled_iki_kat() = capture("sina_gec", ThemeMode.AMOLED, DOUBLE) {
        SelfTestOf(SelfTestResult.Late(Duration.ofSeconds(95)), causes = listOf(HealthCheck.AUTO_START, HealthCheck.BATTERY))
    }

    @Test
    @Config(qualifiers = "w412dp-h320dp-xhdpi")
    fun simdi_karti_koyu() = capture("simdi_karti", ThemeMode.DARK) { Box(Modifier.padding(Spacing.screenEdge)) { SetupCard(remaining = 2, onContinue = {}) } }

    @Test
    @Config(qualifiers = "w412dp-h320dp-xhdpi")
    fun simdi_karti_acik() = capture("simdi_karti", ThemeMode.LIGHT) { Box(Modifier.padding(Spacing.screenEdge)) { SetupCard(remaining = 2, onContinue = {}) } }

    private fun capture(name: String, mode: ThemeMode, fontScale: Float = NORMAL, content: @Composable () -> Unit) {
        val scaleName = if (fontScale == NORMAL) "yazi100" else "yazi200"
        captureRoboImage(filePath = "src/test/screenshots/${name}_${mode.name.lowercase()}_$scaleName.png") {
            val system = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = system.density, fontScale = fontScale)) {
                ToparlaTheme(mode = mode, hapticsEnabled = false) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
                }
            }
        }
    }

    private companion object {
        const val NORMAL = 1f
        const val DOUBLE = 2f
    }
}

/** Ekranların davranışı: doğru satır doğru eylemi çağırıyor mu, durumlar doğru metni ve tek baskın eylemi gösteriyor mu. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h1300dp-xhdpi", application = Application::class)
class ReminderScreensBehaviorTest {
    /** Test uygulamasının manifestinde Activity yok; Compose kuralından önce kaydedilir. */
    @get:Rule(order = 0)
    val registerActivity = object : TestWatcher() {
        override fun starting(description: Description) {
            val app = RuntimeEnvironment.getApplication()
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app.packageName, ComponentActivity::class.java.name))
        }
    }

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) = compose.setContent { ToparlaTheme(hapticsEnabled = false) { content() } }

    @Test
    fun `saglik ekraninda eksik ayarin duzelt dugmesi o ayari bildirir ve kisilan sinif adiyla yazilir`() {
        val fixed = ArrayList<HealthCheck>()
        show { Health(Samples.report(Samples.healthy.copy(batteryExempt = false, weakenedChannels = setOf(ReminderClass.CRITICAL, ReminderClass.IMPORTANT))), fixed::add) }

        compose.onNodeWithText("2 ayar eksik").assertIsDisplayed()
        compose.onNodeWithText("Kritik ve Önemli hatırlatmaların sesi kısılmış.").assertIsDisplayed()
        compose.onAllNodesWithText("Düzelt").assertCountEquals(2)
        compose.onAllNodesWithText("Düzelt")[1].performClick()

        assertEquals(listOf(HealthCheck.BATTERY), fixed)
    }

    @Test
    fun `saglik ekrani son durumu yazar - gec teslim dakikasiyla vaktinde ulasmayan sayisiyla`() {
        val late = Samples.healthy.copy(
            lastDelivery = DeliveryTiming(at("2026-10-10T21:47"), at("2026-10-10T21:51")),
            recentDeliveries = listOf(DeliveryTiming(at("2026-10-10T21:47"), at("2026-10-10T21:51"))),
            lastSelfTest = null,
        )
        show { Health(Samples.report(late)) }

        compose.onNodeWithText("Her şey yerinde").assertIsDisplayed()
        compose.onNodeWithText("Bugün 21:47 · 4 dk geç").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Son 7 gün. Ayarlar tamamsa bir deneme yap.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Henüz yapılmadı").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `otomatik baslatma adiminda once ayar sayfasi acilir donuste acik onayi istenir`() {
        val opened = ArrayList<HealthCheck>()
        var confirmed = 0
        // Sihirbazın gerçek akışı: sayfa açılınca durum "açıldı"ya döner.
        val pageOpened = mutableStateOf(false)
        show {
            Setup(
                Samples.setup(SetupStep.AUTO_START, pageOpened = pageOpened.value),
                onOpenPage = {
                    opened += it
                    pageOpened.value = true
                },
                onConfirm = { confirmed++ },
            )
        }

        compose.onNodeWithText("Açtım").assertDoesNotExist()
        compose.onNodeWithText("Ayar sayfasını aç").performClick()
        assertEquals(listOf(HealthCheck.AUTO_START), opened)

        compose.onNodeWithText("Sayfayı yeniden aç").assertIsDisplayed()
        compose.onNodeWithText("Açtım").performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun `kilit adimi ayar sayfasi acmaz kullanici onayiyla biter ve atlanabilir`() {
        var confirmed = 0
        var skipped = 0
        show { Setup(Samples.setup(SetupStep.RECENTS_LOCK), onConfirm = { confirmed++ }, onSkip = { skipped++ }) }

        compose.onNodeWithText("Ayar sayfasını aç").assertDoesNotExist()
        compose.onNodeWithText("Kilitledim").performClick()
        compose.onNodeWithText("Şimdi değil").performClick()

        assertEquals(1 to 1, confirmed to skipped)
    }

    @Test
    fun `gec ulasan sinama basari gibi gosterilmez - nedenler siralanir kilit her zaman sonda`() {
        val fixed = ArrayList<HealthCheck>()
        var lockShown = 0
        show { SelfTestOf(SelfTestResult.Late(Duration.ofSeconds(95)), listOf(HealthCheck.AUTO_START, HealthCheck.BATTERY), fixed::add) { lockShown++ } }

        compose.onNodeWithText("Deneme hatırlatması geç ulaştı").assertIsDisplayed()
        compose.onNodeWithText("Deneme hatırlatması ulaştı").assertDoesNotExist()
        compose.onNodeWithText("Planlanandan 1 dk 35 sn sonra, ancak uygulama yeniden açılınca geldi. Sırayla bakalım; her birinden sonra yeniden deneriz.").assertIsDisplayed()
        compose.onAllNodesWithText("Düzelt")[0].performClick()
        compose.onNodeWithText("Göster").performClick()

        assertEquals(listOf(HealthCheck.AUTO_START), fixed)
        assertEquals(1, lockShown)
    }

    @Test
    fun `uygulama acikken ulasan deneme kapali uygulamayi sinamadigini soyler ve yeniden denemeyi onerir`() {
        show { SelfTestOf(SelfTestResult.Arrived(Duration.ZERO, appWasClosed = false)) }

        compose.onNodeWithText("Yeniden dene").assertIsDisplayed()
        compose.onNodeWithText("Bu kadarı yeter").assertIsDisplayed()
        compose.onNodeWithText("Tamam").assertDoesNotExist()
    }
}
