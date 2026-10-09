package com.toparla.domain.reminder

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** F2-D çekirdeği: teslim hattı, merdiven, ısrarlı takip, eylemler ve toparlanma; sahte depo, alarm ve bildirimle. */
class ReminderEngineTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val config = FollowUpConfig(Duration.ofMinutes(30), zone, LocalTime.of(23, 30), LocalTime.of(7, 30))

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private class FakeRepo : ReminderRepository {
        val defs = LinkedHashMap<String, Pair<ReminderDef, ReminderInfo>>()
        val alarms = LinkedHashMap<String, ScheduledAlarm>()
        val fired = LinkedHashSet<String>()
        val logs = ArrayList<Pair<String, String>>()
        val occurrences = LinkedHashMap<String, OccurrenceRecord>()

        fun add(def: ReminderDef, title: String = def.id, persistent: Boolean = false) {
            defs[def.id] = def to ReminderInfo(def.id, def.klass, title, "", persistent)
        }

        override suspend fun activeDefinitions() = defs.values.map { it.first }.filter { it.active }

        override suspend fun info(reminderId: String) = defs[reminderId]?.second

        override suspend fun scheduled() = alarms.values.toList()

        override suspend fun applyPlan(scheduled: List<PlannedAlarm>, cancelledKeys: List<String>) {
            cancelledKeys.forEach { alarms.remove(it) }
            scheduled.forEach { alarms[it.key] = ScheduledAlarm(it.key, it.fireAt) }
        }

        override suspend fun removeScheduled(key: String) {
            alarms.remove(key)
        }

        override suspend fun recordFired(key: String, at: Instant) = fired.add(key)

        override suspend fun firedKeys(keys: List<String>) = keys.filterTo(HashSet()) { it in fired }

        override suspend fun log(key: String, at: Instant, event: String, detail: String?) {
            logs += key to event
        }

        override suspend fun occurrence(key: String) = occurrences[key]

        override suspend fun saveOccurrence(record: OccurrenceRecord) {
            occurrences[record.key] = record
        }

        override suspend fun openOccurrences() =
            occurrences.values.filter { it.state == OccurrenceState.DELIVERED || it.state == OccurrenceState.SEEN }

        override suspend fun pendingSnoozes() =
            occurrences.values.filter { it.state == OccurrenceState.PLANNED && AlarmKey.parse(it.key).kind == AlarmKeyKind.SNOOZE }
    }

    private class FakeScheduler : ReminderScheduler {
        val armed = LinkedHashMap<String, PlannedAlarm>()
        var maintenanceAt: Instant? = null

        override fun schedule(alarm: PlannedAlarm) {
            armed[alarm.key] = alarm
        }

        override fun cancel(key: String) {
            armed.remove(key)
        }

        override fun scheduleMaintenance(at: Instant) {
            maintenanceAt = at
        }
    }

    private class FakeNotifier : ReminderNotifier {
        val shown = ArrayList<DeliveryNotice>()
        val cancelled = ArrayList<String>()
        var persistent: List<PersistentItem> = emptyList()
        val asked = ArrayList<String>()

        override fun show(notice: DeliveryNotice) {
            shown += notice
        }

        override fun cancel(occurrenceKey: String) {
            cancelled += occurrenceKey
        }

        override fun showPersistent(items: List<PersistentItem>, askedAt: Instant) {
            persistent = items
        }

        override fun askCarryOrSkip(occurrenceKey: String, reminder: ReminderInfo) {
            asked += occurrenceKey
        }
    }

    private val repo = FakeRepo()
    private val scheduler = FakeScheduler()
    private val notifier = FakeNotifier()
    private val engine = ReminderEngine(repo, scheduler, notifier) { config }

    private fun def(id: String, klass: ReminderClass, start: String, recurrence: Recurrence = Recurrence.Once) =
        ReminderDef(id, klass, LocalDateTime.parse(start), zone, recurrence)

    private fun mainKey(id: String, local: String) = ReminderPlanner.keyOf(id, at(local))

    @Test
    fun `planlama alarmlari kurar kaydeder bakimi planlar ve ikinci kez ayni seyi yapmaz`() = runBlocking {
        repo.add(def("r", ReminderClass.CRITICAL, "2026-10-08T10:00"))

        engine.replan(at("2026-10-08T09:00"))
        val armedOnce = scheduler.armed.keys.toList()
        scheduler.armed.clear()
        engine.replan(at("2026-10-08T09:00"))

        assertEquals(listOf(mainKey("r", "2026-10-08T10:00")), armedOnce)
        assertEquals(armedOnce, repo.alarms.keys.toList())
        assertTrue(scheduler.armed.isEmpty())
        assertEquals(at("2026-10-08T21:00"), scheduler.maintenanceAt)
    }

    @Test
    fun `yeniden baslatmada kayitli butun alarmlar yeniden kurulur`() = runBlocking {
        repo.add(def("r", ReminderClass.CRITICAL, "2026-10-08T10:00"))
        engine.replan(at("2026-10-08T09:00"))
        scheduler.armed.clear() // sistem alarmları silindi

        engine.replan(at("2026-10-08T09:05"), rearm = true)

        assertEquals(setOf(mainKey("r", "2026-10-08T10:00")), scheduler.armed.keys)
    }

    @Test
    fun `kritik teslim bildirim gosterir kaydi siler ve siradaki merdiven basamagini kurar`() = runBlocking {
        repo.add(def("r", ReminderClass.CRITICAL, "2026-10-08T10:00"), title = "Dişçi")
        engine.replan(at("2026-10-08T09:00"))
        val key = mainKey("r", "2026-10-08T10:00")

        engine.onAlarmFired(key, at("2026-10-08T10:00:01"))

        val notice = notifier.shown.single()
        assertEquals("Dişçi", notice.reminder.title)
        assertEquals(LadderAction.NOTIFY, notice.action)
        assertEquals(Duration.ZERO, notice.lateBy)
        assertEquals(OccurrenceState.DELIVERED, repo.occurrences.getValue(key).state)
        assertFalse(key in repo.alarms)
        assertEquals(setOf(ReminderPlanner.ladderKey(key, 1)), scheduler.armed.keys - key)
    }

    @Test
    fun `ayni alarm iki kez ateslenirse tek bildirim gosterilir`() = runBlocking {
        repo.add(def("r", ReminderClass.NORMAL, "2026-10-08T10:00"))
        engine.replan(at("2026-10-08T09:00"))
        val key = mainKey("r", "2026-10-08T10:00")

        engine.onAlarmFired(key, at("2026-10-08T10:00"))
        engine.onAlarmFired(key, at("2026-10-08T10:00:05"))

        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `merdiven basamaklari sirayla isler ve eylemi tasir`() = runBlocking {
        repo.add(def("r", ReminderClass.CRITICAL, "2026-10-08T10:00"))
        engine.replan(at("2026-10-08T09:00"))
        val key = mainKey("r", "2026-10-08T10:00")
        engine.onAlarmFired(key, at("2026-10-08T10:00"))

        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 1), at("2026-10-08T10:02"))
        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 2), at("2026-10-08T10:05"))

        assertEquals(listOf(LadderAction.NOTIFY, LadderAction.VIBRATE_REPEAT, LadderAction.FULL_SCREEN), notifier.shown.map { it.action })
        assertEquals(3, repo.occurrences.getValue(key).ladderStepsDone)
        assertTrue(ReminderPlanner.ladderKey(key, 3) in scheduler.armed)
    }

    @Test
    fun `yaptim denince durum kapanir bildirim kalkar bekleyen basamak iptal edilir`() = runBlocking {
        repo.add(def("r", ReminderClass.CRITICAL, "2026-10-08T10:00"))
        engine.replan(at("2026-10-08T09:00"))
        val key = mainKey("r", "2026-10-08T10:00")
        engine.onAlarmFired(key, at("2026-10-08T10:00"))

        engine.onAction(key, ReminderAction.DONE, at("2026-10-08T10:01"))

        assertEquals(OccurrenceState.DONE, repo.occurrences.getValue(key).state)
        assertEquals(at("2026-10-08T10:01"), repo.occurrences.getValue(key).resolvedAt)
        assertTrue(key in notifier.cancelled)
        assertFalse(ReminderPlanner.ladderKey(key, 1) in scheduler.armed)
        assertTrue(repo.alarms.isEmpty())
        // Yanıtlanan işin geç gelen basamağı bildirim göstermez.
        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 1), at("2026-10-08T10:02"))
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `cift dokunus durumu bozmaz`() = runBlocking {
        repo.add(def("r", ReminderClass.NORMAL, "2026-10-08T10:00"))
        engine.replan(at("2026-10-08T09:00"))
        val key = mainKey("r", "2026-10-08T10:00")
        engine.onAlarmFired(key, at("2026-10-08T10:00"))

        engine.onAction(key, ReminderAction.DONE, at("2026-10-08T10:01"))
        engine.onAction(key, ReminderAction.SNOOZE, at("2026-10-08T10:02"))

        assertEquals(OccurrenceState.DONE, repo.occurrences.getValue(key).state)
        assertTrue(repo.pendingSnoozes().isEmpty())
    }

    @Test
    fun `erteleme on dakika sonraya yeni teslim kurar ucuncuden sonra tasi ya da atla sorulur`() = runBlocking {
        repo.add(def("r", ReminderClass.NORMAL, "2026-10-08T10:00"))
        engine.replan(at("2026-10-08T09:00"))
        var key = mainKey("r", "2026-10-08T10:00")
        var now = at("2026-10-08T10:00")
        engine.onAlarmFired(key, now)

        for (n in 1..3) {
            engine.onAction(key, ReminderAction.SNOOZE, now)
            val snoozed = SnoozePolicy.snoozedKey(mainKey("r", "2026-10-08T10:00"), n)
            assertEquals(now.plus(SnoozePolicy.DEFAULT_DELAY), scheduler.armed.getValue(snoozed).fireAt)
            assertEquals(OccurrenceState.SNOOZED, repo.occurrences.getValue(key).state)
            key = snoozed
            now = now.plus(SnoozePolicy.DEFAULT_DELAY)
            engine.onAlarmFired(key, now)
        }
        engine.onAction(key, ReminderAction.SNOOZE, now)

        assertEquals(listOf(key), notifier.asked)
        assertEquals(OccurrenceState.DELIVERED, repo.occurrences.getValue(key).state)
    }

    @Test
    fun `yarin secilince teslim ertesi sabah uyanis saatine tasinir`() = runBlocking {
        repo.add(def("r", ReminderClass.IMPORTANT, "2026-10-08T15:00"))
        engine.replan(at("2026-10-08T14:00"))
        val key = mainKey("r", "2026-10-08T15:00")
        engine.onAlarmFired(key, at("2026-10-08T15:00"))

        engine.onAction(key, ReminderAction.TOMORROW, at("2026-10-08T15:01"))

        assertEquals(at("2026-10-09T07:30"), scheduler.armed.getValue(SnoozePolicy.snoozedKey(key, 1)).fireAt)
    }

    @Test
    fun `bugun olmayacak israrli olmayan isi atlar israrli isi yarin sabaha tasir`() = runBlocking {
        repo.add(def("n", ReminderClass.NORMAL, "2026-10-08T15:00"))
        repo.add(def("p", ReminderClass.NORMAL, "2026-10-08T15:00"), persistent = true)
        engine.replan(at("2026-10-08T14:00"))
        val n = mainKey("n", "2026-10-08T15:00")
        val p = mainKey("p", "2026-10-08T15:00")
        engine.onAlarmFired(n, at("2026-10-08T15:00"))
        engine.onAlarmFired(p, at("2026-10-08T15:00"))

        engine.onAction(n, ReminderAction.NOT_TODAY, at("2026-10-08T15:01"))
        engine.onAction(p, ReminderAction.NOT_TODAY, at("2026-10-08T15:01"))

        assertEquals(OccurrenceState.SKIPPED, repo.occurrences.getValue(n).state)
        assertEquals(OccurrenceState.SNOOZED, repo.occurrences.getValue(p).state)
        assertEquals(at("2026-10-09T07:30"), scheduler.armed.getValue(SnoozePolicy.snoozedKey(p, 1)).fireAt)
        assertTrue(notifier.persistent.isEmpty())
    }

    @Test
    fun `israrli is yaptim denene dek yarim saatte bir sorulur ve tek birlesik bildirimde listelenir`() = runBlocking {
        repo.add(def("a", ReminderClass.NORMAL, "2026-10-08T10:00"), title = "Fatura", persistent = true)
        repo.add(def("b", ReminderClass.NORMAL, "2026-10-08T10:10"), title = "E-posta", persistent = true)
        engine.replan(at("2026-10-08T09:00"))
        val a = mainKey("a", "2026-10-08T10:00")
        val b = mainKey("b", "2026-10-08T10:10")

        engine.onAlarmFired(a, at("2026-10-08T10:00"))
        engine.onAlarmFired(b, at("2026-10-08T10:10"))
        assertEquals(at("2026-10-08T10:30"), scheduler.armed.getValue(ReminderPlanner.followUpKey(a, 0)).fireAt)

        engine.onAlarmFired(ReminderPlanner.followUpKey(a, 0), at("2026-10-08T10:30"))

        assertEquals(listOf("Fatura", "E-posta"), notifier.persistent.map { it.reminder.title })
        assertEquals(1, repo.occurrences.getValue(a).asksDone)
        assertEquals(at("2026-10-08T11:00"), scheduler.armed.getValue(ReminderPlanner.followUpKey(a, 1)).fireAt)
        // İlk teslimler tek tek gösterildi; takip sorusu ayrı bir tekil bildirim açmadı.
        assertEquals(2, notifier.shown.size)

        engine.onAction(a, ReminderAction.DONE, at("2026-10-08T10:31"))
        assertEquals(listOf("E-posta"), notifier.persistent.map { it.reminder.title })
        assertFalse(ReminderPlanner.followUpKey(a, 1) in scheduler.armed)
    }

    @Test
    fun `telefon kapaliyken vakti gecen teslim acilista gec teslim edilir ve kayda gecer`() = runBlocking {
        repo.add(def("r", ReminderClass.CRITICAL, "2026-10-08T10:00"), title = "Uçuş")
        engine.replan(at("2026-10-08T09:00"))
        scheduler.armed.clear()

        engine.replan(at("2026-10-08T10:20"), rearm = true)

        val notice = notifier.shown.single()
        assertEquals(Duration.ofMinutes(20), notice.lateBy)
        assertTrue(repo.logs.any { it.second == ReminderEngine.EVENT_MISSED_DETECTED })
        assertEquals(OccurrenceState.DELIVERED, repo.occurrences.getValue(mainKey("r", "2026-10-08T10:00")).state)
        // Aynı açılış ikinci kez işlenirse yeniden gösterilmez.
        engine.replan(at("2026-10-08T10:21"), rearm = true)
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `tanimi silinen isin bekleyen basamagi iptal edilir ve gec gelen alarmi gosterilmez`() = runBlocking {
        repo.add(def("r", ReminderClass.CRITICAL, "2026-10-08T10:00"))
        engine.replan(at("2026-10-08T09:00"))
        val key = mainKey("r", "2026-10-08T10:00")
        engine.onAlarmFired(key, at("2026-10-08T10:00"))

        repo.defs.remove("r")
        engine.replan(at("2026-10-08T10:01"))
        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 1), at("2026-10-08T10:02"))

        assertTrue(scheduler.armed.keys.none { it.startsWith(key) && it != key })
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `yanitsiz kalan israrsiz is merdiven bittikten bir saat sonra suresi doldu olur`() = runBlocking {
        repo.add(def("r", ReminderClass.NORMAL, "2026-10-08T10:00"))
        engine.replan(at("2026-10-08T09:00"))
        val key = mainKey("r", "2026-10-08T10:00")
        engine.onAlarmFired(key, at("2026-10-08T10:00"))
        engine.onAlarmFired(ReminderPlanner.ladderKey(key, 1), at("2026-10-08T10:30"))

        engine.replan(at("2026-10-08T11:00"))
        assertEquals(OccurrenceState.DELIVERED, repo.occurrences.getValue(key).state)
        engine.replan(at("2026-10-08T11:31"))

        assertEquals(OccurrenceState.EXPIRED, repo.occurrences.getValue(key).state)
    }

    @Test
    fun `bildirime dokunulunca goruldu olur merdiven surer`() = runBlocking {
        repo.add(def("r", ReminderClass.CRITICAL, "2026-10-08T10:00"))
        engine.replan(at("2026-10-08T09:00"))
        val key = mainKey("r", "2026-10-08T10:00")
        engine.onAlarmFired(key, at("2026-10-08T10:00"))

        engine.onAction(key, ReminderAction.OPENED, at("2026-10-08T10:00:30"))

        assertEquals(OccurrenceState.SEEN, repo.occurrences.getValue(key).state)
        assertTrue(ReminderPlanner.ladderKey(key, 1) in scheduler.armed)
    }

    @Test
    fun `kritik bekci yakindaki kritik olayin eksik alarmini kurar ve kayda yazar`() = runBlocking {
        repo.add(def("r", ReminderClass.CRITICAL, "2026-10-08T10:10"))

        engine.watchdog(at("2026-10-08T10:00"))

        assertTrue(mainKey("r", "2026-10-08T10:10") in scheduler.armed)
        assertTrue(repo.logs.any { it.second == ReminderEngine.EVENT_WATCHDOG_REARMED })
        scheduler.armed.clear()
        engine.watchdog(at("2026-10-08T10:01"))
        assertTrue(scheduler.armed.isEmpty())
    }

    @Test
    fun `bilinmeyen anahtar ve kaydi olmayan eylem sessizce yok sayilir`() = runBlocking {
        engine.onAlarmFired("yok@1", at("2026-10-08T10:00"))
        engine.onAction("yok@1", ReminderAction.DONE, at("2026-10-08T10:00"))

        assertTrue(notifier.shown.isEmpty())
        assertNull(repo.occurrences["yok@1"])
    }
}
