package com.toparla.domain.reminder

import com.toparla.domain.Defaults
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Hatırlatma eklerken sunulan hazır zamanlar (taslak onayı 9 Ekim 2026). */
enum class QuickTime {
    IN_10_MIN,
    IN_1_HOUR,
    TONIGHT,
    TOMORROW_MORNING,
    ;

    fun resolve(now: Instant, zone: ZoneId): LocalDateTime {
        val local = LocalDateTime.ofInstant(now, zone)
        val evening = LocalTime.of(Defaults.QUICK_TIME_EVENING_HOUR, 0)
        return when (this) {
            IN_10_MIN -> local.plusMinutes(Defaults.QUICK_TIME_SOON_MIN)
            IN_1_HOUR -> local.plusHours(1)
            // Akşam saati geldiyse ya da geçtiyse "bu akşam" artık yarın akşamdır.
            TONIGHT -> if (local.toLocalTime().isBefore(evening)) local.toLocalDate().atTime(evening) else local.toLocalDate().plusDays(1).atTime(evening)
            TOMORROW_MORNING -> local.toLocalDate().plusDays(1).atTime(Defaults.QUICK_TIME_MORNING_HOUR, 0)
        }
    }
}

/** Kaydedilmeye hazır, doğrulanmış hatırlatma taslağı. */
data class ReminderDraft(val title: String, val startLocal: LocalDateTime) {
    companion object {
        const val MAX_TITLE = 80

        /** Başlık kırpılır ve sınırlanır; boş başlık ya da geçmiş an taslak üretmez (null). */
        fun validate(title: String, startLocal: LocalDateTime, now: Instant, zone: ZoneId): ReminderDraft? {
            val clean = title.trim().take(MAX_TITLE)
            val inFuture = startLocal.atZone(zone).toInstant().isAfter(now)
            return if (clean.isNotEmpty() && inFuture) ReminderDraft(clean, startLocal) else null
        }
    }
}

/** Hatırlatma Sağlığı denetimleri (blueprint G3, D23). Sıra önem sırasıdır: eksikse ilk düzeltilecek en başta. */
enum class HealthCheck {
    NOTIFICATIONS,
    EXACT_ALARMS,
    AUTO_START,
    BATTERY,
    FULL_SCREEN,
    DND_ACCESS,
}

/**
 * Telefonun o anki durumu. [autoStartConfirmed]: HyperOS otomatik başlatma izni uygulama içinden okunamaz;
 * yalnız Kullanıcı sihirbazda "açtım" dediyse true (sınamanın geçmesi bunu kanıtlamaz).
 */
data class HealthSnapshot(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    val batteryExempt: Boolean,
    val dndAccess: Boolean,
    val autoStartConfirmed: Boolean,
    val pendingAlarms: Int,
    val lastDeliveredAt: Instant?,
)

data class HealthReport(val toFix: List<HealthCheck>, val ok: List<HealthCheck>, val pendingAlarms: Int, val lastDeliveredAt: Instant?) {
    companion object {
        fun of(s: HealthSnapshot): HealthReport {
            val passed = mapOf(
                HealthCheck.NOTIFICATIONS to s.notifications,
                HealthCheck.EXACT_ALARMS to s.exactAlarms,
                HealthCheck.AUTO_START to s.autoStartConfirmed,
                HealthCheck.BATTERY to s.batteryExempt,
                HealthCheck.FULL_SCREEN to s.fullScreen,
                HealthCheck.DND_ACCESS to s.dndAccess,
            )
            val (ok, toFix) = HealthCheck.entries.partition { passed.getValue(it) }
            return HealthReport(toFix, ok, s.pendingAlarms, s.lastDeliveredAt)
        }
    }
}

/** Nabız kararı: [problems] şu an açık olan eksikler; [warn] Kullanıcı'ya yeni bir uyarı gösterilsin mi. */
data class HeartbeatDecision(val problems: Set<HealthCheck>, val warn: Boolean)

/**
 * Nabız (v3 §8.3, blueprint Bölüm I): günde bir ve uygulama açılışında motor sağlığına bakılır; hatırlatmanın
 * kaçmasına yol açabilecek bir eksik varsa sakin tek bir uyarı gösterilir. Aynı eksik için yeniden uyarılmaz:
 * uyarı yalnız **yeni** bir eksik çıkınca yinelenir, hepsi düzelince kalkar.
 */
object Heartbeat {
    /** Otomatik başlatma okunamaz (sihirbazın işi); Rahatsız Etme erişimi isteğe bağlıdır: ikisi de uyarı doğurmaz. */
    val WATCHED: Set<HealthCheck> = setOf(HealthCheck.NOTIFICATIONS, HealthCheck.EXACT_ALARMS, HealthCheck.FULL_SCREEN, HealthCheck.BATTERY)

    fun decide(report: HealthReport, alreadyWarned: Set<HealthCheck>): HeartbeatDecision {
        val problems = report.toFix.filterTo(LinkedHashSet()) { it in WATCHED }
        return HeartbeatDecision(problems, warn = (problems - alreadyWarned).isNotEmpty())
    }
}

sealed interface SelfTestResult {
    data object Waiting : SelfTestResult

    data class Arrived(val delay: Duration) : SelfTestResult

    data object NotArrived : SelfTestResult
}

/** "Hatırlatmaları sına": kısa süre sonraya deneme hatırlatması kurulur; gelip gelmediği teslim kaydından okunur. */
object SelfTest {
    val DELAY: Duration = Duration.ofSeconds(Defaults.SELF_TEST_DELAY_SEC)
    val TIMEOUT: Duration = Duration.ofSeconds(Defaults.SELF_TEST_TIMEOUT_SEC)

    fun evaluate(plannedAt: Instant, deliveredAt: Instant?, now: Instant): SelfTestResult = when {
        deliveredAt != null -> SelfTestResult.Arrived(Duration.between(plannedAt, deliveredAt).coerceAtLeast(Duration.ZERO))
        now.isAfter(plannedAt.plus(TIMEOUT)) -> SelfTestResult.NotArrived
        else -> SelfTestResult.Waiting
    }
}
