package com.toparla.app.medication

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.toparla.app.reminder.CardContent
import com.toparla.app.reminder.CriticalCard
import com.toparla.domain.medication.DoseRow
import com.toparla.domain.medication.DoseState
import com.toparla.domain.medication.Medication
import com.toparla.domain.medication.MedicationForm
import com.toparla.domain.medication.MedicationInput
import com.toparla.domain.medication.TodayDose
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ThemeMode
import com.toparla.ui.theme.ToparlaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private val ZONE: ZoneId = ZoneId.of("Europe/Istanbul")

private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(ZONE).toInstant()

private val TODAY: LocalDate = LocalDate.of(2026, 10, 11)

/** Örnek durumlar: uydurma içerik (gerçek ilaç adı ya da kişisel veri yok). */
private object MedSamples {
    val morning = Medication(
        id = "m1", name = "Sabah ilacı", doseText = "1 tablet", times = listOf(LocalTime.of(8, 0)), startDate = TODAY, zoneId = ZONE, stock = 12, lowStockAt = 5,
    )
    val vitamin = Medication(
        id = "m2", name = "Vitamin", doseText = "1 kapsül", times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), startDate = TODAY, zoneId = ZONE,
    )

    private fun row(med: Medication, local: String, state: DoseState, logged: String? = null, actionable: Boolean = true) =
        DoseRow(med.id, at(local), if (state == DoseState.UPCOMING) null else "${med.id}@$local", state, logged?.let(::at), answerable = actionable)

    val taken = row(morning, "2026-10-11T08:00", DoseState.TAKEN, logged = "2026-10-11T08:03")
    val notLogged = row(vitamin, "2026-10-11T08:00", DoseState.NOT_LOGGED)
    val pending = row(vitamin, "2026-10-11T20:00", DoseState.PENDING)
    val upcoming = row(vitamin, "2026-10-11T20:00", DoseState.UPCOMING, actionable = false)

    val list = MedicationsUi(
        loaded = true,
        enabled = true,
        medications = listOf(morning, vitamin),
        today = listOf(
            TodayDose(LocalTime.of(8, 0), morning, taken),
            TodayDose(LocalTime.of(8, 0), vitamin, notLogged),
            TodayDose(LocalTime.of(20, 0), vitamin, upcoming),
        ),
        history = listOf(
            HistoryRow(notLogged, "Vitamin"),
            HistoryRow(taken, "Sabah ilacı"),
            HistoryRow(row(vitamin, "2026-10-10T20:00", DoseState.LATE, logged = "2026-10-10T21:40"), "Vitamin"),
            HistoryRow(row(vitamin, "2026-10-10T08:00", DoseState.SKIPPED, logged = "2026-10-10T08:20"), "Vitamin"),
        ),
    )

    val form = MedicationFormUi(
        input = MedicationInput(
            name = "Sabah ilacı", doseText = "1 tablet", times = listOf(LocalTime.of(8, 0)), days = DayOfWeek.entries.toSet(), minGapHours = 4,
            stock = "30", perDose = "1", lowStock = "5", notes = "Yemekten sonra", hideName = false,
        ),
        loaded = true,
    )
}

@Composable
private fun ListOf(ui: MedicationsUi, actions: DoseActions = DoseActions.None, onEdit: (String) -> Unit = {}) =
    MedicationListContent(ui, ZONE, actions, MedicationNav(onBack = {}, onHistory = {}, onAdd = {}, onEdit = onEdit))

/**
 * İlaç ekranlarının ekran görüntüleri (onaylı taslak `docs/tasarim/2026-10-11-ilac/`). Görüntüler mdpi çekilir: bulut
 * oturumu temel görüntüleri yalnız CI günlüğünden alabiliyor (proje beyni Bölüm 9).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-mdpi", application = Application::class)
class MedicationScreenshotTest {
    @Test fun ilaclar_koyu() = capture("ilaclar", ThemeMode.DARK) { ListOf(MedSamples.list) }

    @Test fun ilaclar_acik() = capture("ilaclar", ThemeMode.LIGHT) { ListOf(MedSamples.list) }

    @Test
    @Config(qualifiers = "w412dp-h1500dp-mdpi")
    fun ilac_ekle_koyu() = capture("ilac_ekle", ThemeMode.DARK) { MedicationFormContent(MedSamples.form, MedicationFormActions.None) }

    @Test
    @Config(qualifiers = "w412dp-h1500dp-mdpi")
    fun ilac_duzenle_eksikli_acik() = capture("ilac_duzenle_eksikli", ThemeMode.LIGHT) {
        val invalid = MedSamples.form.copy(
            id = "m1",
            input = MedSamples.form.input.copy(name = "", days = emptySet()),
            problems = setOf(MedicationForm.Problem.NAME, MedicationForm.Problem.DAYS),
        )
        MedicationFormContent(invalid, MedicationFormActions.None)
    }

    @Test fun ilac_gecmis_koyu() = capture("ilac_gecmis", ThemeMode.DARK) { HistoryOf(MedSamples.list) }

    @Test fun ilac_disa_aktar_koyu() = capture("ilac_disa_aktar", ThemeMode.DARK) { ExportSheetContent(ExportRange.DAYS_30, ExportFormat.CSV, {}, {}, {}) }

    @Test fun ayarlar_moduller_koyu() = capture("ayarlar_moduller", ThemeMode.DARK) { SettingsContent(SettingsUi(medication = true), lockAvailable = true, NoSettings) }

    @Test fun ilac_kilitli_amoled() = capture("ilac_kilitli", ThemeMode.AMOLED) { MedicationLockedContent(onUnlock = {}, onBack = {}) }

    @Test
    @Config(qualifiers = "w412dp-h200dp-mdpi")
    fun plan_ilac_karti_acik() = capture("plan_ilac_karti", ThemeMode.LIGHT) {
        Box(Modifier.padding(Spacing.screenEdge)) { MedicationPlanCard(MedicationSummary(next = LocalTime.of(20, 0), notLogged = 1), onOpen = {}) }
    }

    @Test fun ilac_tam_ekran_koyu() = capture("ilac_tam_ekran", ThemeMode.DARK) {
        CriticalCard(CardContent("08:00", "Sabah ilacı", "1 tablet", medicationId = "m1"), onDone = {}, onSnooze = {}, onSkip = {})
    }

    private fun capture(name: String, mode: ThemeMode, content: @Composable () -> Unit) {
        captureRoboImage(filePath = "src/test/screenshots/${name}_${mode.name.lowercase()}_mdpi.png") {
            ToparlaTheme(mode = mode, hapticsEnabled = false) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
            }
        }
    }
}

@Composable
private fun HistoryOf(ui: MedicationsUi, actions: DoseActions = DoseActions.None, onExport: () -> Unit = {}) =
    MedicationHistoryContent(ui, ZONE, TODAY, actions, onBack = {}, onExport = onExport)

private val NoSettings = SettingsActions(onBack = {}, onMedication = {}, onMedicationLock = {}, onMedications = {}, onHealth = {})

/** Compose kuralından önce boş Activity kaydı (test uygulamasının manifestinde Activity yok). */
private class RegisterActivity : TestWatcher() {
    override fun starting(description: Description) {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app.packageName, ComponentActivity::class.java.name))
    }
}

/** İlaç ekranlarının davranışı (F2.39–F2.40): doğru satır doğru eylemi çağırır, durum metni ve eylemler doğru. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h1300dp-xhdpi", application = Application::class)
class MedicationScreensBehaviorTest {
    @get:Rule(order = 0)
    val registerActivity = RegisterActivity()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) = compose.setContent { ToparlaTheme(hapticsEnabled = false) { content() } }

    @Test
    fun `kaydedilmeyen doz amber metinle gec aldim ve atladim sunar dogru dozu bildirir`() {
        val late = ArrayList<DoseRow>()
        val skipped = ArrayList<DoseRow>()
        show { ListOf(MedSamples.list, DoseActions(onTake = {}, onLate = late::add, onSkip = skipped::add)) }

        compose.onNodeWithText("Kaydedilmedi").assertIsDisplayed()
        compose.onNodeWithText("Alındı 08:03").assertIsDisplayed()
        compose.onNodeWithText("Sırada").assertIsDisplayed()
        compose.onNodeWithText("Geç aldım").performClick()
        compose.onNodeWithText("Atladım").performClick()

        assertEquals(listOf(MedSamples.notLogged), late)
        assertEquals(listOf(MedSamples.notLogged), skipped)
    }

    @Test
    fun `yaniti bekleyen doz aldim sunar sirada olan doz hicbir eylem sunmaz`() {
        val taken = ArrayList<DoseRow>()
        val ui = MedSamples.list.copy(today = listOf(TodayDose(LocalTime.of(20, 0), MedSamples.vitamin, MedSamples.pending)))
        show { ListOf(ui, DoseActions(onTake = taken::add, onLate = {}, onSkip = {})) }

        compose.onNodeWithText("Bekliyor").assertIsDisplayed()
        compose.onNodeWithText("Aldım").performClick()
        compose.onNodeWithText("Geç aldım").assertDoesNotExist()
        assertEquals(listOf(MedSamples.pending), taken)
    }

    @Test
    fun `ilac satiri duzenlemeyi acar ve cizelgeyi gun saat stokla ozetler`() {
        val opened = ArrayList<String>()
        show { ListOf(MedSamples.list, onEdit = opened::add) }

        compose.onNodeWithText("Her gün · 08:00 · 12 kaldı").performScrollTo().performClick()
        compose.onNodeWithText("Her gün · 08:00, 20:00").performScrollTo().assertIsDisplayed()
        assertEquals(listOf("m1"), opened)
    }

    @Test
    fun `ilac yokken bos durum ve ilac ekle gorunur`() {
        show { ListOf(MedicationsUi(loaded = true, enabled = true)) }

        compose.onNodeWithText("Henüz ilaç eklemedin. Eklediğinde saatinde hatırlatırım.").assertIsDisplayed()
        compose.onNodeWithText("İlaç ekle").assertIsDisplayed()
    }

    @Test
    fun `gecmis telafi onermeden doktor ya da eczaci yonlendirmesini ve gun basliklarini gosterir`() {
        var exported = 0
        show { HistoryOf(MedSamples.list, onExport = { exported++ }) }

        compose.onNodeWithText("Kaydı olmayan doz için telafi önermem. Talimat için doktorunu ya da eczacını izle.").assertIsDisplayed()
        compose.onNodeWithText("Dün").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Geç alındı 21:40").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Dışa aktar").performClick()
        assertEquals(1, exported)
    }

    @Test
    fun `formda eksik alanlar adiyla soylenir ve sil yalniz duzenlerken gorunur`() {
        val ui = mutableStateOf(MedSamples.form.copy(problems = setOf(MedicationForm.Problem.NAME, MedicationForm.Problem.TIMES)))
        show { MedicationFormContent(ui.value, MedicationFormActions.None) }

        compose.onNodeWithText("Bir ad yaz.").assertIsDisplayed()
        compose.onNodeWithText("En az bir saat ekle.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("İlacı sil").assertDoesNotExist()

        ui.value = MedSamples.form.copy(id = "m1")
        compose.onNodeWithText("İlacı sil").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `formda cift doz araligi ve gunler secilir`() {
        val ui = mutableStateOf(MedSamples.form)
        var everyDay: Boolean? = null
        val actions = MedicationFormActions.None.copy(
            onEdit = { change -> ui.value = ui.value.copy(input = change(ui.value.input)) },
            onEveryDay = { everyDay = it },
        )
        show { MedicationFormContent(ui.value, actions) }

        compose.onNodeWithText("8 sa").performScrollTo().performClick()
        compose.onNodeWithText("Seçili günler").performScrollTo().performClick()

        assertEquals(8, ui.value.input.minGapHours)
        assertEquals(false, everyDay)
    }

    @Test
    fun `tam ekran ilac karti aldim 15 dk sonra ve atliyorum sunar`() {
        var skipped = 0
        show { CriticalCard(CardContent("08:00", "Sabah ilacı", "1 tablet", medicationId = "m1"), onDone = {}, onSnooze = {}, onSkip = { skipped++ }) }

        compose.onNodeWithText("Aldım").assertIsDisplayed()
        compose.onNodeWithText("15 dk sonra").assertIsDisplayed()
        compose.onNodeWithText("Atlıyorum").performClick()
        compose.onNodeWithText("Yaptım").assertDoesNotExist()
        assertEquals(1, skipped)
    }

    @Test
    fun `ilac olmayan tam ekran kart eskisi gibi yaptim ve 10 dk sonra sunar`() {
        show { CriticalCard(CardContent("15:00", "Dişçi", ""), onDone = {}, onSnooze = {}) }

        compose.onNodeWithText("Yaptım").assertIsDisplayed()
        compose.onNodeWithText("10 dk sonra").assertIsDisplayed()
        compose.onNodeWithText("Atlıyorum").assertDoesNotExist()
    }

    @Test
    fun `plan karti siradaki dozu ve kaydedilmeyen sayisini yazar`() {
        show { MedicationPlanCard(MedicationSummary(next = LocalTime.of(20, 0), notLogged = 1), onOpen = {}) }
        compose.onNodeWithText("Sıradaki 20:00 · 1 doz kaydedilmedi").assertIsDisplayed()
    }
}

/**
 * F2.41 (blueprint M9 kabulü, K18): modül kapalıyken hiçbir ilaç yüzeyi görünmez ve çalışma zamanında hiçbir izin
 * istenmez. Motor tarafı (kapalıyken ilaç hatırlatması kurulmaz, gösterilmez) cihaz testindedir
 * (`MedicationDatabaseTest.modulKapaliykenIlacHatirlatmasiMotoraGorunmezAcilincaGorunur`).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h1300dp-xhdpi", application = Application::class)
class MedicationFlagOffTest {
    @get:Rule(order = 0)
    val registerActivity = RegisterActivity()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) = compose.setContent { ToparlaTheme(hapticsEnabled = false) { content() } }

    @Test
    fun `modul kapaliyken ayarlarda yalniz anahtar gorunur ilaclar ve kilit satiri yok`() {
        var turnedOn: Boolean? = null
        show { SettingsContent(SettingsUi(medication = false), lockAvailable = true, NoSettings.copy(onMedication = { turnedOn = it })) }

        compose.onNodeWithText("İlaç takibi").assertIsDisplayed()
        compose.onNodeWithText("İlaçlar").assertDoesNotExist()
        compose.onNodeWithText("İlaç ekranlarını kilitle").assertDoesNotExist()
        compose.onNodeWithText("İlaç takibi").performClick()
        assertEquals(true, turnedOn)
    }

    @Test
    fun `modul acikken ilaclar ve kilit satiri gorunur ekran kilidi yoksa kilit sunulmaz`() {
        show { SettingsContent(SettingsUi(medication = true), lockAvailable = false, NoSettings) }

        compose.onNodeWithText("İlaçlar").assertIsDisplayed()
        compose.onNodeWithText("Telefonda ekran kilidi olmadığı için şu an kullanılamıyor.").assertIsDisplayed()
    }

    @Test
    fun `modul kapaliyken ilac ekrani cizilmez ve kapanir`() {
        var closed = 0
        show { ModuleGate(loaded = true, enabled = false, onClosed = { closed++ }) { Text("ilaç ekranı") } }

        compose.onNodeWithText("ilaç ekranı").assertDoesNotExist()
        compose.waitForIdle()
        assertEquals(1, closed)
    }

    @Test
    fun `modul acikken ilac ekrani cizilir ayar okunmadan kapanmaz`() {
        var closed = 0
        val state = mutableStateOf(false to false)
        show { ModuleGate(loaded = state.value.first, enabled = state.value.second, onClosed = { closed++ }) { Text("ilaç ekranı") } }
        compose.waitForIdle()
        assertEquals(0, closed)

        state.value = true to true
        compose.onNodeWithText("ilaç ekranı").assertIsDisplayed()
        assertEquals(0, closed)
    }

    @Test
    fun `ilac modulu calisma zamani izni eklemez - tehlikeli izin yalniz bildirim izni`() {
        val app = RuntimeEnvironment.getApplication()
        val requested = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()

        assertTrue("android.permission.USE_BIOMETRIC" in requested)
        assertEquals(setOf("android.permission.POST_NOTIFICATIONS"), requested intersect RUNTIME_PERMISSIONS)
        assertFalse(requested.any { it.contains("STORAGE") || it.contains("MEDIA") })
    }

    private companion object {
        /** Çalışma zamanında pencereyle sorulan izinler (Android "dangerous" koruma düzeyi); ilaçla ilgili olabilecekler. */
        val RUNTIME_PERMISSIONS = setOf(
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "android.permission.READ_CONTACTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.BODY_SENSORS",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.SEND_SMS",
        )
    }
}
