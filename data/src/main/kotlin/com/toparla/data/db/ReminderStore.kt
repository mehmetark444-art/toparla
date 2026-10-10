package com.toparla.data.db

import com.toparla.domain.reminder.AlarmKey
import com.toparla.domain.reminder.AlarmKeyKind
import com.toparla.domain.reminder.OccurrenceRecord
import com.toparla.domain.reminder.OccurrenceState
import com.toparla.domain.reminder.PlannedAlarm
import com.toparla.domain.reminder.RecurrenceCodec
import com.toparla.domain.reminder.ReminderDef
import com.toparla.domain.reminder.ReminderInfo
import com.toparla.domain.reminder.ReminderRepository
import com.toparla.domain.reminder.ScheduledAlarm
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.zone.ZoneRulesException

data class DeliveryStats(val pendingAlarms: Int, val lastDeliveredAt: Instant?)

/**
 * Hatırlatma verisinin planlayıcıya ve teslim hattına bakan yüzü: tabloları `:domain` modellerine çevirir.
 * Bozuk satır (çözülemeyen tekrar kuralı, saat ya da dilim) planı çökertmez; atlanır ve günlüğe yazılır.
 */
class ReminderStore(private val db: ToparlaDatabase) : ReminderRepository {
    override suspend fun activeDefinitions(): List<ReminderDef> = db.reminders().activeReminders().mapNotNull { it.toDef() }

    override suspend fun scheduled(): List<ScheduledAlarm> =
        db.scheduledAlarms().all().map { ScheduledAlarm(it.key, Instant.ofEpochMilli(it.fireAt)) }

    override suspend fun info(reminderId: String): ReminderInfo? =
        db.reminders().byId(reminderId)?.takeIf { it.deletedAt == null }?.let { ReminderInfo(it.id, it.klass, it.title, it.body, it.persistent) }

    override suspend fun removeScheduled(key: String) {
        db.scheduledAlarms().deleteByKeys(listOf(key))
    }

    override suspend fun firedKeys(keys: List<String>): Set<String> =
        if (keys.isEmpty()) emptySet() else db.deliveryLog().keysWithEvent(keys, DeliveryEvent.FIRED).toSet()

    override suspend fun occurrence(key: String): OccurrenceRecord? = db.occurrences().byKey(key)?.toRecord()

    override suspend fun saveOccurrence(record: OccurrenceRecord) {
        db.occurrences().upsert(
            ReminderOccurrenceEntity(
                key = record.key,
                reminderId = record.reminderId,
                plannedAt = record.plannedAt.toEpochMilli(),
                state = record.state,
                deliveredAt = record.deliveredAt?.toEpochMilli(),
                resolvedAt = record.resolvedAt?.toEpochMilli(),
                ladderStep = record.ladderStepsDone,
                snoozeCount = record.snoozeCount,
                lastAskedAt = record.lastAskedAt?.toEpochMilli(),
                asksDone = record.asksDone,
            ),
        )
    }

    /** Teslim edilmiş, yanıt bekleyen teslimler (merdiven ve ısrarlı takip bunlar için sürer). */
    override suspend fun openOccurrences(): List<OccurrenceRecord> =
        db.occurrences().inStates(listOf(OccurrenceState.DELIVERED, OccurrenceState.SEEN)).map { it.toRecord() }

    override suspend fun pendingSnoozes(): List<OccurrenceRecord> =
        db.occurrences().inStates(listOf(OccurrenceState.PLANNED)).map { it.toRecord() }
            .filter { AlarmKey.parse(it.key).kind == AlarmKeyKind.SNOOZE }

    fun observeWaiting(): Flow<List<WaitingRow>> = db.reminders().observeWaiting()

    fun observeUpcoming(): Flow<List<UpcomingRow>> = db.reminders().observeUpcoming()

    /** Hatırlatma Sağlığı ekranının iki göstergesi: kurulu alarm sayısı ve son çalan hatırlatmanın anı. */
    suspend fun deliveryStats(): DeliveryStats =
        DeliveryStats(db.scheduledAlarms().count(), db.deliveryLog().lastTs(DeliveryEvent.FIRED)?.let(Instant::ofEpochMilli))

    /** Tanımı kaydeder (yeni ya da güncelleme). Planlama ayrıca tetiklenir. */
    suspend fun saveReminder(reminder: ReminderEntity) = db.reminders().upsert(reminder)

    suspend fun deleteReminder(id: String, at: Instant) = db.reminders().softDelete(id, at.toEpochMilli())

    private fun ReminderOccurrenceEntity.toRecord() = OccurrenceRecord(
        key = key,
        reminderId = reminderId,
        plannedAt = Instant.ofEpochMilli(plannedAt),
        state = state,
        deliveredAt = deliveredAt?.let(Instant::ofEpochMilli),
        resolvedAt = resolvedAt?.let(Instant::ofEpochMilli),
        ladderStepsDone = ladderStep,
        snoozeCount = snoozeCount,
        lastAskedAt = lastAskedAt?.let(Instant::ofEpochMilli),
        asksDone = asksDone,
    )

    /** Planın sonucunu tabloya yansıtır: kurulanlar yazılır, iptal edilenler silinir. */
    override suspend fun applyPlan(scheduled: List<PlannedAlarm>, cancelledKeys: List<String>) {
        if (cancelledKeys.isNotEmpty()) db.scheduledAlarms().deleteByKeys(cancelledKeys)
        if (scheduled.isNotEmpty()) {
            db.scheduledAlarms().upsertAll(
                scheduled.map {
                    ScheduledAlarmEntity(it.key, it.reminderId, requestCodeOf(it.key), it.fireAt.toEpochMilli(), kindOf(it.key), it.api)
                },
            )
        }
    }

    /**
     * Ateşlenme kaydı. Aynı teslim ikinci kez gelirse (çift yayın, yeniden başlatma) false döner
     * ve çağıran bildirimi yeniden göstermez (çift teslim engeli).
     */
    override suspend fun recordFired(key: String, at: Instant): Boolean =
        db.deliveryLog().insert(DeliveryLogEntity(key = key, ts = at.toEpochMilli(), event = DeliveryEvent.FIRED, dedupeKey = "$key|${DeliveryEvent.FIRED}")) != -1L

    /** Motorun olay adı tablo sözlüğünde yoksa kayıt kaybolmaz: ACTION olarak, adı ayrıntıda saklanır. */
    override suspend fun log(key: String, at: Instant, event: String, detail: String?) {
        val known = DeliveryEvent.entries.firstOrNull { it.name == event }
        val text = if (known == null) listOfNotNull(event, detail).joinToString(": ") else detail
        db.deliveryLog().insert(DeliveryLogEntity(key = key, ts = at.toEpochMilli(), event = known ?: DeliveryEvent.ACTION, detail = text))
    }

    private fun ReminderEntity.toDef(): ReminderDef? {
        val rule = RecurrenceCodec.decode(recurrence)
        if (rule == null) {
            Timber.w("Hatırlatma %s atlandı: tekrar kuralı çözülemedi", id)
            return null
        }
        return try {
            ReminderDef(id, klass, LocalDateTime.parse(startLocal), ZoneId.of(zoneId), rule, active)
        } catch (e: DateTimeParseException) {
            Timber.w(e, "Hatırlatma %s atlandı: başlangıç saati çözülemedi", id)
            null
        } catch (e: ZoneRulesException) {
            Timber.w(e, "Hatırlatma %s atlandı: saat dilimi çözülemedi", id)
            null
        }
    }

    companion object {
        /** `PendingIntent` isteği için kararlı sayı: aynı anahtar her zaman aynı kodu verir (idempotans). */
        fun requestCodeOf(key: String): Int = key.hashCode()

        fun kindOf(key: String): AlarmKind = when {
            LADDER.containsMatchIn(key) -> AlarmKind.LADDER
            FOLLOW_UP.containsMatchIn(key) -> AlarmKind.FOLLOW_UP
            SNOOZE.containsMatchIn(key) -> AlarmKind.SNOOZE
            else -> AlarmKind.MAIN
        }

        private val LADDER = Regex("#l\\d+$")
        private val FOLLOW_UP = Regex("#f\\d+$")
        private val SNOOZE = Regex("#s\\d+$")
    }
}
