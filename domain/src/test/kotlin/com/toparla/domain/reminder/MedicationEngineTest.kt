package com.toparla.domain.reminder

import com.toparla.domain.medication.DoseStatus
import com.toparla.domain.medication.DoubleDoseCheck
import com.toparla.domain.medication.Medication
import com.toparla.domain.medication.MedicationLog
import com.toparla.domain.medication.MedicationRecorder
import com.toparla.domain.medication.MedicationRepository
import com.toparla.domain.medication.MedicationSchedule
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * F2.39: ilaç hatırlatması motorun kritik yolundan geçer (blueprint M9 "açılınca M6 kritik testlerini geçer");
 * eylemleri Aldım · 15 dk sonra · Atlıyorum; çift doz koruması; kayıt ve stok.
 */
class MedicationEngineTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val config = FollowUpConfig(Duration.ofMinutes(30), zone, LocalTime.of(23, 30), LocalTime.of(7, 30))

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private class FakeMedications : MedicationRepository {
        val meds = LinkedHashMap<String, Medication>()
        val logs = LinkedHashMap<Pair<String, Instant>, MedicationLog>()
        val restocks = ArrayList<Pair<String, Instant>>()

        override suspend fun medication(id: String) = meds[id]

        override suspend fun lastTakenAt(medicationId: String): Instant? =
            logs.values.filter { it.medicationId == medicationId && it.status.counted }.maxOfOrNull { it.loggedAt }

        override suspend fun log(medicationId: String, plannedAt: Instant) = logs[medicationId to plannedAt]

        override suspend fun saveLog(log: MedicationLog) {
            logs[log.medicationId to log.plannedAt] = log
        }

        override suspend fun updateStock(medicationId: String, stock: Int, at: Instant) {
            meds[medicationId]?.let { meds[medicationId] = it.copy(stock = stock) }
        }

        override suspend fun requestRestock(medicationId: String, at: Instant) {
            restocks += medicationId to at
        }
    }

    private val repo = FakeRepo()
    private val scheduler = FakeScheduler()
    private val notifier = FakeNotifier()
    private val meds = FakeMedications()
    private val engine = ReminderEngine(repo, scheduler, notifier, MedicationRecorder(meds)) { config }

    private val morning = LocalTime.of(8, 0)
    private val doseId = MedicationSchedule.doseReminderId("m1", morning)

    private fun addMedication(minGap: Duration = Duration.ofHours(4), stock: Int? = null, lowStockAt: Int? = null) {
        val med = Medication(
            id = "m1", name = "Örnek", doseText = "1 tablet", times = listOf(morning), startDate = LocalDate.of(2026, 10, 11),
            zoneId = zone, minGap = minGap, stock = stock, lowStockAt = lowStockAt,
        )
        meds.meds[med.id] = med
        MedicationSchedule.definitions(med).forEach { repo.add(it, title = med.name, medication = MedicationTag("m1", MedicationRole.DOSE)) }
    }

    /** 08:00 dozunu teslim eder ve teslimin anahtarını döner. */
    private suspend fun deliverMorningDose(): String {
        engine.replan(at("2026-10-11T07:00"))
        val key = ReminderPlanner.keyOf(doseId, at("2026-10-11T08:00"))
        engine.onAlarmFired(key, at("2026-10-11T08:00"))
        return key
    }

    @Test
    fun `ilac dozu kritik yoldan kurulur ve basligi kilitli kopyaya yazilmaz`() = runBlocking {
        addMedication()
        engine.replan(at("2026-10-11T07:00"))

        val key = ReminderPlanner.keyOf(doseId, at("2026-10-11T08:00"))
        assertEquals(AlarmApi.ALARM_CLOCK, scheduler.armed.getValue(key).api)
        // Cihaz korumalı kopya kilit açılmadan okunabilir: ilaç adı oraya girmez (blueprint D25 "İlaç zamanı").
        assertNull(scheduler.titles.getValue(key))
    }

    @Test
    fun `kritik bekcinin yeniden kurdugu ilac alarminda da baslik kopyaya yazilmaz`() = runBlocking {
        addMedication()
        // Alarm hiç kurulmamış: bekçi yakındaki kritik dozu bulur ve kurar.
        engine.watchdog(at("2026-10-11T07:50"))

        val key = ReminderPlanner.keyOf(doseId, at("2026-10-11T08:00"))
        assertTrue(key in scheduler.armed)
        assertNull(scheduler.titles.getValue(key))
    }

    @Test
    fun `ilac dozu kritik merdivenle yukselir`() = runBlocking {
        addMedication()
        val key = deliverMorningDose()
        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 1), at("2026-10-11T08:02"))
        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 2), at("2026-10-11T08:05"))

        assertEquals(listOf(LadderAction.NOTIFY, LadderAction.VIBRATE_REPEAT, LadderAction.FULL_SCREEN), notifier.shown.map { it.action })
    }

    @Test
    fun `aldim dozu kapatir zamaninda diye kaydeder ve stoktan duser`() = runBlocking {
        addMedication(stock = 10)
        val key = deliverMorningDose()

        engine.onAction(key, ReminderAction.DONE, at("2026-10-11T08:01"))

        assertEquals(OccurrenceState.DONE, repo.occurrences.getValue(key).state)
        val log = meds.logs.values.single()
        assertEquals(DoseStatus.TAKEN, log.status)
        assertEquals(at("2026-10-11T08:00"), log.plannedAt)
        assertEquals(9, meds.meds.getValue("m1").stock)
        assertTrue(key in notifier.cancelled)
    }

    @Test
    fun `asgari aralik dolmadan aldim denirse doz kapanmaz ve emin misin sorulur`() = runBlocking {
        addMedication(minGap = Duration.ofHours(4))
        meds.saveLog(MedicationLog("m1", at("2026-10-11T06:00"), at("2026-10-11T06:30"), DoseStatus.TAKEN))
        val key = deliverMorningDose()

        engine.onAction(key, ReminderAction.DONE, at("2026-10-11T08:01"))

        assertEquals(OccurrenceState.DELIVERED, repo.occurrences.getValue(key).state)
        assertEquals(listOf(key to Duration.ofMinutes(91)), notifier.doubleDoseAsked)
        assertNull(meds.logs[("m1" to at("2026-10-11T08:00"))])
        // Soru sorulması bir yanıt değildir: merdiven sürer.
        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 1), at("2026-10-11T08:02"))
        assertEquals(LadderAction.VIBRATE_REPEAT, notifier.shown.last().action)
    }

    @Test
    fun `emin misin sorusuna evet denince doz kaydedilir`() = runBlocking {
        addMedication(minGap = Duration.ofHours(4))
        meds.saveLog(MedicationLog("m1", at("2026-10-11T06:00"), at("2026-10-11T06:30"), DoseStatus.TAKEN))
        val key = deliverMorningDose()

        engine.onAction(key, ReminderAction.DONE, at("2026-10-11T08:01"))
        engine.onAction(key, ReminderAction.CONFIRM_TAKEN, at("2026-10-11T08:02"))

        assertEquals(OccurrenceState.DONE, repo.occurrences.getValue(key).state)
        assertEquals(DoseStatus.TAKEN, meds.logs.getValue("m1" to at("2026-10-11T08:00")).status)
    }

    @Test
    fun `vazgec denince ilk bildirim geri gelir doz acik kalir`() = runBlocking {
        addMedication(minGap = Duration.ofHours(4))
        meds.saveLog(MedicationLog("m1", at("2026-10-11T06:00"), at("2026-10-11T06:30"), DoseStatus.TAKEN))
        val key = deliverMorningDose()
        engine.onAction(key, ReminderAction.DONE, at("2026-10-11T08:01"))
        val before = notifier.shown.size

        engine.onAction(key, ReminderAction.KEEP_OPEN, at("2026-10-11T08:01:30"))

        assertEquals(before + 1, notifier.shown.size)
        assertEquals(key, notifier.shown.last().occurrenceKey)
        assertEquals(OccurrenceState.DELIVERED, repo.occurrences.getValue(key).state)
    }

    @Test
    fun `on bes dakika sonra varsayilan ilac ertelemesidir ve en cok otuz dakika`() = runBlocking {
        addMedication()
        val key = deliverMorningDose()

        engine.onAction(key, ReminderAction.SNOOZE, at("2026-10-11T08:01"))
        val snoozed = SnoozePolicy.snoozedKey(key, 1)
        assertEquals(at("2026-10-11T08:16"), repo.occurrences.getValue(snoozed).plannedAt)

        engine.onAlarmFired(snoozed, at("2026-10-11T08:16"))
        engine.onAction(snoozed, ReminderAction.SNOOZE, at("2026-10-11T08:17"), snoozeDelay = Duration.ofMinutes(45))
        assertEquals(at("2026-10-11T08:47"), repo.occurrences.getValue(SnoozePolicy.snoozedKey(key, 2)).plannedAt)
    }

    @Test
    fun `ertelenen doz aldim denince ilk planlanan ana yazilir`() = runBlocking {
        addMedication()
        val key = deliverMorningDose()
        engine.onAction(key, ReminderAction.SNOOZE, at("2026-10-11T08:01"))
        val snoozed = SnoozePolicy.snoozedKey(key, 1)
        engine.onAlarmFired(snoozed, at("2026-10-11T08:16"))

        engine.onAction(snoozed, ReminderAction.DONE, at("2026-10-11T08:17"))

        assertEquals(at("2026-10-11T08:00"), meds.logs.values.single().plannedAt)
        assertEquals(DoseStatus.TAKEN, meds.logs.values.single().status)
    }

    @Test
    fun `atliyorum dozu kapatir atlandi diye kaydeder stoga dokunmaz`() = runBlocking {
        addMedication(stock = 10)
        val key = deliverMorningDose()

        engine.onAction(key, ReminderAction.NOT_TODAY, at("2026-10-11T08:01"))

        assertEquals(OccurrenceState.SKIPPED, repo.occurrences.getValue(key).state)
        assertEquals(DoseStatus.SKIPPED, meds.logs.values.single().status)
        assertEquals(10, meds.meds.getValue("m1").stock)
    }

    @Test
    fun `ilac dozu yarina tasinmaz doz zamani degistirilmez`() = runBlocking {
        addMedication()
        val key = deliverMorningDose()

        engine.onAction(key, ReminderAction.TOMORROW, at("2026-10-11T08:01"))

        assertEquals(OccurrenceState.DELIVERED, repo.occurrences.getValue(key).state)
        assertTrue(repo.occurrences.keys.none { AlarmKey.parse(it).kind == AlarmKeyKind.SNOOZE })
    }

    @Test
    fun `kaydedilmeden kapanan doz sonradan gec alindi diye kaydedilebilir`() = runBlocking {
        addMedication()
        val key = deliverMorningDose()
        // Merdiven biter (08:10'daki son basamak), süre dolar ("Kaydedilmedi").
        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 1), at("2026-10-11T08:02"))
        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 2), at("2026-10-11T08:05"))
        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 3), at("2026-10-11T08:10"))
        engine.replan(at("2026-10-11T09:15"))
        assertEquals(OccurrenceState.EXPIRED, repo.occurrences.getValue(key).state)

        engine.onAction(key, ReminderAction.CONFIRM_TAKEN, at("2026-10-11T13:00"))

        assertEquals(OccurrenceState.DONE, repo.occurrences.getValue(key).state)
        assertEquals(DoseStatus.LATE, meds.logs.values.single().status)
    }

    @Test
    fun `ayni doz ikinci kez kaydedilince stok bir daha dusmez`() = runBlocking {
        addMedication(stock = 10)
        val key = deliverMorningDose()
        engine.onAction(key, ReminderAction.DONE, at("2026-10-11T08:01"))

        // Ekrandan aynı dozu yeniden kaydetme (çift dokunuş): durum değişmez, stok bir kez düşer.
        engine.onAction(key, ReminderAction.CONFIRM_TAKEN, at("2026-10-11T08:02"))

        assertEquals(9, meds.meds.getValue("m1").stock)
    }

    @Test
    fun `kaydedici ayni dozu ikinci kez yazinca ya da kayit duzeltilince stogu bir kez dusurur`() = runBlocking {
        addMedication(stock = 10)
        val recorder = MedicationRecorder(meds)
        val dose = at("2026-10-11T08:00")

        recorder.taken("m1", dose, at("2026-10-11T08:01"))
        recorder.taken("m1", dose, at("2026-10-11T08:02"))
        assertEquals(9, meds.meds.getValue("m1").stock)

        // Önce "Atlıyorum", sonra düzeltme "Aldım": stok o zaman düşer.
        val evening = at("2026-10-11T20:00")
        recorder.skipped("m1", evening, at("2026-10-11T20:01"))
        assertEquals(9, meds.meds.getValue("m1").stock)
        recorder.taken("m1", evening, at("2026-10-11T20:05"))
        assertEquals(8, meds.meds.getValue("m1").stock)
        assertEquals(DoseStatus.TAKEN, meds.logs.getValue("m1" to evening).status)
    }

    @Test
    fun `silinmis ilacta cift doz sorulmaz`() = runBlocking {
        assertEquals(DoubleDoseCheck.Clear, MedicationRecorder(meds).check("yok", at("2026-10-11T08:00")))
    }

    @Test
    fun `stok esigi gecilince eczane hatirlatmasi istenir`() = runBlocking {
        addMedication(stock = 3, lowStockAt = 2)
        val key = deliverMorningDose()

        engine.onAction(key, ReminderAction.DONE, at("2026-10-11T08:01"))

        assertEquals(2, meds.meds.getValue("m1").stock)
        assertEquals(listOf("m1" to at("2026-10-11T08:01").plus(MedicationRecorder.RESTOCK_DELAY)), meds.restocks)
    }

    @Test
    fun `ilac olmayan hatirlatmada aldim eski gibi yaptim sayilir`() = runBlocking {
        repo.add(ReminderDef("r", ReminderClass.CRITICAL, LocalDateTime.parse("2026-10-11T08:00"), zone))
        engine.replan(at("2026-10-11T07:00"))
        val key = ReminderPlanner.keyOf("r", at("2026-10-11T08:00"))
        engine.onAlarmFired(key, at("2026-10-11T08:00"))

        engine.onAction(key, ReminderAction.DONE, at("2026-10-11T08:01"))

        assertEquals(OccurrenceState.DONE, repo.occurrences.getValue(key).state)
        assertTrue(meds.logs.isEmpty())
        assertTrue(notifier.doubleDoseAsked.isEmpty())
    }
}
