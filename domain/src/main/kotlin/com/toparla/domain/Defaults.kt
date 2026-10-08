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

    /** Teslim bu süreden fazla gecikirse "geç teslim" sayılır (kritikte ±1 dk sözü). */
    const val LATE_DELIVERY_TOLERANCE_SEC = 60L

    /** Kritik bekçinin ileriye baktığı süre, dakika (v3 §10.6). */
    const val CRITICAL_WATCHDOG_LOOKAHEAD_MIN = 20L

    /** Hatırlatma penceresinin en geç yeniden doldurulma aralığı, saat. */
    const val MAINTENANCE_INTERVAL_HOURS = 12L
}
