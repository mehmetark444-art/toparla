package com.toparla.data.db

import com.toparla.domain.reminder.InFlightOccurrence
import com.toparla.domain.reminder.OccurrenceState
import com.toparla.domain.reminder.PlannedAlarm
import com.toparla.domain.reminder.RecurrenceCodec
import com.toparla.domain.reminder.ReminderDef
import com.toparla.domain.reminder.ScheduledAlarm
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.zone.ZoneRulesException

/**
 * Hatırlatma verisinin planlayıcıya ve teslim hattına bakan yüzü: tabloları `:domain` modellerine çevirir.
 * Bozuk satır (çözülemeyen tekrar kuralı, saat ya da dilim) planı çökertmez; atlanır ve günlüğe yazılır.
 */
class ReminderStore(private val db: ToparlaDatabase) {
    suspend fun activeDefinitions(): List<ReminderDef> = db.reminders().activeReminders().mapNotNull { it.toDef() }

    suspend fun scheduled(): List<ScheduledAlarm> =
        db.scheduledAlarms().all().map { ScheduledAlarm(it.key, Instant.ofEpochMilli(it.fireAt)) }

    /** Teslim edilmiş, yanıt bekleyen teslimler (merdiven ve ısrarlı takip bunlar için sürer). */
    suspend fun inFlight(): List<InFlightOccurrence> {
        val open = db.occurrences().inStates(listOf(OccurrenceState.DELIVERED, OccurrenceState.SEEN))
        return open.mapNotNull { o ->
            val reminder = db.reminders().byId(o.reminderId) ?: return@mapNotNull null
            val firedAt = Instant.ofEpochMilli(o.deliveredAt ?: return@mapNotNull null)
            InFlightOccurrence(
                key = o.key,
                reminderId = o.reminderId,
                klass = reminder.klass,
                firedAt = firedAt,
                ladderStepsDone = o.ladderStep.coerceAtLeast(1),
                persistent = reminder.persistent,
                lastAskedAt = o.lastAskedAt?.let(Instant::ofEpochMilli) ?: firedAt,
                asksDone = o.asksDone,
            )
        }
    }

    /** Planın sonucunu tabloya yansıtır: kurulanlar yazılır, iptal edilenler silinir. */
    suspend fun applyPlan(scheduled: List<PlannedAlarm>, cancelledKeys: List<String>) {
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
    suspend fun recordFired(key: String, at: Instant): Boolean =
        db.deliveryLog().insert(DeliveryLogEntity(key = key, ts = at.toEpochMilli(), event = DeliveryEvent.FIRED, dedupeKey = "$key|${DeliveryEvent.FIRED}")) != -1L

    suspend fun log(key: String, at: Instant, event: DeliveryEvent, detail: String? = null) {
        db.deliveryLog().insert(DeliveryLogEntity(key = key, ts = at.toEpochMilli(), event = event, detail = detail))
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
