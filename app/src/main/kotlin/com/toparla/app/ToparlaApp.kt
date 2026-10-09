package com.toparla.app

import android.app.Application
import android.os.UserManager
import android.util.Log
import com.toparla.domain.core.Clock
import com.toparla.domain.core.RotatingLogFile
import com.toparla.reminders.ReminderEntryPoint
import com.toparla.reminders.ReminderSafetyNets
import com.toparla.reminders.ReminderStartup
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
class ToparlaApp : Application() {
    @Inject lateinit var clock: Clock

    override fun onCreate() {
        super.onCreate()
        Timber.plant(FileLogTree(RotatingLogFile(File(filesDir, LOG_DIR), LOG_FILE_BYTES, LOG_FILE_COUNT), clock))
        DevTools.install(this)
        startReminders()
    }

    /**
     * Uygulama her açıldığında pencere yeniden doldurulur ve güvenlik ağları kurulur: zorla durdurma ya da
     * güncelleme sistem alarmlarını silmiş olabilir (F1 bulgusu). Kilit açılmadan (Direct Boot) veritabanı
     * kapalıdır; o durumda alarmlar alıcıdaki kopyadan kurulur ve bu adım atlanır.
     */
    private fun startReminders() {
        if (!getSystemService(UserManager::class.java).isUserUnlocked) return
        val entry = ReminderEntryPoint.of(this)
        entry.scope().launch { ReminderStartup.recover(this@ToparlaApp, entry) }
        ReminderSafetyNets.ensureScheduled(this)
    }

    private companion object {
        const val LOG_DIR = "logs"
        const val LOG_FILE_BYTES = 512L * 1024
        const val LOG_FILE_COUNT = 4
    }
}

/**
 * Timber iletilerini dönen dosyaya yazar: INFO ve üstü. Tanılama dışa aktarımı (F10.7) bu dosyaları kullanır.
 * Çağıran kural: günlüğe gizli değer, bildirim/ekran içeriği ve sağlık verisi yazılmaz.
 */
class FileLogTree(private val file: RotatingLogFile, private val clock: Clock) : Timber.Tree() {
    /** Dosyaya yazma çağıran iş parçacığını (çoğu zaman ana iş parçacığı) bekletmesin diye tek arka plan sırası. */
    private val writer = Executors.newSingleThreadExecutor { task -> Thread(task, "toparla-log").apply { isDaemon = true } }

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = LEVELS[priority] ?: priority.toString()
        val error = t?.let { " | ${it.javaClass.simpleName}: ${it.message}" }.orEmpty()
        val line = "${clock.now()} $level ${tag.orEmpty()} $message$error"
        writer.execute { file.append(line) }
    }

    private companion object {
        val LEVELS = mapOf(Log.INFO to "I", Log.WARN to "W", Log.ERROR to "E", Log.ASSERT to "A")
    }
}
