package com.toparla.data.db

import com.toparla.domain.medication.DoseStatus
import com.toparla.domain.medication.Medication
import com.toparla.domain.medication.MedicationLog
import com.toparla.domain.medication.MedicationRepository
import com.toparla.domain.medication.MedicationSchedule
import com.toparla.domain.medication.PlannedDose
import com.toparla.domain.reminder.AlarmKey
import com.toparla.domain.reminder.MedicationRole
import com.toparla.domain.reminder.RecurrenceCodec
import com.toparla.domain.reminder.Recurrence
import com.toparla.domain.reminder.ReminderClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * İlaç verisi (M9). Tanım kaydedilince her saat için bir Kritik hatırlatma tanımı yazılır ya da güncellenir
 * ([MedicationSchedule]); artık olmayan saatin hatırlatması silinir. Planlama ayrıca tetiklenir (çağıran motoru
 * `replan` ile uyarır). Bozuk satır atlanır ve günlüğe yazılır.
 */
class MedicationStore(private val db: ToparlaDatabase) : MedicationRepository {
    fun observeMedications(): Flow<List<Medication>> = db.medications().observeAll().map { rows -> rows.mapNotNull { it.toModel() } }

    suspend fun medications(): List<Medication> = db.medications().all().mapNotNull { it.toModel() }

    override suspend fun medication(id: String): Medication? = db.medications().byId(id)?.toModel()

    /** Silinmiş ilacın adı da (geçmiş ve dışa aktarım için). */
    suspend fun nameOf(id: String): String? = db.medications().byIdIncludingDeleted(id)?.name

    override suspend fun lastTakenAt(medicationId: String): Instant? =
        db.medicationLogs().lastTakenAt(medicationId, DoseStatus.entries.filter { it.counted })?.let(Instant::ofEpochMilli)

    override suspend fun log(medicationId: String, plannedAt: Instant): MedicationLog? =
        db.medicationLogs().byDose(medicationId, plannedAt.toEpochMilli())?.toModel()

    override suspend fun saveLog(log: MedicationLog) {
        db.medicationLogs().upsert(
            MedicationLogEntity(log.medicationId, log.plannedAt.toEpochMilli(), log.loggedAt.toEpochMilli(), log.status, log.note),
        )
    }

    override suspend fun updateStock(medicationId: String, stock: Int, at: Instant) {
        db.medications().updateStock(medicationId, stock, at.toEpochMilli())
    }

    /**
     * Stok eşiği geçildi: tek seferlik, Önemli sınıf "Eczaneye uğra" hatırlatması. Başlığı bildirim katmanı koyar
     * (metin kaynaklardadır); burada ilacın adı gövdeye yazılır, "adı gizle" açıksa bildirim onu göstermez.
     */
    override suspend fun requestRestock(medicationId: String, at: Instant) {
        val med = db.medications().byId(medicationId) ?: return
        val zone = parseZone(med.zoneId) ?: return
        db.reminders().upsert(
            ReminderEntity(
                id = MedicationSchedule.restockReminderId(medicationId, at),
                ownerType = OwnerType.MED,
                ownerId = medicationId,
                klass = ReminderClass.IMPORTANT,
                title = med.name,
                body = "",
                startLocal = at.atZone(zone).toLocalDateTime().toString(),
                zoneId = zone.id,
                recurrence = RecurrenceCodec.encode(Recurrence.Once),
                active = true,
                persistent = false,
                createdBy = CreatedBy.SYSTEM,
                createdAt = at.toEpochMilli(),
                updatedAt = at.toEpochMilli(),
            ),
        )
    }

    /** Tanımı ve hatırlatmalarını kaydeder (yeni ya da düzenleme). Kayıtlar (geçmiş) değişmez. */
    suspend fun save(med: Medication, now: Instant) {
        val existing = db.medications().byIdIncludingDeleted(med.id)
        db.medications().upsert(med.toEntity(createdAt = existing?.createdAt ?: now.toEpochMilli(), updatedAt = now.toEpochMilli()))
        val wanted = MedicationSchedule.definitions(med)
        val wantedIds = wanted.map { it.id }.toSet()
        db.reminders().forOwner(OwnerType.MED, med.id)
            .filter { MedicationSchedule.parse(it.id)?.role == MedicationRole.DOSE && it.id !in wantedIds }
            .forEach { db.reminders().softDelete(it.id, now.toEpochMilli()) }
        wanted.forEach { def ->
            val previous = db.reminders().byId(def.id)
            db.reminders().upsert(
                ReminderEntity(
                    id = def.id,
                    ownerType = OwnerType.MED,
                    ownerId = med.id,
                    klass = def.klass,
                    title = med.name,
                    body = med.doseText,
                    startLocal = def.startLocal.toString(),
                    zoneId = def.zoneId.id,
                    recurrence = RecurrenceCodec.encode(def.recurrence),
                    active = def.active,
                    persistent = false,
                    createdBy = CreatedBy.USER,
                    createdAt = previous?.createdAt ?: now.toEpochMilli(),
                    updatedAt = now.toEpochMilli(),
                ),
            )
        }
    }

    /** Kullanıcı'nın isteğiyle: ilaç ve hatırlatmaları çöpe gider (yumuşak silme); kayıtları kalır. */
    suspend fun delete(medicationId: String, now: Instant) {
        db.medications().softDelete(medicationId, now.toEpochMilli())
        db.reminders().forOwner(OwnerType.MED, medicationId).forEach { db.reminders().softDelete(it.id, now.toEpochMilli()) }
    }

    /**
     * [since] sonrasında planlanmış, teslim edilmiş dozlar. Ertelenen doz tek doz sayılır: anahtarı, ilk planlanan anı
     * taşıyan son (en çok ertelenmiş) teslimdir; "Aldım" o teslime gider.
     */
    suspend fun plannedDoses(since: Instant): List<PlannedDose> = dosesOf(db.occurrences().medicationSince(since.toEpochMilli()))

    fun observePlannedDoses(since: Instant): Flow<List<PlannedDose>> =
        db.occurrences().observeMedicationSince(since.toEpochMilli()).map(::dosesOf)

    suspend fun logsBetween(from: Instant, to: Instant): List<MedicationLog> =
        db.medicationLogs().between(from.toEpochMilli(), to.toEpochMilli()).map { it.toModel() }

    fun observeLogsSince(since: Instant): Flow<List<MedicationLog>> =
        db.medicationLogs().observeSince(since.toEpochMilli()).map { rows -> rows.map { it.toModel() } }

    private fun dosesOf(occurrences: List<ReminderOccurrenceEntity>): List<PlannedDose> =
        occurrences.mapNotNull { o ->
            val medicationId = MedicationSchedule.parse(o.reminderId)?.medicationId ?: return@mapNotNull null
            val rootAt = AlarmKey.plannedAtOf(o.key) ?: return@mapNotNull null
            Triple(medicationId to rootAt, o.snoozeCount, o.key)
        }
            .groupBy { it.first }
            .map { (dose, versions) -> PlannedDose(dose.first, dose.second, versions.maxBy { it.second }.third) }

    private fun MedicationLogEntity.toModel() = MedicationLog(medId, Instant.ofEpochMilli(plannedAt), Instant.ofEpochMilli(loggedAt), status, note)

    private fun MedicationEntity.toModel(): Medication? = try {
        Medication(
            id = id,
            name = name,
            doseText = doseText,
            times = times.split(LIST_SEPARATOR).filter { it.isNotBlank() }.map(LocalTime::parse),
            days = days.split(LIST_SEPARATOR).filter { it.isNotBlank() }.map(DayOfWeek::valueOf).toSet(),
            startDate = LocalDate.parse(startDate),
            zoneId = ZoneId.of(zoneId),
            minGap = Duration.ofMinutes(minGapMin),
            stock = stockCount,
            unitsPerDose = unitsPerDose,
            lowStockAt = lowStockAt,
            notes = notes,
            hideName = hideName,
            active = active,
            routineId = routineId,
        )
    } catch (e: DateTimeException) {
        Timber.w(e, "İlaç %s atlandı: saat, tarih ya da saat dilimi çözülemedi", id)
        null
    } catch (e: IllegalArgumentException) {
        Timber.w(e, "İlaç %s atlandı: tanım geçersiz", id)
        null
    }

    private fun Medication.toEntity(createdAt: Long, updatedAt: Long) = MedicationEntity(
        id = id,
        name = name,
        doseText = doseText,
        times = times.joinToString(LIST_SEPARATOR) { it.toString() },
        days = days.sorted().joinToString(LIST_SEPARATOR) { it.name },
        startDate = startDate.toString(),
        zoneId = zoneId.id,
        minGapMin = minGap.toMinutes(),
        stockCount = stock,
        unitsPerDose = unitsPerDose,
        lowStockAt = lowStockAt,
        notes = notes,
        hideName = hideName,
        active = active,
        routineId = routineId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun parseZone(id: String): ZoneId? = try {
        ZoneId.of(id)
    } catch (e: DateTimeException) {
        Timber.w(e, "Saat dilimi çözülemedi: %s", id)
        null
    }

    private companion object {
        const val LIST_SEPARATOR = ","
    }
}
