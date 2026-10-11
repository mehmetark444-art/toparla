package com.toparla.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.toparla.domain.medication.DoseStatus

/**
 * İlaç tanımı (blueprint H2 `Medication`, v3 §9.2; M9). İlaç verisi Sarı'dır: cihazdan çıkmaz, buluta gitmez.
 * `times` "HH:mm" listesi (virgülle), `days` `DayOfWeek` adları (virgülle); çevirisi [MedicationStore]'dadır.
 * v3'teki `scheduleJson` yerine iki sade sütun: çizelge yalnız saatler ve günlerden oluşur.
 */
@Entity(tableName = "Medication", indices = [Index("active")])
data class MedicationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val doseText: String,
    val times: String,
    val days: String,
    val startDate: String,
    val zoneId: String,
    val minGapMin: Long,
    val stockCount: Int?,
    val unitsPerDose: Int,
    val lowStockAt: Int?,
    val notes: String,
    val hideName: Boolean,
    val active: Boolean,
    val routineId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * Dozun kaydı (v3 `MedicationLog`). Bir doz için tek satır: birincil anahtar (ilaç, planlanan an); düzeltme
 * aynı satırın yerine yazar. İlaç silinse de kayıt kalır (Kullanıcı'nın geçmişi; dışa aktarımda görünür).
 */
@Entity(tableName = "MedicationLog", primaryKeys = ["medId", "plannedAt"], indices = [Index("loggedAt")])
data class MedicationLogEntity(
    val medId: String,
    val plannedAt: Long,
    val loggedAt: Long,
    val status: DoseStatus,
    val note: String? = null,
)
