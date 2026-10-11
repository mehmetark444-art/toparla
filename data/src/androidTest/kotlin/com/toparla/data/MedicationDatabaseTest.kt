package com.toparla.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.toparla.data.db.MedicationStore
import com.toparla.data.db.OwnerType
import com.toparla.data.db.ReminderStore
import com.toparla.data.db.ToparlaDatabase
import com.toparla.domain.medication.DoseStatus
import com.toparla.domain.medication.Medication
import com.toparla.domain.medication.MedicationLog
import com.toparla.domain.medication.MedicationSchedule
import com.toparla.domain.reminder.MedicationRole
import com.toparla.domain.reminder.OccurrenceRecord
import com.toparla.domain.reminder.OccurrenceState
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderPlanner
import com.toparla.domain.reminder.SnoozePolicy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** F2.39–F2.41: ilaç tabloları, şema 1 → 2 geçişi, hatırlatma eşitlemesi ve modül anahtarı; gerçek cihazda. */
@RunWith(AndroidJUnit4::class)
class MedicationDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("Europe/Istanbul")
    private lateinit var db: ToparlaDatabase
    private var enabled = true

    @get:Rule
    val migrations = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = ToparlaDatabase::class,
        driver = BundledSQLiteDriver(),
        file = context.getDatabasePath(MIGRATION_DB),
    )

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder<ToparlaDatabase>(context).setDriver(BundledSQLiteDriver()).build()
    }

    @After
    fun close() = db.close()

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun med(times: List<LocalTime> = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))) = Medication(
        id = "m1", name = "Örnek", doseText = "1 tablet", times = times, startDate = LocalDate.of(2026, 10, 11), zoneId = zone, stock = 10,
    )

    @Test
    fun surum1den2yeGecisVeriyiKorurVeIlacTablolariniKurar() = runTest {
        migrations.createDatabase(1).apply {
            execSQL(
                "INSERT INTO Reminder (id, ownerType, ownerId, klass, title, body, startLocal, zoneId, recurrence, active, persistent, " +
                    "createdBy, createdAt, updatedAt, deletedAt) VALUES ('r1', 'CUSTOM', NULL, 'CRITICAL', 't', '', '2026-10-11T09:00', " +
                    "'Europe/Istanbul', 'ONCE', 1, 0, 'USER', 1, 1, NULL)",
            )
            close()
        }

        val migrated = migrations.runMigrationsAndValidate(2, emptyList())

        val kept = migrated.prepare("SELECT COUNT(*) FROM Reminder WHERE id = 'r1'").use { it.step(); it.getLong(0) }
        val medications = migrated.prepare("SELECT COUNT(*) FROM Medication").use { it.step(); it.getLong(0) }
        migrated.close()
        assertEquals(1L, kept)
        assertEquals(0L, medications)
    }

    @Test
    fun ilacKaydedilinceHerSaatIcinKritikHatirlatmaYazilirSaatKalkincaSilinir() = runTest {
        val store = MedicationStore(db)
        store.save(med(), at("2026-10-11T07:00"))

        val reminders = db.reminders().forOwner(OwnerType.MED, "m1")
        assertEquals(2, reminders.size)
        assertTrue(reminders.all { it.klass == ReminderClass.CRITICAL && it.title == "Örnek" && it.body == "1 tablet" })

        store.save(med(times = listOf(LocalTime.of(8, 0))), at("2026-10-11T07:05"))

        assertEquals(listOf(MedicationSchedule.doseReminderId("m1", LocalTime.of(8, 0))), db.reminders().forOwner(OwnerType.MED, "m1").map { it.id })
        assertEquals(med(times = listOf(LocalTime.of(8, 0))), store.medication("m1"))
    }

    @Test
    fun modulKapaliykenIlacHatirlatmasiMotoraGorunmezAcilincaGorunur() = runTest {
        MedicationStore(db).save(med(), at("2026-10-11T07:00"))
        val reminders = ReminderStore(db) { enabled }
        val doseId = MedicationSchedule.doseReminderId("m1", LocalTime.of(8, 0))

        enabled = false
        assertTrue(reminders.activeDefinitions().none { it.id == doseId })
        assertNull(reminders.info(doseId))

        enabled = true
        assertTrue(reminders.activeDefinitions().any { it.id == doseId })
        assertEquals(MedicationRole.DOSE, reminders.info(doseId)?.medication?.role)
        // Varsayılan kurucu anahtarın varsayılanını (kapalı) izler.
        assertNull(ReminderStore(db).info(doseId))
    }

    @Test
    fun kayitAyniDozaYazilirSonAlinanAnOkunurStokGuncellenir() = runTest {
        val store = MedicationStore(db)
        store.save(med(), at("2026-10-11T07:00"))
        val dose = at("2026-10-11T08:00")

        store.saveLog(MedicationLog("m1", dose, at("2026-10-11T08:01"), DoseStatus.SKIPPED))
        assertNull(store.lastTakenAt("m1"))
        store.saveLog(MedicationLog("m1", dose, at("2026-10-11T08:05"), DoseStatus.TAKEN))
        store.updateStock("m1", 9, at("2026-10-11T08:05"))

        assertEquals(DoseStatus.TAKEN, store.log("m1", dose)?.status)
        assertEquals(at("2026-10-11T08:05"), store.lastTakenAt("m1"))
        assertEquals(9, store.medication("m1")?.stock)
        assertEquals(1, store.logsBetween(at("2026-10-11T00:00"), at("2026-10-12T00:00")).size)
    }

    @Test
    fun ertelenenDozTekDozSayilirAnahtariSonErtelemedir() = runTest {
        val store = MedicationStore(db)
        store.save(med(), at("2026-10-11T07:00"))
        val reminders = ReminderStore(db) { true }
        val doseId = MedicationSchedule.doseReminderId("m1", LocalTime.of(8, 0))
        val key = ReminderPlanner.keyOf(doseId, at("2026-10-11T08:00"))
        reminders.saveOccurrence(OccurrenceRecord(key, doseId, at("2026-10-11T08:00"), OccurrenceState.SNOOZED, deliveredAt = at("2026-10-11T08:00")))
        reminders.saveOccurrence(
            OccurrenceRecord(SnoozePolicy.snoozedKey(key, 1), doseId, at("2026-10-11T08:15"), OccurrenceState.DELIVERED, deliveredAt = at("2026-10-11T08:15"), snoozeCount = 1),
        )

        val dose = store.plannedDoses(at("2026-10-11T00:00")).single()

        assertEquals(at("2026-10-11T08:00"), dose.plannedAt)
        assertEquals(SnoozePolicy.snoozedKey(key, 1), dose.occurrenceKey)
    }

    @Test
    fun stokEsigindeEczaneHatirlatmasiOnemliSiniftaTekSeferlikKurulur() = runTest {
        val store = MedicationStore(db)
        store.save(med(), at("2026-10-11T07:00"))

        store.requestRestock("m1", at("2026-10-11T08:02"))

        val restock = db.reminders().forOwner(OwnerType.MED, "m1").single { MedicationSchedule.parse(it.id)?.role == MedicationRole.RESTOCK }
        assertEquals(ReminderClass.IMPORTANT, restock.klass)
        assertEquals("ONCE", restock.recurrence)
    }

    @Test
    fun ilacSilinceHatirlatmalariSilinirKayitlariKalir() = runTest {
        val store = MedicationStore(db)
        store.save(med(), at("2026-10-11T07:00"))
        store.saveLog(MedicationLog("m1", at("2026-10-11T08:00"), at("2026-10-11T08:01"), DoseStatus.TAKEN))

        store.delete("m1", at("2026-10-11T09:00"))

        assertNull(store.medication("m1"))
        assertTrue(db.reminders().forOwner(OwnerType.MED, "m1").isEmpty())
        assertEquals("Örnek", store.nameOf("m1"))
        assertEquals(1, store.logsBetween(at("2026-10-11T00:00"), at("2026-10-12T00:00")).size)
    }

    private companion object {
        const val MIGRATION_DB = "migration-test"
    }
}
