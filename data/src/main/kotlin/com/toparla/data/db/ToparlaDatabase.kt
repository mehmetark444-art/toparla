package com.toparla.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Tek veritabanı. Şema `data/schemas/` altına dışa aktarılır; yıkıcı migration yasaktır (blueprint H, B7).
 * Sürüm artınca: yeni `Migration` yaz, `MigrationTestHelper` ile test et, [CURRENT_VERSION]'ı artır.
 */
@Database(
    entities = [ReminderEntity::class, ReminderOccurrenceEntity::class, ScheduledAlarmEntity::class, DeliveryLogEntity::class],
    version = ToparlaDatabase.CURRENT_VERSION,
    exportSchema = true,
)
abstract class ToparlaDatabase : RoomDatabase() {
    abstract fun reminders(): ReminderDao

    abstract fun occurrences(): ReminderOccurrenceDao

    abstract fun scheduledAlarms(): ScheduledAlarmDao

    abstract fun deliveryLog(): DeliveryLogDao

    companion object {
        const val CURRENT_VERSION = 1
        const val FILE_NAME = "toparla.db"

        /**
         * Üretim veritabanını açar. Sürüm yükseltmesi varsa önce dosyanın kopyasını alır
         * ([PreMigrationBackup]); migration başarısız olursa çağıran [PreMigrationBackup.restore] ile geri döner.
         */
        fun open(context: Context, io: CoroutineDispatcher): ToparlaDatabase {
            val file = context.getDatabasePath(FILE_NAME)
            PreMigrationBackup.backupIfUpgrading(file, File(context.filesDir, PreMigrationBackup.DIR), CURRENT_VERSION)
            return Room.databaseBuilder<ToparlaDatabase>(context, file.path)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(io)
                .build()
        }
    }
}

/** Migration'dan önce veritabanı dosyasının kopyası ve gerekirse geri dönüş (blueprint B7). */
object PreMigrationBackup {
    const val DIR = "yedek"

    /** Diskteki veritabanının şema sürümü; dosya yoksa ya da okunamıyorsa 0. */
    fun storedVersion(dbFile: File): Int {
        if (!dbFile.exists()) return 0
        val connection = BundledSQLiteDriver().open(dbFile.path)
        return try {
            // WAL'daki son yazılar ana dosyaya insin ki kopya eksiksiz olsun.
            connection.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
            connection.prepare("PRAGMA user_version").use { if (it.step()) it.getLong(0).toInt() else 0 }
        } finally {
            connection.close()
        }
    }

    /**
     * Diskteki sürüm [targetVersion]'dan küçükse dosyayı `pre-migration-<eski sürüm>.db` adıyla kopyalar.
     * @return alınan kopya; yükseltme yoksa null
     */
    fun backupIfUpgrading(dbFile: File, backupDir: File, targetVersion: Int): File? {
        val stored = storedVersion(dbFile)
        if (stored == 0 || stored >= targetVersion) return null
        if (!backupDir.exists() && !backupDir.mkdirs()) throw IOException("Yedek klasörü oluşturulamadı: $backupDir")
        val backup = File(backupDir, "pre-migration-$stored.db")
        dbFile.copyTo(backup, overwrite = true)
        Timber.i("Migration öncesi kopya alındı: sürüm %d → %d", stored, targetVersion)
        return backup
    }

    /** Kopyayı geri koyar; WAL ve SHM artıkları silinir ki eski sürüm temiz açılsın. */
    fun restore(backup: File, dbFile: File) {
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        backup.copyTo(dbFile, overwrite = true)
    }
}
