package com.toparla.domain.medication

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** F2.39–F2.40: ilaç formunun doğrulaması ve dışa aktarım tablosu (ekran yalnız bunları çağırır). */
class MedicationFormTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val today = LocalDate.of(2026, 10, 11)

    private fun input(
        name: String = "Sabah ilacı",
        times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        stock: String = "",
        perDose: String = "",
        lowStock: String = "",
        gapHours: Int = 4,
    ) = MedicationInput(
        name = name, doseText = " 1 tablet ", times = times, days = days, minGapHours = gapHours,
        stock = stock, perDose = perDose, lowStock = lowStock, notes = "", hideName = false,
    )

    @Test
    fun `gecerli form ilaca donusur metinler kirpilir bos sayilar varsayilana duser`() {
        val result = MedicationForm.build("m1", input(), today, zone) as MedicationForm.Result.Ok

        val med = result.medication
        assertEquals("Sabah ilacı", med.name)
        assertEquals("1 tablet", med.doseText)
        assertEquals(null, med.stock)
        assertEquals(1, med.unitsPerDose)
        assertEquals(null, med.lowStockAt)
        assertEquals(Duration.ofHours(4), med.minGap)
        assertEquals(today, med.startDate)
    }

    @Test
    fun `adsiz saatsiz gunsuz ya da bozuk stoklu form hangi alanin eksik oldugunu soyler`() {
        val result = MedicationForm.build("m1", input(name = " ", times = emptyList(), days = emptySet(), stock = "-3", perDose = "0"), today, zone)

        assertEquals(
            MedicationForm.Result.Invalid(setOf(MedicationForm.Problem.NAME, MedicationForm.Problem.TIMES, MedicationForm.Problem.DAYS, MedicationForm.Problem.STOCK)),
            result,
        )
    }

    @Test
    fun `sayi olmayan ya da cok buyuk stok reddedilir`() {
        assertTrue(MedicationForm.build("m1", input(stock = "otuz"), today, zone) is MedicationForm.Result.Invalid)
        assertTrue(MedicationForm.build("m1", input(lowStock = "100000"), today, zone) is MedicationForm.Result.Invalid)
        assertTrue(MedicationForm.build("m1", input(stock = "30", perDose = "2", lowStock = "5"), today, zone) is MedicationForm.Result.Ok)
    }

    @Test
    fun `cift doz araligi yalniz onerilen seceneklerden biri olabilir`() {
        assertEquals(listOf(0, 2, 4, 6, 8, 12), MedicationForm.GAP_OPTIONS_HOURS)
        assertTrue(MedicationForm.build("m1", input(gapHours = 5), today, zone) is MedicationForm.Result.Invalid)
        val off = MedicationForm.build("m1", input(gapHours = 0), today, zone) as MedicationForm.Result.Ok
        assertEquals(Duration.ZERO, off.medication.minGap)
    }

    @Test
    fun `saatler forma girerken dakikaya yuvarlanir siralanir ve tekillesir`() {
        val result = MedicationForm.build("m1", input(times = listOf(LocalTime.of(20, 0, 15), LocalTime.of(8, 0), LocalTime.of(20, 0))), today, zone)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), (result as MedicationForm.Result.Ok).medication.times)
    }

    @Test
    fun `var olan ilac duzenlenince baslangic gunu ve etkinligi korunur`() {
        val original = (MedicationForm.build("m1", input(), today.minusDays(10), zone) as MedicationForm.Result.Ok).medication
        val edited = MedicationForm.build("m1", input(name = "Yeni ad"), today, zone, existing = original) as MedicationForm.Result.Ok
        assertEquals(today.minusDays(10), edited.medication.startDate)
        assertEquals("Yeni ad", edited.medication.name)
    }

    @Test
    fun `mevcut ilac forma geri doner`() {
        val med = Medication(
            id = "m1", name = "A", doseText = "1", times = listOf(LocalTime.of(9, 0)), days = setOf(DayOfWeek.MONDAY),
            startDate = today, zoneId = zone, minGap = Duration.ofHours(6), stock = 12, unitsPerDose = 2, lowStockAt = 4, notes = "n", hideName = true,
        )
        val back = MedicationInput.of(med)
        assertEquals(med, (MedicationForm.build("m1", back, today.plusDays(3), zone, existing = med) as MedicationForm.Result.Ok).medication)
    }

    // --- Dışa aktarım tablosu ---

    private val labels = ExportLabels(
        header = listOf("Tarih", "Planlanan saat", "İlaç", "Doz", "Durum", "Kayıt saati"),
        states = mapOf(
            DoseState.TAKEN to "Alındı", DoseState.LATE to "Geç alındı", DoseState.SKIPPED to "Atlandı",
            DoseState.NOT_LOGGED to "Kaydedilmedi", DoseState.PENDING to "Bekliyor", DoseState.UPCOMING to "Sırada", DoseState.SNOOZED to "Ertelendi",
        ),
    )

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    @Test
    fun `disa aktarim tablosu basliktan sonra eskiden yeniye satirlar uretir`() {
        val rows = listOf(
            DoseRow("m1", at("2026-10-11T20:00"), "k2", DoseState.NOT_LOGGED),
            DoseRow("m1", at("2026-10-11T08:00"), "k1", DoseState.TAKEN, loggedAt = at("2026-10-11T08:03")),
        )
        val table = ExportTable.build(rows, mapOf("m1" to ("Sabah ilacı" to "1 tablet")), zone, labels)

        assertEquals(labels.header, table.first())
        assertEquals(listOf("2026-10-11", "08:00", "Sabah ilacı", "1 tablet", "Alındı", "08:03"), table[1])
        assertEquals(listOf("2026-10-11", "20:00", "Sabah ilacı", "1 tablet", "Kaydedilmedi", ""), table[2])
    }

    @Test
    fun `adi bilinmeyen ilac bos adla yazilir satir kaybolmaz`() {
        val table = ExportTable.build(listOf(DoseRow("x", at("2026-10-11T08:00"), "k", DoseState.SKIPPED)), emptyMap(), zone, labels)
        assertEquals(listOf("2026-10-11", "08:00", "", "", "Atlandı", ""), table[1])
    }
}
