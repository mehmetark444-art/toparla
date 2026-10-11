package com.toparla.data.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.toparla.domain.medication.DoseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Upsert
    suspend fun upsert(medication: MedicationEntity)

    @Query("SELECT * FROM Medication WHERE id = :id AND deletedAt IS NULL")
    suspend fun byId(id: String): MedicationEntity?

    /** Silinmiş ilaç da okunur: geçmiş ve dışa aktarım adını gösterebilsin. */
    @Query("SELECT * FROM Medication WHERE id = :id")
    suspend fun byIdIncludingDeleted(id: String): MedicationEntity?

    @Query("SELECT * FROM Medication WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM Medication WHERE deletedAt IS NULL")
    suspend fun all(): List<MedicationEntity>

    @Query("UPDATE Medication SET stockCount = :stock, updatedAt = :now WHERE id = :id")
    suspend fun updateStock(id: String, stock: Int, now: Long)

    @Query("UPDATE Medication SET deletedAt = :now, active = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface MedicationLogDao {
    @Upsert
    suspend fun upsert(log: MedicationLogEntity)

    @Query("SELECT * FROM MedicationLog WHERE medId = :medId AND plannedAt = :plannedAt")
    suspend fun byDose(medId: String, plannedAt: Long): MedicationLogEntity?

    /** Son "alındı" kaydı (çift doz koruması). */
    @Query("SELECT MAX(loggedAt) FROM MedicationLog WHERE medId = :medId AND status IN (:counted)")
    suspend fun lastTakenAt(medId: String, counted: List<DoseStatus>): Long?

    @Query("SELECT * FROM MedicationLog WHERE plannedAt >= :from AND plannedAt < :to ORDER BY plannedAt")
    suspend fun between(from: Long, to: Long): List<MedicationLogEntity>

    @Query("SELECT * FROM MedicationLog WHERE plannedAt >= :from ORDER BY plannedAt")
    fun observeSince(from: Long): Flow<List<MedicationLogEntity>>
}
