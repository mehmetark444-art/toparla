package com.toparla.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.toparla.data.db.ReminderStore
import com.toparla.data.settings.SettingsStore
import com.toparla.domain.Defaults
import com.toparla.domain.core.Clock
import com.toparla.domain.core.DispatcherProvider
import com.toparla.domain.reminder.FollowUpConfig
import com.toparla.domain.reminder.ReminderEngine
import com.toparla.domain.reminder.ReminderNotifier
import com.toparla.domain.reminder.ReminderRepository
import com.toparla.domain.reminder.ReminderScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Alıcıların ve servisin işi için uygulama ömürlü arka plan kapsamı. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReminderScope

@Module
@InstallIn(SingletonComponent::class)
object RemindersModule {
    /**
     * Arka plan işinde yakalanmamış hata uygulamayı düşürmez, günlüğe yazılır: hatırlatma uygulamasında çökme
     * (ör. depolama doluyken açılışta), sıradaki alarmın alıcısını da geciktirir. Teslimin kendi hatası ayrıca
     * [deliverSafely] ile ele alınır.
     */
    @Provides @Singleton @ReminderScope
    fun scope(dispatchers: DispatcherProvider): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatchers.io + CoroutineExceptionHandler { _, e -> Timber.e(e, "Hatırlatma işi yarıda kaldı") })

    @Provides @Singleton
    fun alarmScheduler(@ApplicationContext context: Context, intents: ReminderIntents): AlarmManagerScheduler =
        AlarmManagerScheduler(context, intents)

    @Provides
    fun scheduler(impl: AlarmManagerScheduler): ReminderScheduler = impl

    @Provides @Singleton
    fun notifier(@ApplicationContext context: Context, intents: ReminderIntents): ReminderNotifier =
        AndroidReminderNotifier(context, intents)

    @Provides
    fun repository(store: ReminderStore): ReminderRepository = store

    @Provides @Singleton
    fun healthWatch(@ApplicationContext context: Context, probe: HealthProbe, settings: SettingsStore, intents: ReminderIntents): HealthWatch =
        HealthWatch(context, settings, intents) { probe.report() }

    /**
     * Tek motor. Israrlı takip ayarı her planlamada ayarlardan yeniden okunur; saat dilimi telefonun o anki dilimidir.
     * Sessizlik (odak, Bunaldım, "Bugün sessiz") ilgili fazlarda bağlanır; şimdilik yoktur.
     */
    @Provides @Singleton
    fun engine(
        repository: ReminderRepository,
        scheduler: ReminderScheduler,
        notifier: ReminderNotifier,
        settings: SettingsStore,
        clock: Clock,
    ): ReminderEngine = ReminderEngine(repository, scheduler, notifier) {
        val current = settings.settings.first()
        FollowUpConfig(
            interval = Duration.ofMinutes(current.persistentIntervalMin.toLong()),
            zone = clock.zone(),
            sleepStart = current.sleepStart,
            sleepEnd = current.sleepEnd,
        )
    }
}

/** Güvenlik ağları (blueprint Bölüm I): WorkManager gecikse bile bakım alarmı pencereyi ayrıca doldurur. */
object ReminderSafetyNets {
    private const val WATCHDOG = "kritik-bekci"
    private const val DAILY = "gunluk-bakim"
    private const val HEARTBEAT = "nabiz"

    /** Aynı adla yeniden çağırmak mevcut işi korur (idempotans). */
    fun ensureScheduled(context: Context) {
        val work = WorkManager.getInstance(context)
        work.enqueueUniquePeriodicWork(
            WATCHDOG,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CriticalWatchdogWorker>(Defaults.CRITICAL_WATCHDOG_PERIOD_MIN, TimeUnit.MINUTES).build(),
        )
        work.enqueueUniquePeriodicWork(
            DAILY,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyMaintenanceWorker>(1, TimeUnit.DAYS).build(),
        )
        work.enqueueUniquePeriodicWork(
            HEARTBEAT,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<HeartbeatWorker>(1, TimeUnit.DAYS).build(),
        )
    }
}

/** 15 dakikada bir: önümüzdeki 20 dakikadaki kritik olayın sistem alarmı gerçekten kurulu mu? */
class CriticalWatchdogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val entry = ReminderEntryPoint.of(applicationContext)
        entry.engine().watchdog(entry.clock().now())
        Timber.i("Kritik bekçi koştu")
        return Result.success()
    }
}

/** Günde bir: pencereyi doldurur, teslim denetçisini koşar (çalmamış teslim geç teslim edilir), kopyayı budar. */
class DailyMaintenanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val entry = ReminderEntryPoint.of(applicationContext)
        val now = entry.clock().now()
        entry.engine().replan(now)
        BootMirror(applicationContext).prune(now.toEpochMilli())
        Timber.i("Günlük bakım koştu")
        return Result.success()
    }
}
