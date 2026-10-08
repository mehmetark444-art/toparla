package com.toparla.app

import android.app.Application
import android.util.Log
import com.toparla.domain.core.Clock
import com.toparla.domain.core.RotatingLogFile
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class ToparlaApp : Application() {
    @Inject lateinit var clock: Clock

    override fun onCreate() {
        super.onCreate()
        Timber.plant(FileLogTree(RotatingLogFile(File(filesDir, LOG_DIR), LOG_FILE_BYTES, LOG_FILE_COUNT), clock))
        DevTools.install(this)
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
    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = LEVELS[priority] ?: priority.toString()
        val error = t?.let { " | ${it.javaClass.simpleName}: ${it.message}" }.orEmpty()
        file.append("${clock.now()} $level ${tag.orEmpty()} $message$error")
    }

    private companion object {
        val LEVELS = mapOf(Log.INFO to "I", Log.WARN to "W", Log.ERROR to "E", Log.ASSERT to "A")
    }
}
