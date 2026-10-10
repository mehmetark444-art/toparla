package com.toparla.reminders

import com.toparla.domain.core.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.Test

/** Depolama dolu senaryosunun ikinci yarısı: arka plan işindeki hata uygulamayı düşürmez. */
class ReminderScopeTest {
    private val inline = object : DispatcherProvider {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
    }

    @Test
    fun `arka plan isindeki yakalanmamis hata sureci dusurmez ve sonraki isi engellemez`() {
        val uncaught = ArrayList<Throwable>()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> uncaught += e }
        try {
            val scope = RemindersModule.scope(inline)
            var laterJobRan = false

            scope.launch { error("kayıt açılamadı") }
            scope.launch { laterJobRan = true }

            assertTrue("hata sürecin genel işleyicisine ulaştı: $uncaught", uncaught.isEmpty())
            assertTrue(laterJobRan)
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
    }
}
