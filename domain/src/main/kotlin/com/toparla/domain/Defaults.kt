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

    /** Aynı anda aktif koçluk alan alışkanlık üst sınırı (K23). */
    const val MAX_FOCUS_HABITS = 2

    /** Bulut AI aylık bütçe varsayılanı, ABD doları (K21). */
    const val MONTHLY_AI_BUDGET_USD = 25
}
