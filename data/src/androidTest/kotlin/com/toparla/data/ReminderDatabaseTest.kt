package com.toparla.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toparla.data.db.AlarmKind
import com.toparla.data.db.CreatedBy
import com.toparla.data.db.DeliveryEvent
import com.toparla.data.db.OwnerType
import com.toparla.data.db.PreMigrationBackup
import com.toparla.data.db.ReminderEntity
import com.toparla.data.db.ReminderOccurrenceEntity
import com.toparla.data.db.ReminderStore
import com.toparla.data.db.ToparlaDatabase
import com.toparla.domain.reminder.AlarmApi
import com.toparla.domain.reminder.OccurrenceState
import com.toparla.domain.reminder.Recurrence
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderPlanner
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** F2.11 ve F2.12: tablolar, DAO'lar, planlayıcıyla uçtan uca akış ve migration öncesi kopya; gerçek cihazda. */
@RunWith(AndroidJUnit4::class)
class ReminderDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("Europe/Istanbul")
    private lateinit var db: ToparlaDatabase
    private lateinit var store: ReminderStore

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder<ToparlaDatabase>(context).setDriver(BundledSQLiteDriver()).build()
        store = ReminderStore(db)
    }

    @After
    fun close() = db.close()

    private fun reminder(id: String, klass: ReminderClass = ReminderClass.CRITICAL, recurrence: String = "DAILY", persistent: Boolean = false) =
        ReminderEntity(
            id = id, ownerType = OwnerType.CUSTOM, ownerId = null, klass = klass, title = "t", body = "",
            startLocal = "2026-10-08T09:00", zoneId = zone.id, recurrence = recurrence, active = true,
            persistent = persistent, createdBy = CreatedBy.USER, createdAt = 1, updatedAt = 1,
        )

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    @Test
    fun tanimYazilirOkunurVeAlanTanimaCevrilir() = runTest {
        db.reminders().upsert(reminder("r1", recurrence = "WEEKLY:MONDAY,FRIDAY"))

        val def = store.activeDefinitions().single()

        assertEquals("r1", def.id)
        assertEquals(ReminderClass.CRITICAL, def.klass)
        assertEquals(LocalDateTime.parse("2026-10-08T09:00"), def.startLocal)
        assertTrue(def.recurrence is Recurrence.Weekly)
    }

    @Test
    fun bozukSatirPlaniCokertmezAtlanir() = runTest {
        db.reminders().upsert(reminder("iyi"))
        db.reminders().upsert(reminder("bozuk-kural", recurrence = "YILDA_BIR"))
        db.reminders().upsert(reminder("bozuk-saat").copy(startLocal = "dün"))
        db.reminders().upsert(reminder("bozuk-dilim").copy(zoneId = "Mars/Olympus"))

        assertEquals(listOf("iyi"), store.activeDefinitions().map { it.id })
    }

    @Test
    fun yumusakSilinenTanimPlanaGirmezVeSuresiDolancaTemizlenir() = runTest {
        db.reminders().upsert(reminder("r1"))
        db.reminders().softDelete("r1", now = 1000)

        assertTrue(store.activeDefinitions().isEmpty())
        assertNotNull(db.reminders().byId("r1"))
        assertEquals(0, db.reminders().purgeDeleted(before = 1000))
        assertEquals(1, db.reminders().purgeDeleted(before = 1001))
        assertNull(db.reminders().byId("r1"))
    }

    @Test
    fun planUygulaninaTabloyaYansirIkinciPlanBosDoner() = runTest {
        db.reminders().upsert(reminder("r1"))
        val now = at("2026-10-08T08:00")

        val first = ReminderPlanner.plan(now, store.activeDefinitions(), store.scheduled())
        store.applyPlan(first.toSchedule, first.toCancel)
        val second = ReminderPlanner.plan(now, store.activeDefinitions(), store.scheduled())

        assertEquals(2, db.scheduledAlarms().count())
        assertTrue(second.isEmpty)
        val row = db.scheduledAlarms().all().first()
        assertEquals(AlarmKind.MAIN, row.kind)
        assertEquals(AlarmApi.ALARM_CLOCK, row.api)
        assertEquals(ReminderStore.requestCodeOf(row.key), row.requestCode)
    }

    @Test
    fun tanimSilinincePlanAlarmlariniTablodanDaKaldirir() = runTest {
        db.reminders().upsert(reminder("r1"))
        val now = at("2026-10-08T08:00")
        val first = ReminderPlanner.plan(now, store.activeDefinitions(), store.scheduled())
        store.applyPlan(first.toSchedule, first.toCancel)

        db.reminders().softDelete("r1", now.toEpochMilli())
        val after = ReminderPlanner.plan(now, store.activeDefinitions(), store.scheduled())
        store.applyPlan(after.toSchedule, after.toCancel)

        assertEquals(2, after.toCancel.size)
        assertEquals(0, db.scheduledAlarms().count())
    }

    @Test
    fun ayniTesliminIkinciAteslenmeKaydiDuser() = runTest {
        val key = "r1@1"

        assertTrue(store.recordFired(key, Instant.ofEpochMilli(10)))
        assertFalse(store.recordFired(key, Instant.ofEpochMilli(20)))
        // Tekil olması gerekmeyen olaylar serbestçe yazılır.
        store.log(key, Instant.ofEpochMilli(30), DeliveryEvent.TAPPED.name, null)
        store.log(key, Instant.ofEpochMilli(40), "BILINMEYEN_OLAY", "ayrinti")

        val events = db.deliveryLog().forKey(key).map { it.event }
        assertEquals(listOf(DeliveryEvent.FIRED, DeliveryEvent.TAPPED, DeliveryEvent.ACTION), events)
        assertEquals("BILINMEYEN_OLAY: ayrinti", db.deliveryLog().forKey(key).last().detail)
        assertEquals(listOf(key), db.deliveryLog().keysWithEvent(listOf(key, "baska"), DeliveryEvent.FIRED))
    }

    @Test
    fun yanitBekleyenTeslimPlanlayiciyaIsrarliTakipBilgisiyleGelir() = runTest {
        db.reminders().upsert(reminder("r1", klass = ReminderClass.NORMAL, persistent = true))
        db.occurrences().upsert(
            ReminderOccurrenceEntity(
                key = "r1@1", reminderId = "r1", plannedAt = 100, state = OccurrenceState.DELIVERED,
                deliveredAt = 100, ladderStep = 1, lastAskedAt = 500, asksDone = 2,
            ),
        )
        db.occurrences().upsert(ReminderOccurrenceEntity("r1@2", "r1", 200, OccurrenceState.DONE, deliveredAt = 200))
        db.occurrences().upsert(ReminderOccurrenceEntity("r1@3", "r1", 300, OccurrenceState.PLANNED))
        db.occurrences().upsert(ReminderOccurrenceEntity("r1@1#s1", "r1", 400, OccurrenceState.PLANNED, snoozeCount = 1))

        val flight = store.openOccurrences().single()

        assertEquals("r1@1", flight.key)
        assertTrue(store.info("r1")?.persistent == true)
        assertEquals(1, store.pendingSnoozes().size)
        assertEquals(Instant.ofEpochMilli(500), flight.lastAskedAt)
        assertEquals(2, flight.asksDone)
    }

    @Test
    fun ayniAnahtarliTeslimIkinciKezEklenmez() = runTest {
        val first = ReminderOccurrenceEntity("r1@1", "r1", 100, OccurrenceState.DELIVERED, deliveredAt = 100)

        assertTrue(db.occurrences().insertIfAbsent(first) != -1L)
        assertEquals(-1L, db.occurrences().insertIfAbsent(first.copy(state = OccurrenceState.PLANNED)))
        assertEquals(OccurrenceState.DELIVERED, db.occurrences().byKey("r1@1")?.state)
    }

    @Test
    fun anahtarEkiAlarmTurunuBelirler() {
        assertEquals(AlarmKind.MAIN, ReminderStore.kindOf("r1@1"))
        assertEquals(AlarmKind.LADDER, ReminderStore.kindOf("r1@1#l2"))
        assertEquals(AlarmKind.FOLLOW_UP, ReminderStore.kindOf("r1@1#f0"))
        assertEquals(AlarmKind.SNOOZE, ReminderStore.kindOf("r1@1#s1"))
    }

    @Test
    fun surumYukselirkenDosyaninKopyasiAlinirVeGeriKonabilir() {
        val dir = File(context.cacheDir, "migration-test").apply {
            deleteRecursively()
            mkdirs()
        }
        val dbFile = File(dir, "eski.db")
        val driver = BundledSQLiteDriver()
        driver.open(dbFile.path).apply {
            prepare("CREATE TABLE t(v TEXT)").use { it.step() }
            prepare("INSERT INTO t VALUES('ilk')").use { it.step() }
            prepare("PRAGMA user_version = 1").use { it.step() }
            close()
        }

        assertEquals(1, PreMigrationBackup.storedVersion(dbFile))
        // Aynı sürümde ve yeni kurulumda kopya alınmaz.
        assertNull(PreMigrationBackup.backupIfUpgrading(dbFile, File(dir, "yedek"), targetVersion = 1))
        assertNull(PreMigrationBackup.backupIfUpgrading(File(dir, "yok.db"), File(dir, "yedek"), targetVersion = 2))

        val backup = PreMigrationBackup.backupIfUpgrading(dbFile, File(dir, "yedek"), targetVersion = 2)
        assertEquals("pre-migration-1.db", backup?.name)

        // "Başarısız migration": dosya bozulur, sonra kopyadan geri dönülür.
        driver.open(dbFile.path).apply {
            prepare("DELETE FROM t").use { it.step() }
            close()
        }
        PreMigrationBackup.restore(requireNotNull(backup), dbFile)
        val restored = driver.open(dbFile.path)
        val value = restored.prepare("SELECT v FROM t").use {
            it.step()
            it.getText(0)
        }
        restored.close()
        assertEquals("ilk", value)
    }
}
