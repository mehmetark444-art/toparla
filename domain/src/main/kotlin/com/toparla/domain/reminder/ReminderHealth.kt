package com.toparla.domain.reminder

import com.toparla.domain.Defaults
import java.time.Duration
import java.time.Instant

/** Hatırlatma Sağlığı denetimleri (blueprint G3, D23). Sıra önem sırasıdır: eksikse ilk düzeltilecek en başta. */
enum class HealthCheck {
    NOTIFICATIONS,
    CHANNELS,
    EXACT_ALARMS,
    AUTO_START,
    BATTERY,
    STANDBY_BUCKET,
    FULL_SCREEN,
    DND_ACCESS,
}

/**
 * Android'in uygulamayı koyduğu kullanım kovası (blueprint G3: hedef etkin ya da çalışma kümesi). Kova kötüleştikçe
 * sistem uygulamanın alarmlarını ve işlerini daha çok erteler. Pil kısıtlamasından muaf uygulama kovaya girmez.
 */
enum class StandbyBucket(private val upTo: Int) {
    EXEMPTED(5),
    ACTIVE(10),
    WORKING_SET(20),
    FREQUENT(30),
    RARE(40),
    RESTRICTED(Int.MAX_VALUE),
    ;

    val healthy: Boolean get() = this <= WORKING_SET

    companion object {
        /** `UsageStatsManager.getAppStandbyBucket()` değeri; tanınmayan ara değer bir üst (daha kısıtlı) kovaya düşer. */
        fun of(androidValue: Int): StandbyBucket = entries.first { androidValue <= it.upTo }
    }
}

/** Bildirim kanalının önemi (blueprint G3 "kanal önemi"). Değerler Android'in önem düzeyleridir: büyük olan daha belirgin. */
object ChannelHealth {
    /** Kanal kapatıldıysa ya da önemi kurulduğundan aşağı çekildiyse kısılmıştır; Kullanıcı'nın yükseltmesi sorun değildir. */
    fun weakened(designedImportance: Int, actualImportance: Int): Boolean = actualImportance < designedImportance
}

/** Bir teslimin zamanlaması. [blocked]: vaktinde çaldı ama bildirim kapalı olduğu için gösterilemedi. */
data class DeliveryTiming(val plannedAt: Instant, val deliveredAt: Instant, val blocked: Boolean = false) {
    /** Tolerans (±1 dk sözü) içindeyse sıfır. */
    val lateBy: Duration get() = DeliveryGrouping.lateBy(plannedAt, deliveredAt)

    val onTime: Boolean get() = !blocked && lateBy.isZero
}

/** "Vaktinde ulaşmayan" sayımı (blueprint D23 "son 7 gün kaçan"): Kullanıcı'nın değil, teslim hattının kaydıdır. */
object DeliveryHealth {
    val WINDOW: Duration = Duration.ofDays(Defaults.HEALTH_LOOKBACK_DAYS)

    fun missed(deliveries: List<DeliveryTiming>, now: Instant): Int {
        val since = now.minus(WINDOW)
        return deliveries.count { !it.plannedAt.isBefore(since) && !it.onTime }
    }
}

/**
 * Telefonun o anki durumu. [autoStartConfirmed]: HyperOS otomatik başlatma izni uygulama içinden okunamaz;
 * yalnız Kullanıcı sihirbazda "açtım" dediyse true (sınamanın geçmesi bunu kanıtlamaz).
 *
 * @param weakenedChannels bildirim kanalı kapatılmış ya da önemi düşürülmüş hatırlatma sınıfları
 * @param upcomingReminders kurulu sıradaki hatırlatma sayısı (merdiven basamakları ve takip soruları sayılmaz)
 * @param lastDelivery en son çalan hatırlatma (deneme hatırlatması dahil)
 * @param recentDeliveries Kullanıcı'nın hatırlatmalarının son teslimleri; pencere [DeliveryHealth.missed] ile uygulanır
 */
data class HealthSnapshot(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    val batteryExempt: Boolean,
    val dndAccess: Boolean,
    val autoStartConfirmed: Boolean,
    val weakenedChannels: Set<ReminderClass>,
    val standbyBucket: StandbyBucket,
    val upcomingReminders: Int,
    val lastDelivery: DeliveryTiming?,
    val recentDeliveries: List<DeliveryTiming>,
    val lastSelfTest: SelfTestRecord?,
)

data class HealthReport(
    val toFix: List<HealthCheck>,
    val ok: List<HealthCheck>,
    val weakenedChannels: Set<ReminderClass>,
    val standbyBucket: StandbyBucket,
    val upcomingReminders: Int,
    val lastDelivery: DeliveryTiming?,
    val missedLastWeek: Int,
    val lastSelfTest: SelfTestRecord?,
) {
    companion object {
        fun of(s: HealthSnapshot, now: Instant): HealthReport {
            val passed = mapOf(
                HealthCheck.NOTIFICATIONS to s.notifications,
                HealthCheck.CHANNELS to s.weakenedChannels.isEmpty(),
                HealthCheck.EXACT_ALARMS to s.exactAlarms,
                HealthCheck.AUTO_START to s.autoStartConfirmed,
                HealthCheck.BATTERY to s.batteryExempt,
                HealthCheck.STANDBY_BUCKET to s.standbyBucket.healthy,
                HealthCheck.FULL_SCREEN to s.fullScreen,
                HealthCheck.DND_ACCESS to s.dndAccess,
            )
            val (ok, toFix) = HealthCheck.entries.partition { passed.getValue(it) }
            return HealthReport(
                toFix = toFix,
                ok = ok,
                weakenedChannels = s.weakenedChannels,
                standbyBucket = s.standbyBucket,
                upcomingReminders = s.upcomingReminders,
                lastDelivery = s.lastDelivery,
                missedLastWeek = DeliveryHealth.missed(s.recentDeliveries, now),
                lastSelfTest = s.lastSelfTest,
            )
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
    /**
     * Otomatik başlatma okunamaz (sihirbazın işi); Rahatsız Etme erişimi isteğe bağlıdır; bekleme kovasının çaresi
     * pil muafiyetidir ve o zaten izlenir (muaf uygulama kovaya girmez): üçü de uyarı doğurmaz.
     */
    val WATCHED: Set<HealthCheck> =
        setOf(HealthCheck.NOTIFICATIONS, HealthCheck.CHANNELS, HealthCheck.EXACT_ALARMS, HealthCheck.FULL_SCREEN, HealthCheck.BATTERY)

    fun decide(report: HealthReport, alreadyWarned: Set<HealthCheck>): HeartbeatDecision {
        val problems = report.toFix.filterTo(LinkedHashSet()) { it in WATCHED }
        return HeartbeatDecision(problems, warn = (problems - alreadyWarned).isNotEmpty())
    }
}

/** Alarm kapalı uygulamayı mı uyandırdı? Süreç alarmdan hemen önce doğduysa evet. */
object ColdDelivery {
    val MAX_PROCESS_AGE: Duration = Duration.ofMillis(Defaults.WOKEN_PROCESS_MAX_AGE_MS)

    /** @param processAge alarm alındığı anda sürecin yaşı; okunamadıysa negatif verilir ve kanıt sayılmaz */
    fun wokeApp(processAge: Duration): Boolean = !processAge.isNegative && processAge <= MAX_PROCESS_AGE
}

sealed interface SelfTestResult {
    data object Waiting : SelfTestResult

    /**
     * Vaktinde geldi. [appWasClosed]: alarm kapalı uygulamayı uyandırdıysa true. Uygulama açıkken gelen deneme
     * kapalı uygulamaya teslimi sınamış olmaz (proje beyni H38).
     */
    data class Arrived(val delay: Duration, val appWasClosed: Boolean) : SelfTestResult

    /** Tolerans dışında geldi: sistem alarmı vaktinde teslim etmedi; hatırlatma sonradan, çoğunlukla uygulama açılınca gösterildi. */
    data class Late(val delay: Duration) : SelfTestResult

    data object NotArrived : SelfTestResult
}

/** Biten sınamanın kalıcı sonucu (blueprint G3: "sonuç kaydedilir"). */
enum class SelfTestOutcome {
    ARRIVED_CLOSED,
    ARRIVED_OPEN,
    LATE,
    NOT_ARRIVED,
    ;

    val passed: Boolean get() = this == ARRIVED_CLOSED || this == ARRIVED_OPEN
}

data class SelfTestRecord(val at: Instant, val outcome: SelfTestOutcome) {
    /** Ayarlarda tek metin olarak saklanır. */
    fun encode(): String = "${at.toEpochMilli()}$SEPARATOR${outcome.name}"

    companion object {
        private const val SEPARATOR = "|"

        /** Bozuk ya da tanınmayan kayıt yok sayılır (null). */
        fun decode(text: String?): SelfTestRecord? {
            val parts = text?.split(SEPARATOR) ?: return null
            val at = parts.getOrNull(0)?.toLongOrNull() ?: return null
            val outcome = SelfTestOutcome.entries.firstOrNull { it.name == parts.getOrNull(1) } ?: return null
            return SelfTestRecord(Instant.ofEpochMilli(at), outcome)
        }
    }
}

fun SelfTestResult.outcome(): SelfTestOutcome? = when (this) {
    SelfTestResult.Waiting -> null
    is SelfTestResult.Arrived -> if (appWasClosed) SelfTestOutcome.ARRIVED_CLOSED else SelfTestOutcome.ARRIVED_OPEN
    is SelfTestResult.Late -> SelfTestOutcome.LATE
    SelfTestResult.NotArrived -> SelfTestOutcome.NOT_ARRIVED
}

/** "Hatırlatmaları sına": kısa süre sonraya deneme hatırlatması kurulur; gelip gelmediği teslim kaydından okunur. */
object SelfTest {
    val DELAY: Duration = Duration.ofSeconds(Defaults.SELF_TEST_DELAY_SEC)
    val TIMEOUT: Duration = Duration.ofSeconds(Defaults.SELF_TEST_TIMEOUT_SEC)

    /** Deneme hatırlatmasının ulaşmasını etkileyebilen denetimler; tam ekran ve Rahatsız Etme erişimi etkilemez. */
    private val DELIVERY_CHECKS = setOf(
        HealthCheck.NOTIFICATIONS, HealthCheck.CHANNELS, HealthCheck.EXACT_ALARMS,
        HealthCheck.AUTO_START, HealthCheck.BATTERY, HealthCheck.STANDBY_BUCKET,
    )

    /**
     * @param wokeApp alarm kapalı uygulamayı uyandırdı mı ([ColdDelivery])
     * Tolerans dışında gelen deneme "ulaştı" sayılmaz: uygulama yeniden açılınca motor vakti geçmiş teslimi geç
     * de olsa yapar; bunu başarı saymak, sınamanın yakalaması gereken arızayı gizler.
     */
    fun evaluate(plannedAt: Instant, deliveredAt: Instant?, now: Instant, wokeApp: Boolean = false): SelfTestResult {
        if (deliveredAt == null) return if (now.isAfter(plannedAt.plus(TIMEOUT))) SelfTestResult.NotArrived else SelfTestResult.Waiting
        val delay = Duration.between(plannedAt, deliveredAt).coerceAtLeast(Duration.ZERO)
        return if (delay > DeliveryGrouping.LATE_TOLERANCE) SelfTestResult.Late(delay) else SelfTestResult.Arrived(delay, wokeApp)
    }

    /**
     * Sınama başarısızsa sırayla bakılacak ayarlar: teslimi etkileyen eksikler ve otomatik başlatma. Otomatik
     * başlatma okunamadığı için Kullanıcı daha önce onaylamış olsa da listede kalır.
     */
    fun likelyCauses(report: HealthReport): List<HealthCheck> =
        HealthCheck.entries.filter { it in DELIVERY_CHECKS && (it in report.toFix || it == HealthCheck.AUTO_START) }
}

/** Kurulum sihirbazının adımları (blueprint D1-4, G3). Sıra gösterim sırasıdır. */
enum class SetupStep(val check: HealthCheck?) {
    NOTIFICATIONS(HealthCheck.NOTIFICATIONS),
    CHANNELS(HealthCheck.CHANNELS),
    EXACT_ALARMS(HealthCheck.EXACT_ALARMS),
    AUTO_START(HealthCheck.AUTO_START),
    BATTERY(HealthCheck.BATTERY),
    FULL_SCREEN(HealthCheck.FULL_SCREEN),

    /** Son uygulamalarda kilit (F1 bulgusu: kilitli uygulama bellek temizliğinden etkilenmiyor). Okunamaz; Kullanıcı onaylar. */
    RECENTS_LOCK(null),
    SELF_TEST(null),
}

data class SetupStepState(val step: SetupStep, val done: Boolean)

data class SetupProgress(val steps: List<SetupStepState>) {
    val remaining: Int get() = steps.count { !it.done }
    val done: Boolean get() = remaining == 0
    val next: SetupStep? get() = steps.firstOrNull { !it.done }?.step

    /** [current] bitince ya da atlanınca sıradaki eksik adım. Geride kalan eksiğe dönülmez; kalmadıysa null. */
    fun after(current: SetupStep): SetupStep? {
        val index = steps.indexOfFirst { it.step == current }
        return if (index < 0) null else steps.drop(index + 1).firstOrNull { !it.done }?.step
    }
}

object SetupWizard {
    /** Her kurulumda gösterilen adımlar; ötekiler (kanal, tam vakit alarm, tam ekran) yalnız eksikse adım olur. */
    private val ALWAYS = setOf(SetupStep.NOTIFICATIONS, SetupStep.AUTO_START, SetupStep.BATTERY, SetupStep.RECENTS_LOCK, SetupStep.SELF_TEST)

    /**
     * Rahatsız Etme erişimi isteğe bağlıdır ve bekleme kovasının çaresi pil adımıdır: ikisi sihirbaza girmez,
     * Hatırlatma Sağlığı ekranında durur. Deneme adımı ancak vaktinde ulaşan denemeyle kapanır.
     */
    fun progress(report: HealthReport, recentsLockConfirmed: Boolean): SetupProgress {
        val states = SetupStep.entries.mapNotNull { step ->
            val missing = step.check?.let { it in report.toFix } ?: false
            if (step !in ALWAYS && !missing) return@mapNotNull null
            val done = when (step) {
                SetupStep.RECENTS_LOCK -> recentsLockConfirmed
                SetupStep.SELF_TEST -> report.lastSelfTest?.outcome?.passed == true
                else -> !missing
            }
            SetupStepState(step, done)
        }
        return SetupProgress(states)
    }
}
