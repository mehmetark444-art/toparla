package com.toparla.domain.medication

import com.toparla.domain.reminder.MedicationRole
import com.toparla.domain.reminder.Recurrence
import com.toparla.domain.reminder.ReminderClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** F2.39–F2.40 kuralları (blueprint M9): tanım, çizelge, çift doz koruması, kayıt durumu, stok, geçmiş, CSV. */
class MedicationRulesTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun med(
        times: List<LocalTime> = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        stock: Int? = null,
        lowStockAt: Int? = null,
        unitsPerDose: Int = 1,
        minGap: Duration = Duration.ofHours(4),
        active: Boolean = true,
    ) = Medication(
        id = "m1",
        name = "Örnek",
        doseText = "1 tablet",
        times = times,
        days = days,
        startDate = LocalDate.of(2026, 10, 11),
        zoneId = zone,
        minGap = minGap,
        stock = stock,
        unitsPerDose = unitsPerDose,
        lowStockAt = lowStockAt,
        active = active,
    )

    // --- Tanım ---

    @Test
    fun `bos ad saatsiz tanim ve gecersiz sayilar reddedilir`() {
        assertThrows<IllegalArgumentException> { med().copy(name = " ") }
        assertThrows<IllegalArgumentException> { med(times = emptyList()) }
        assertThrows<IllegalArgumentException> { med(days = emptySet()) }
        assertThrows<IllegalArgumentException> { med(stock = -1) }
        assertThrows<IllegalArgumentException> { med(unitsPerDose = 0) }
        assertThrows<IllegalArgumentException> { med(lowStockAt = -1) }
        assertThrows<IllegalArgumentException> { med(minGap = Duration.ofMinutes(-1)) }
    }

    @Test
    fun `saatler dakikalik sirali ve tekil olmali`() {
        val raw = listOf(LocalTime.of(20, 0, 30), LocalTime.of(8, 0), LocalTime.of(20, 0))
        assertThrows<IllegalArgumentException> { med(times = raw) }
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), Medication.normalizeTimes(raw))
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), med(times = Medication.normalizeTimes(raw)).times)
    }

    // --- Çizelge: her saat bir kritik hatırlatma ---

    @Test
    fun `her saat icin bir kritik hatirlatma tanimi uretilir`() {
        val defs = MedicationSchedule.definitions(med())

        assertEquals(2, defs.size)
        assertTrue(defs.all { it.klass == ReminderClass.CRITICAL && it.recurrence == Recurrence.Daily && it.active })
        assertEquals(listOf(LocalDateTime.of(2026, 10, 11, 8, 0), LocalDateTime.of(2026, 10, 11, 20, 0)), defs.map { it.startLocal })
        assertEquals(setOf(MedicationSchedule.doseReminderId("m1", LocalTime.of(8, 0)), MedicationSchedule.doseReminderId("m1", LocalTime.of(20, 0))), defs.map { it.id }.toSet())
    }

    @Test
    fun `haftanin bazi gunleri secilince haftalik tekrar uretilir`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        val def = MedicationSchedule.definitions(med(times = listOf(LocalTime.of(9, 30)), days = days)).single()
        assertEquals(Recurrence.Weekly(days), def.recurrence)
    }

    @Test
    fun `pasif ilacin tanimlari pasif uretilir`() {
        assertTrue(MedicationSchedule.definitions(med(active = false)).none { it.active })
    }

    @Test
    fun `hatirlatma kimligi ilaca ve rolune geri cozulur`() {
        val dose = MedicationSchedule.doseReminderId("abc-123", LocalTime.of(8, 5))
        val restock = MedicationSchedule.restockReminderId("abc-123", at("2026-10-11T08:06"))

        assertEquals(MedicationSchedule.Parsed("abc-123", MedicationRole.DOSE), MedicationSchedule.parse(dose))
        assertEquals(MedicationSchedule.Parsed("abc-123", MedicationRole.RESTOCK), MedicationSchedule.parse(restock))
        assertNull(MedicationSchedule.parse("r1"))
        assertNull(MedicationSchedule.parse("med:"))
        assertTrue(MedicationSchedule.isMedicationReminder(dose))
        assertFalse(MedicationSchedule.isMedicationReminder("medikal-olmayan"))
    }

    // --- Çift doz koruması ---

    @Test
    fun `asgari aralik dolmadan aldim denirse sorulur`() {
        val result = DoubleDoseGuard.check(lastTakenAt = at("2026-10-11T08:00"), minGap = Duration.ofHours(4), now = at("2026-10-11T10:30"))
        assertEquals(DoubleDoseCheck.TooSoon(Duration.ofMinutes(150)), result)
    }

    @Test
    fun `aralik tam dolunca ya da kayit yokken ya da aralik sifirken sorulmaz`() {
        assertEquals(DoubleDoseCheck.Clear, DoubleDoseGuard.check(at("2026-10-11T08:00"), Duration.ofHours(4), at("2026-10-11T12:00")))
        assertEquals(DoubleDoseCheck.Clear, DoubleDoseGuard.check(null, Duration.ofHours(4), at("2026-10-11T12:00")))
        assertEquals(DoubleDoseCheck.Clear, DoubleDoseGuard.check(at("2026-10-11T11:59"), Duration.ZERO, at("2026-10-11T12:00")))
    }

    @Test
    fun `aralik dolmasina bir saniye kala yine sorulur`() {
        val result = DoubleDoseGuard.check(at("2026-10-11T08:00"), Duration.ofHours(4), at("2026-10-11T11:59:59"))
        assertTrue(result is DoubleDoseCheck.TooSoon)
    }

    @Test
    fun `saat geri alinmissa son kayit gelecekte gorunur ve yine sorulur`() {
        val result = DoubleDoseGuard.check(at("2026-10-11T12:00"), Duration.ofHours(4), at("2026-10-11T11:00"))
        assertEquals(DoubleDoseCheck.TooSoon(Duration.ZERO), result)
    }

    // --- Kaydın durumu ---

    @Test
    fun `kaydedilmedi siniri icinde alinan doz zamaninda sonrasi gec sayilir`() {
        val planned = at("2026-10-11T08:00")
        assertEquals(DoseStatus.TAKEN, DoseRules.statusForTaken(planned, at("2026-10-11T08:59:59")))
        assertEquals(DoseStatus.TAKEN, DoseRules.statusForTaken(planned, at("2026-10-11T09:00")))
        assertEquals(DoseStatus.LATE, DoseRules.statusForTaken(planned, at("2026-10-11T09:00:01")))
        // Vaktinden önce "Aldım" (bildirimden önce ekrandan) zamanında sayılır.
        assertEquals(DoseStatus.TAKEN, DoseRules.statusForTaken(planned, at("2026-10-11T07:50")))
    }

    // --- Stok ---

    @Test
    fun `alinan doz stoktan duser ve esik gecilince bir kez eczane uyarisi verilir`() {
        val first = StockRule.afterDose(stock = 6, unitsPerDose = 2, lowStockAt = 4)
        val second = StockRule.afterDose(stock = first.stock, unitsPerDose = 2, lowStockAt = 4)

        assertEquals(StockChange(stock = 4, restock = true), first)
        assertEquals(StockChange(stock = 2, restock = false), second)
    }

    @Test
    fun `stok sifirin altina inmez stoksuz ilacta ve esiksiz ilacta uyari yok`() {
        assertEquals(StockChange(stock = 0, restock = true), StockRule.afterDose(stock = 1, unitsPerDose = 3, lowStockAt = 0))
        assertEquals(StockChange(stock = null, restock = false), StockRule.afterDose(stock = null, unitsPerDose = 1, lowStockAt = 5))
        assertEquals(StockChange(stock = 2, restock = false), StockRule.afterDose(stock = 3, unitsPerDose = 1, lowStockAt = null))
        assertEquals(StockChange(stock = 0, restock = false), StockRule.afterDose(stock = 0, unitsPerDose = 1, lowStockAt = 2))
    }

    // --- Geçmiş ve kaçan doz ---

    @Test
    fun `gecmiste kayitli doz durumuyla kayitsiz doz altmis dakika sonra kaydedilmedi gorunur`() {
        val now = at("2026-10-11T21:00")
        val planned = listOf(
            PlannedDose("m1", at("2026-10-11T08:00"), "k1"),
            PlannedDose("m1", at("2026-10-11T14:00"), "k2"),
            PlannedDose("m1", at("2026-10-11T20:00"), "k3"),
            PlannedDose("m1", at("2026-10-11T20:30"), "k4"),
        )
        val logs = listOf(
            MedicationLog("m1", at("2026-10-11T08:00"), at("2026-10-11T08:03"), DoseStatus.TAKEN),
            MedicationLog("m1", at("2026-10-11T14:00"), at("2026-10-11T14:10"), DoseStatus.SKIPPED),
        )

        val rows = DoseHistory.rows(planned, logs, now)

        assertEquals(listOf("k4", "k3", "k2", "k1"), rows.map { it.occurrenceKey })
        assertEquals(listOf(DoseState.PENDING, DoseState.NOT_LOGGED, DoseState.SKIPPED, DoseState.TAKEN), rows.map { it.state })
        assertEquals(at("2026-10-11T08:03"), rows.last().loggedAt)
    }

    @Test
    fun `kaydedilmedi siniri tam altmisinci dakikada baslar`() {
        val planned = listOf(PlannedDose("m1", at("2026-10-11T08:00"), "k"))
        assertEquals(DoseState.PENDING, DoseHistory.rows(planned, emptyList(), at("2026-10-11T08:59:59")).single().state)
        assertEquals(DoseState.NOT_LOGGED, DoseHistory.rows(planned, emptyList(), at("2026-10-11T09:00")).single().state)
    }

    @Test
    fun `yalniz kaydedilmemis ya da bekleyen doz sonradan kaydedilebilir`() {
        assertTrue(DoseState.NOT_LOGGED.canLogLater)
        assertTrue(DoseState.PENDING.canLogLater)
        assertFalse(DoseState.TAKEN.canLogLater)
        assertFalse(DoseState.SKIPPED.canLogLater)
    }

    // --- Dışa aktarım ---

    @Test
    fun `csv virgul tirnak ve satir sonunu kacirir`() {
        val text = Csv.format(listOf(listOf("Ad", "Not"), listOf("A, B", "\"tırnak\"\nikinci satır")))
        assertEquals("Ad,Not\r\n\"A, B\",\"\"\"tırnak\"\"\nikinci satır\"\r\n", text)
    }

    @Test
    fun `csv formul gibi baslayan hucreyi metne cevirir`() {
        val text = Csv.format(listOf(listOf("=1+1", "+90", "-3", "@x", "normal")))
        assertEquals("'=1+1,'+90,'-3,'@x,normal\r\n", text)
    }
}
