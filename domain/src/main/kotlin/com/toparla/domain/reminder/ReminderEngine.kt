package com.toparla.domain.reminder

import com.toparla.domain.Defaults
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Hatırlatma motoru (Katman 0; AI yok). Planlayıcıyı, depoyu, sistem alarmlarını ve bildirimi birbirine bağlar:
 * pencere doldurma, teslim, merdiven, ısrarlı takip, eylemler, geç teslim ve kritik bekçi (blueprint Bölüm I).
 *
 * Her giriş noktası idempotenttir: aynı alarm ya da aynı dokunuş ikinci kez gelirse sonuç değişmez.
 * Zaman dışarıdan verilir; Android'e dokunmaz (yan etkiler arayüzlerin arkasındadır).
 */
class ReminderEngine(
    private val repo: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
    private val followUpConfig: suspend () -> FollowUpConfig,
) {
    private val mutex = Mutex()

    /**
     * Pencereyi doldurur: eksik alarmı kurar, fazlayı iptal eder, vakti geçmiş teslimleri hemen teslim eder.
     * @param rearm sistem alarmlarının silinmiş olabileceği durumlarda (açılış, güncelleme, saat değişimi) true:
     * kayıtlı olsa da her alarm yeniden kurulur.
     */
    suspend fun replan(now: Instant, rearm: Boolean = false) = mutex.withLock { replanLocked(now, rearm) }

    /** Sistem alarmı ateşlendi (ana teslim, merdiven basamağı, ısrarlı takip sorusu ya da erteleme). */
    suspend fun onAlarmFired(key: String, now: Instant) = mutex.withLock {
        val plannedAt = repo.scheduled().firstOrNull { it.key == key }?.fireAt
        if (deliver(key, now, plannedAt)) replanLocked(now, rearm = false)
    }

    /** Bildirim eylemi ya da ekrandan gelen yanıt. Geçersiz ya da yinelenen eylem yok sayılır. */
    suspend fun onAction(occurrenceKey: String, action: ReminderAction, now: Instant, snoozeDelay: Duration? = null) = mutex.withLock {
        val occurrence = repo.occurrence(occurrenceKey) ?: return@withLock
        val info = repo.info(occurrence.reminderId)
        val changed = when (action) {
            ReminderAction.OPENED -> transition(occurrence, OccurrenceEvent.OPENED, now, resolves = false)
            ReminderAction.DONE -> transition(occurrence, OccurrenceEvent.MARKED_DONE, now, resolves = true)
            ReminderAction.SNOOZE -> snooze(occurrence, info, now, snoozeDelay)
            ReminderAction.TOMORROW -> snoozeUntil(occurrence, now, nextWake(now))
            ReminderAction.NOT_TODAY ->
                if (info?.persistent == true) {
                    // Israrlı iş sessizce bitmez: açık seçimle ertesi sabaha taşınır (karar 0003).
                    snoozeUntil(occurrence, now, nextWake(now))
                } else {
                    transition(occurrence, OccurrenceEvent.SKIP, now, resolves = true)
                }
        }
        if (changed) {
            repo.log(occurrenceKey, now, EVENT_ACTION, action.name)
            refreshPersistent(now)
            replanLocked(now, rearm = false)
        }
    }

    /** Kritik bekçi (15 dk'da bir): yakındaki kritik olayın alarmı yoksa hemen kurar ve kayda yazar. */
    suspend fun watchdog(now: Instant) = mutex.withLock {
        val missing = CriticalWatchdog.missing(now, repo.activeDefinitions(), repo.scheduled())
        if (missing.isEmpty()) return@withLock
        missing.forEach {
            scheduler.schedule(it, repo.info(it.reminderId)?.title)
            repo.log(it.key, now, EVENT_WATCHDOG_REARMED)
        }
        repo.applyPlan(missing, emptyList())
    }

    /**
     * Bildirimler yeniden açıldı (izin ya da kanal): kapalıyken gösterilemeyen, hâlâ yanıt bekleyen teslimler
     * yeniden gösterilir. Sistem, izin kapanınca uygulamanın bildirimlerini siler ve açılınca geri getirmez.
     */
    suspend fun reshowOpen(now: Instant) = mutex.withLock {
        for (occurrence in repo.openOccurrences()) {
            val info = repo.info(occurrence.reminderId)?.takeUnless { it.persistent } ?: continue
            notifier.show(notice(occurrence.key, info, LadderAction.NOTIFY, occurrence.plannedAt, Duration.ZERO))
        }
        refreshPersistent(now)
    }

    private suspend fun replanLocked(now: Instant, rearm: Boolean) {
        val config = followUpConfig()
        closeOrphans(now)
        for (pass in 0 until MAX_PASSES) {
            expireStale(now, config)
            val definitions = repo.activeDefinitions()
            val existing = repo.scheduled()
            val inFlight = repo.openOccurrences().mapNotNull { inFlightOf(it) }
            val snoozes = repo.pendingSnoozes().mapNotNull { o ->
                repo.info(o.reminderId)?.let { SnoozedDelivery(o.key, o.reminderId, it.klass, o.plannedAt) }
            }
            val result = ReminderPlanner.plan(now, definitions, existing, inFlight = inFlight, snoozes = snoozes, followUp = config)
            result.toCancel.forEach(scheduler::cancel)
            val toArm = if (rearm && pass == 0) {
                // Sistem alarmları silinmiş olabilir: tabloda kayıtlı olanlar dahil hepsi yeniden kurulur.
                ReminderPlanner.plan(now, definitions, emptyList(), inFlight = inFlight, snoozes = snoozes, followUp = config).toSchedule
            } else {
                result.toSchedule
            }
            // Başlık, kilit açılmadan çalarsa gösterilmek üzere alarmla birlikte verilir.
            toArm.forEach { scheduler.schedule(it, repo.info(it.reminderId)?.title) }
            repo.applyPlan(result.toSchedule, result.toCancel)

            var delivered = false
            val past = existing.filter { !it.fireAt.isAfter(now) }
            val fired = repo.firedKeys(past.map { it.key })
            // Ateşlenmiş ama kaydı kalmış satır (teslim sırasında süreç öldüyse) temizlenir.
            past.filter { it.key in fired }.forEach { repo.removeScheduled(it.key) }
            for (unfired in DeliveryAuditor.findUnfired(now, existing, fired)) {
                repo.log(unfired.key, now, EVENT_MISSED_DETECTED)
                if (deliver(unfired.key, now, unfired.fireAt)) delivered = true
            }
            for (due in result.dueNow) {
                if (deliver(due.key, now, due.fireAt)) delivered = true
            }
            // Teslim yeni basamak ya da soru doğurabilir; onları da kurmak için bir tur daha.
            if (!delivered) break
        }
        // Vakti yeni geçmiş, henüz ateşlenmemiş kayıt varsa (tolerans içinde: sistem teslim ediyor olabilir)
        // bir sonraki bakım 12 saat sonraya bırakılmaz; tolerans dolunca yeniden bakılır. Aksi hâlde kilitli
        // açılıştan hemen sonra çalmış bir hatırlatma saatlerce bekler (9 Ekim cihaz denemesi).
        val stillWaiting = repo.scheduled().any { !it.fireAt.isAfter(now) }
        val next = if (stillWaiting) now.plus(DeliveryGrouping.LATE_TOLERANCE).plus(RECHECK_MARGIN) else MaintenancePolicy.nextMaintenanceAt(now)
        scheduler.scheduleMaintenance(next)
    }

    /** @return bir şey gösterildiyse ya da durum değiştiyse true */
    private suspend fun deliver(key: String, now: Instant, plannedFire: Instant?): Boolean {
        // Alarm kaydını burada silmeye gerek yok: ateşlenmiş kaydı bir sonraki planlama turu temizler.
        if (!repo.recordFired(key, now)) return false
        val parsed = AlarmKey.parse(key)
        val info = repo.info(parsed.reminderId) ?: return false
        return when (parsed.kind) {
            AlarmKeyKind.MAIN, AlarmKeyKind.SNOOZE -> deliverOccurrence(parsed, info, now, plannedFire ?: AlarmKey.plannedAtOf(key) ?: now)
            AlarmKeyKind.LADDER -> deliverLadderStep(parsed, info, now)
            AlarmKeyKind.FOLLOW_UP -> deliverFollowUp(parsed, now)
        }
    }

    private suspend fun deliverOccurrence(parsed: AlarmKey, info: ReminderInfo, now: Instant, plannedAt: Instant): Boolean {
        val existing = repo.occurrence(parsed.occurrenceKey)
        val base = existing ?: OccurrenceRecord(parsed.occurrenceKey, parsed.reminderId, plannedAt, OccurrenceState.PLANNED)
        val next = OccurrenceStateMachine.next(base.state, OccurrenceEvent.FIRED) ?: return false
        repo.saveOccurrence(base.copy(state = next, deliveredAt = now, ladderStepsDone = 1, lastAskedAt = now))
        notifier.show(notice(parsed.occurrenceKey, info, LadderAction.NOTIFY, base.plannedAt, DeliveryGrouping.lateBy(base.plannedAt, now)))
        // Bildirim kapalıysa teslim "gösterildi" diye kaydedilmez; izin açılınca yeniden gösterilir ([reshowOpen]).
        repo.log(parsed.occurrenceKey, now, if (notifier.isBlocked(info.klass)) EVENT_BLOCKED else EVENT_POSTED)
        return true
    }

    private suspend fun deliverLadderStep(parsed: AlarmKey, info: ReminderInfo, now: Instant): Boolean {
        val occurrence = repo.occurrence(parsed.occurrenceKey)?.takeIf { it.state in OPEN_STATES } ?: return false
        val step = Ladder.stepsFor(info.klass, followUpConfig().trustedContactEnabled).getOrNull(parsed.index) ?: return false
        repo.saveOccurrence(occurrence.copy(ladderStepsDone = maxOf(occurrence.ladderStepsDone, parsed.index + 1), lastAskedAt = now))
        notifier.show(notice(parsed.occurrenceKey, info, step.action, occurrence.plannedAt, Duration.ZERO))
        return true
    }

    private suspend fun deliverFollowUp(parsed: AlarmKey, now: Instant): Boolean {
        val occurrence = repo.occurrence(parsed.occurrenceKey)?.takeIf { it.state in OPEN_STATES } ?: return false
        repo.saveOccurrence(occurrence.copy(asksDone = maxOf(occurrence.asksDone, parsed.index + 1), lastAskedAt = now))
        refreshPersistent(now)
        return true
    }

    private suspend fun transition(occurrence: OccurrenceRecord, event: OccurrenceEvent, now: Instant, resolves: Boolean): Boolean {
        val next = OccurrenceStateMachine.next(occurrence.state, event) ?: return false
        repo.saveOccurrence(occurrence.copy(state = next, resolvedAt = if (resolves) now else occurrence.resolvedAt))
        if (resolves) notifier.cancel(occurrence.key)
        return true
    }

    private suspend fun snooze(occurrence: OccurrenceRecord, info: ReminderInfo?, now: Instant, requested: Duration?): Boolean {
        if (OccurrenceStateMachine.next(occurrence.state, OccurrenceEvent.SNOOZE) == null) return false
        return when (val decision = SnoozePolicy.decide(occurrence.snoozeCount, requested)) {
            is SnoozeDecision.Snooze -> snoozeUntil(occurrence, now, now.plus(decision.delay))
            SnoozeDecision.AskCarryOrSkip -> {
                if (info != null) notifier.askCarryOrSkip(occurrence.key, info)
                false
            }
        }
    }

    /** Ertelenen teslim kapanır; aynı olayın yeni anahtarlı yeni teslimi [until] anına kurulur. */
    private suspend fun snoozeUntil(occurrence: OccurrenceRecord, now: Instant, until: Instant): Boolean {
        if (!transition(occurrence, OccurrenceEvent.SNOOZE, now, resolves = true)) return false
        val index = occurrence.snoozeCount + 1
        val rootKey = occurrence.key.replace(SNOOZE_TAIL, "")
        repo.saveOccurrence(
            OccurrenceRecord(SnoozePolicy.snoozedKey(rootKey, index), occurrence.reminderId, until, OccurrenceState.PLANNED, snoozeCount = index),
        )
        return true
    }

    /** Tanımı silinmiş hatırlatmanın yanıt bekleyen ve ertelenmiş teslimleri kapanır; bildirimi kaldırılır. */
    private suspend fun closeOrphans(now: Instant) {
        val orphans = (repo.openOccurrences() + repo.pendingSnoozes()).filter { repo.info(it.reminderId) == null }
        orphans.forEach { transition(it, OccurrenceEvent.DEFINITION_CHANGED, now, resolves = true) }
        if (orphans.isNotEmpty()) refreshPersistent(now)
    }

    /** Merdiveni bitmiş, yanıtsız kalmış ısrarsız iş bir süre sonra "süresi doldu" olur (gün kapanışında Taşınan). */
    private suspend fun expireStale(now: Instant, config: FollowUpConfig) {
        for (occurrence in repo.openOccurrences()) {
            // Israrlı iş süresi dolmaz: "Yaptım" ya da "Bugün olmayacak" denene kadar açıktır (karar 0003).
            val info = repo.info(occurrence.reminderId)?.takeUnless { it.persistent }
            val lastAsked = occurrence.lastAskedAt
            if (info != null && lastAsked != null) {
                val ladderDone = occurrence.ladderStepsDone >= Ladder.stepsFor(info.klass, config.trustedContactEnabled).size
                if (ladderDone && !lastAsked.plus(EXPIRY).isAfter(now)) {
                    transition(occurrence, OccurrenceEvent.LADDER_EXHAUSTED, now, resolves = false)
                }
            }
        }
    }

    private suspend fun refreshPersistent(now: Instant) {
        val items = repo.openOccurrences().mapNotNull { o ->
            repo.info(o.reminderId)?.takeIf { it.persistent }?.let { PersistentItem(o.key, it, o.asksDone) }
        }
        notifier.showPersistent(items, now)
    }

    private suspend fun inFlightOf(o: OccurrenceRecord): InFlightOccurrence? {
        val info = repo.info(o.reminderId) ?: return null
        val firedAt = o.deliveredAt ?: return null
        return InFlightOccurrence(o.key, o.reminderId, info.klass, firedAt, o.ladderStepsDone.coerceAtLeast(1), info.persistent, o.lastAskedAt ?: firedAt, o.asksDone)
    }

    /** Uyku penceresinin bittiği ilk an (şimdiden sonra): "Yarın" ve "Bugün olmayacak" buraya taşır. */
    private suspend fun nextWake(now: Instant): Instant {
        val config = followUpConfig()
        val local = now.atZone(config.zone)
        val today = ZonedDateTime.of(local.toLocalDate(), config.sleepEnd, config.zone).toInstant()
        return if (today.isAfter(now)) today else ZonedDateTime.of(local.toLocalDate().plusDays(1), config.sleepEnd, config.zone).toInstant()
    }

    private fun notice(occurrenceKey: String, info: ReminderInfo, action: LadderAction, plannedAt: Instant, lateBy: Duration) =
        DeliveryNotice(occurrenceKey, info, action, plannedAt, lateBy, "g@${plannedAt.truncatedTo(ChronoUnit.MINUTES).toEpochMilli()}")

    companion object {
        const val EVENT_MISSED_DETECTED = "MISSED_DETECTED"
        const val EVENT_WATCHDOG_REARMED = "WATCHDOG_REARMED"
        const val EVENT_POSTED = "POSTED"
        const val EVENT_BLOCKED = "BLOCKED"
        const val EVENT_ACTION = "ACTION"

        /** Alarm kapalı uygulamayı uyandırdı ([ColdDelivery]); teslim hattının Android tarafı yazar. */
        const val EVENT_WOKE_APP = "WOKE_APP"

        val EXPIRY: Duration = Duration.ofMinutes(Defaults.UNANSWERED_EXPIRY_MIN)
        private const val MAX_PASSES = 3

        /** Tolerans dolduktan sonra yeniden bakmadan önce bırakılan pay. */
        val RECHECK_MARGIN: Duration = Duration.ofSeconds(5)
        private val OPEN_STATES = setOf(OccurrenceState.DELIVERED, OccurrenceState.SEEN)
        private val SNOOZE_TAIL = Regex("""#s\d+$""")
    }
}
