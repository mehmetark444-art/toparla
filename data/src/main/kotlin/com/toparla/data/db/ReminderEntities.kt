package com.toparla.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.toparla.domain.reminder.AlarmApi
import com.toparla.domain.reminder.OccurrenceState
import com.toparla.domain.reminder.ReminderClass

/** Hatırlatmanın sahibi (blueprint H2). */
enum class OwnerType { TASK, MED, ROUTINE, EVENT, HABIT, COMMITMENT, CUSTOM }

enum class CreatedBy { USER, AGENT, SYSTEM }

/** Sistem alarmının türü; anahtar ekleriyle eşleşir (`#l`, `#f`, `#s`). */
enum class AlarmKind { MAIN, LADDER, FOLLOW_UP, SNOOZE }

enum class DeliveryEvent { SCHEDULED, FIRED, POSTED, BLOCKED, TAPPED, ACTION, MISSED_DETECTED, WATCHDOG_REARMED }

/**
 * Hatırlatma tanımı (v3 §9.2, blueprint H2). Zamanlar UTC epoch ms; yerel başlangıç ISO metin + `zoneId`.
 * `recurrence` alanı [com.toparla.domain.reminder.RecurrenceCodec] biçimindedir (v3'teki `recurrenceJson`'un karşılığı).
 * `persistent`: ısrarlı takip (karar 0003).
 */
@Entity(tableName = "Reminder", indices = [Index("active", "klass")])
data class ReminderEntity(
    @PrimaryKey val id: String,
    val ownerType: OwnerType,
    val ownerId: String?,
    val klass: ReminderClass,
    val title: String,
    val body: String,
    val startLocal: String,
    val zoneId: String,
    val recurrence: String,
    val active: Boolean,
    val persistent: Boolean,
    val createdBy: CreatedBy,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** Tek bir teslim. `key` = reminderId + plannedAt (idempotans anahtarı). */
@Entity(tableName = "ReminderOccurrence", indices = [Index("plannedAt"), Index("resolvedAt"), Index("reminderId")])
data class ReminderOccurrenceEntity(
    @PrimaryKey val key: String,
    val reminderId: String,
    val plannedAt: Long,
    val state: OccurrenceState,
    val deliveredAt: Long? = null,
    val seenAt: Long? = null,
    val resolvedAt: Long? = null,
    /** t0 dahil işlenmiş merdiven basamağı sayısı. */
    val ladderStep: Int = 0,
    val attempts: Int = 0,
    val snoozeCount: Int = 0,
    val lastAskedAt: Long? = null,
    val asksDone: Int = 0,
)

/** Sistemde kurulu alarmın kaydı; planlayıcı bununla fark alır. */
@Entity(tableName = "ScheduledAlarm", indices = [Index("fireAt"), Index("reminderId")])
data class ScheduledAlarmEntity(
    @PrimaryKey val key: String,
    val reminderId: String,
    val requestCode: Int,
    val fireAt: Long,
    val kind: AlarmKind,
    val api: AlarmApi,
)

/**
 * Teslim günlüğü. `dedupeKey` yalnız tekil olması gereken olaylarda doludur (ör. `key|FIRED`):
 * aynı teslimin ikinci FIRED kaydı `INSERT OR IGNORE` ile düşer (çift teslim engeli, Bölüm I).
 */
@Entity(tableName = "DeliveryLog", indices = [Index("key"), Index("ts"), Index("dedupeKey", unique = true)])
data class DeliveryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val ts: Long,
    val event: DeliveryEvent,
    val detail: String? = null,
    val dedupeKey: String? = null,
)
