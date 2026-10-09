package com.toparla.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.toparla.data.db.CreatedBy
import com.toparla.data.db.OwnerType
import com.toparla.data.db.ReminderEntity
import com.toparla.data.db.ReminderStore
import com.toparla.domain.core.IdGenerator
import com.toparla.domain.reminder.ReminderClass
import com.toparla.reminders.ReminderEntryPoint
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Yalnız `debug` sürümünde: cihaz testlerinde hatırlatma kurmak için adb tetikleyicisi (ekran gerektirmez).
 * `adb shell am broadcast -n com.toparla.app.dev/com.toparla.app.DebugReminderReceiver --es title Dişçi --es klass CRITICAL --ei delaySec 20 --ez persistent false`
 * `--es clear all` bütün hatırlatmaları siler (yalnız geliştirme sürümünün kendi verisi).
 */
class DebugReminderReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Access {
        fun store(): ReminderStore

        fun ids(): IdGenerator
    }

    override fun onReceive(context: Context, intent: Intent) {
        val access = EntryPointAccessors.fromApplication(context.applicationContext, Access::class.java)
        val entry = ReminderEntryPoint.of(context)
        val pending = goAsync()
        entry.scope().launch {
            try {
                val now = entry.clock().now()
                if (intent.hasExtra("clear")) {
                    access.store().activeDefinitions().forEach { access.store().deleteReminder(it.id, now) }
                } else if (intent.hasExtra("advance")) {
                    // Beklemeden deneme: sıradaki alarm (merdiven basamağı, ısrarlı takip sorusu) vakti gelmiş gibi ateşlenir.
                    // Gerçek zamanlama ayrıca, Kullanıcı beklemeden kayıttan ölçülür.
                    val next = access.store().scheduled().minByOrNull { it.fireAt }
                    Timber.i("Sıradaki alarm hemen ateşleniyor: %s", next?.key)
                    if (next != null) entry.engine().onAlarmFired(next.key, now)
                } else {
                    val start = LocalDateTime.ofInstant(now.plusSeconds(intent.getIntExtra("delaySec", DEFAULT_DELAY_SEC).toLong()), entry.clock().zone())
                    val id = access.ids().newId()
                    access.store().saveReminder(
                        ReminderEntity(
                            id = id,
                            ownerType = OwnerType.CUSTOM,
                            ownerId = null,
                            klass = ReminderClass.entries.firstOrNull { it.name == intent.getStringExtra("klass") } ?: ReminderClass.NORMAL,
                            title = intent.getStringExtra("title") ?: "Deneme",
                            body = intent.getStringExtra("body").orEmpty(),
                            // Planlayıcı saniyeyi korur; deneme için saniye hassasiyeti yeterli.
                            startLocal = start.truncatedTo(ChronoUnit.SECONDS).toString(),
                            zoneId = entry.clock().zone().id,
                            recurrence = "ONCE",
                            active = true,
                            persistent = intent.getBooleanExtra("persistent", false),
                            createdBy = CreatedBy.SYSTEM,
                            createdAt = now.toEpochMilli(),
                            updatedAt = now.toEpochMilli(),
                        ),
                    )
                    Timber.i("Deneme hatırlatması kuruldu: %s", id)
                }
                entry.engine().replan(entry.clock().now())
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val DEFAULT_DELAY_SEC = 20
    }
}
