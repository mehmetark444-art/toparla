package com.toparla.domain.reminder

import java.time.Duration
import java.time.Instant

/** Motor testlerinin sahte deposu, alarmı ve bildirimi (ReminderEngineTest, MedicationEngineTest). */
internal class FakeRepo : ReminderRepository {
    val defs = LinkedHashMap<String, Pair<ReminderDef, ReminderInfo>>()
    val alarms = LinkedHashMap<String, ScheduledAlarm>()
    val fired = LinkedHashSet<String>()
    val logs = ArrayList<Pair<String, String>>()
    val occurrences = LinkedHashMap<String, OccurrenceRecord>()

    fun add(def: ReminderDef, title: String = def.id, persistent: Boolean = false, medication: MedicationTag? = null) {
        defs[def.id] = def to ReminderInfo(def.id, def.klass, title, "", persistent, medication)
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

internal class FakeScheduler : ReminderScheduler {
    val armed = LinkedHashMap<String, PlannedAlarm>()
    val titles = LinkedHashMap<String, String?>()
    var maintenanceAt: Instant? = null

    override fun schedule(alarm: PlannedAlarm, title: String?) {
        armed[alarm.key] = alarm
        titles[alarm.key] = title
    }

    override fun cancel(key: String) {
        armed.remove(key)
    }

    override fun scheduleMaintenance(at: Instant) {
        maintenanceAt = at
    }
}

internal class FakeNotifier : ReminderNotifier {
    val shown = ArrayList<DeliveryNotice>()
    val cancelled = ArrayList<String>()
    var persistent: List<PersistentItem> = emptyList()
    val asked = ArrayList<String>()
    val doubleDoseAsked = ArrayList<Pair<String, Duration>>()
    var blocked = false

    override fun show(notice: DeliveryNotice) {
        shown += notice
    }

    override fun isBlocked(klass: ReminderClass) = blocked

    override fun cancel(occurrenceKey: String) {
        cancelled += occurrenceKey
    }

    override fun showPersistent(items: List<PersistentItem>, askedAt: Instant) {
        persistent = items
    }

    override fun askCarryOrSkip(occurrenceKey: String, reminder: ReminderInfo) {
        asked += occurrenceKey
    }

    override fun askDoubleDose(occurrenceKey: String, reminder: ReminderInfo, sinceLastDose: Duration) {
        doubleDoseAsked += occurrenceKey to sinceLastDose
    }
}
