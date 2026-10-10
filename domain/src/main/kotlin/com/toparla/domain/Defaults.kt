package com.toparla.domain

/**
 * Adlandırılmış varsayılanlar. Sihirli sabit kullanılmaz (CLAUDE.md, kod kuralları).
 * Kullanıcı ayarı olanların buradaki değeri yalnız başlangıç değeridir.
 */
object Defaults {
    /** Günlük proaktif bildirim bütçesi (karar 0003). */
    const val DAILY_NOTIFICATION_BUDGET = 10
    const val DAILY_NOTIFICATION_BUDGET_MIN = 10
    const val DAILY_NOTIFICATION_BUDGET_MAX = 20

    /** Konu özetine ayrılan günlük slot sayısı; bütçenin içindedir (K19). */
    const val TOPIC_DIGEST_SLOTS_PER_DAY = 2

    /** Israrlı takip tekrar aralığı, dakika (karar 0003). */
    const val PERSISTENT_REMINDER_INTERVAL_MIN = 30
    val PERSISTENT_REMINDER_INTERVAL_OPTIONS_MIN = listOf(15, 30, 60)

    /** Uyku penceresi başlangıç değerleri, günün dakikası (23:30–07:30); Kullanıcı ayarından değişir. */
    const val SLEEP_START_MINUTE_OF_DAY = 23 * 60 + 30
    const val SLEEP_END_MINUTE_OF_DAY = 7 * 60 + 30

    /** Teslim bu süreden fazla gecikirse "geç teslim" sayılır (kritikte ±1 dk sözü). */
    const val LATE_DELIVERY_TOLERANCE_SEC = 60L

    /** Hatırlatma eklerken hazır zamanlar: "10 dk sonra", "bu akşam" ve "yarın sabah" saatleri. */
    const val QUICK_TIME_SOON_MIN = 10L
    const val QUICK_TIME_EVENING_HOUR = 20
    const val QUICK_TIME_MORNING_HOUR = 9

    /** Kendi kendini sınama: deneme hatırlatmasının gecikmesi ve "gelmedi" demeden önce beklenen süre, saniye. */
    const val SELF_TEST_DELAY_SEC = 20L
    const val SELF_TEST_TIMEOUT_SEC = 60L

    /** Merdiveni biten yanıtsız iş bu kadar dakika sonra "süresi doldu" sayılır (blueprint Bölüm I: +60 dk). */
    const val UNANSWERED_EXPIRY_MIN = 60L

    /** Kritik bekçinin çalışma aralığı, dakika (v3 §10.6; WorkManager'ın en kısa periyodu da 15 dk). */
    const val CRITICAL_WATCHDOG_PERIOD_MIN = 15L

    /** Kritik bekçinin ileriye baktığı süre, dakika (v3 §10.6). */
    const val CRITICAL_WATCHDOG_LOOKAHEAD_MIN = 20L

    /** Hatırlatma penceresinin en geç yeniden doldurulma aralığı, saat. */
    const val MAINTENANCE_INTERVAL_HOURS = 12L
}
