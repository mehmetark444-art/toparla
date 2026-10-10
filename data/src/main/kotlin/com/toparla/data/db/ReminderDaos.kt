package com.toparla.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.toparla.domain.reminder.OccurrenceState
import com.toparla.domain.reminder.ReminderClass
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("SELECT * FROM Reminder WHERE id = :id")
    suspend fun byId(id: String): ReminderEntity?

    /** Planlayıcının girdisi: açık ve silinmemiş tanımlar. */
    @Query("SELECT * FROM Reminder WHERE active = 1 AND deletedAt IS NULL")
    suspend fun activeReminders(): List<ReminderEntity>

    /** Yumuşak silme (blueprint H: 30 gün çöp). */
    @Query("UPDATE Reminder SET deletedAt = :now, active = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM Reminder WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeleted(before: Long): Int

    /** Yanıt bekleyen teslimler, en eskisi üstte (Plan ekranı "Seni bekleyenler"). */
    @Query(
        "SELECT o.`key` AS occurrenceKey, r.title AS title, r.klass AS klass, r.persistent AS persistent, o.deliveredAt AS since " +
            "FROM ReminderOccurrence o JOIN Reminder r ON r.id = o.reminderId " +
            "WHERE o.state IN ('DELIVERED', 'SEEN') AND r.deletedAt IS NULL ORDER BY o.deliveredAt",
    )
    fun observeWaiting(): Flow<List<WaitingRow>>

    /** Kurulu sıradaki teslimler (ana teslim ve ertelenenler), en yakını üstte (Plan ekranı "Sıradakiler"). */
    @Query(
        "SELECT s.`key` AS alarmKey, r.id AS reminderId, r.title AS title, r.klass AS klass, r.recurrence AS recurrence, " +
            "s.fireAt AS fireAt, s.kind AS kind " +
            "FROM ScheduledAlarm s JOIN Reminder r ON r.id = s.reminderId " +
            "WHERE s.kind IN ('MAIN', 'SNOOZE') AND r.deletedAt IS NULL ORDER BY s.fireAt",
    )
    fun observeUpcoming(): Flow<List<UpcomingRow>>
}

data class WaitingRow(val occurrenceKey: String, val title: String, val klass: ReminderClass, val persistent: Boolean, val since: Long?)

data class UpcomingRow(
    val alarmKey: String,
    val reminderId: String,
    val title: String,
    val klass: ReminderClass,
    val recurrence: String,
    val fireAt: Long,
    val kind: AlarmKind,
)

@Dao
interface ReminderOccurrenceDao {
    @Upsert
    suspend fun upsert(occurrence: ReminderOccurrenceEntity)

    /** Aynı anahtar zaten varsa dokunmaz; -1 döner. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(occurrence: ReminderOccurrenceEntity): Long

    @Query("SELECT * FROM ReminderOccurrence WHERE `key` = :key")
    suspend fun byKey(key: String): ReminderOccurrenceEntity?

    /** Teslim edilmiş, yanıt bekleyenler: merdiven ve ısrarlı takip bunlar için sürer. */
    @Query("SELECT * FROM ReminderOccurrence WHERE state IN (:states)")
    suspend fun inStates(states: List<OccurrenceState>): List<ReminderOccurrenceEntity>
}

@Dao
interface ScheduledAlarmDao {
    @Upsert
    suspend fun upsertAll(alarms: List<ScheduledAlarmEntity>)

    @Query("SELECT * FROM ScheduledAlarm ORDER BY fireAt")
    suspend fun all(): List<ScheduledAlarmEntity>

    @Query("DELETE FROM ScheduledAlarm WHERE `key` IN (:keys)")
    suspend fun deleteByKeys(keys: List<String>): Int

    @Query("SELECT COUNT(*) FROM ScheduledAlarm")
    suspend fun count(): Int
}

@Dao
interface DeliveryLogDao {
    /** `dedupeKey` çakışırsa satır eklenmez ve -1 döner. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: DeliveryLogEntity): Long

    @Query("SELECT * FROM DeliveryLog WHERE `key` = :key ORDER BY ts, id")
    suspend fun forKey(key: String): List<DeliveryLogEntity>

    @Query("SELECT DISTINCT `key` FROM DeliveryLog WHERE event = :event AND `key` IN (:keys)")
    suspend fun keysWithEvent(keys: List<String>, event: DeliveryEvent): List<String>

    /** Son teslimin anı (Hatırlatma Sağlığı "Son hatırlatma"). */
    @Query("SELECT MAX(ts) FROM DeliveryLog WHERE event = :event")
    suspend fun lastTs(event: DeliveryEvent): Long?
}
