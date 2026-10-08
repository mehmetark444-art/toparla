package com.toparla.domain

import java.time.LocalTime

/**
 * Özellik anahtarları (blueprint B4): bitmemiş ya da kapalı başlayan özellikler bunların arkasındadır.
 * Yalnız kullanılan anahtar eklenir; yenisi ait olduğu fazda gelir.
 */
enum class FeatureFlag(val defaultOn: Boolean) {
    /** M9 İlaç Takibi: kodlanır, kapalı başlar. */
    MEDICATION(false),

    /** M15 Güvenilir Kişi: merdivenin son basamağını açar. */
    TRUSTED_CONTACT(false),

    /** F3 Günlük sürücü: F2'nin 7 günlük sınaması sürerken kapalı yazılır (karar 0005-4). */
    DAILY_DRIVER(false),
}

/** Kullanıcı ayarlarının geçerlilik kuralları: saklanan ham değer buradan geçmeden kullanılmaz. */
object SettingsRules {
    private const val MINUTES_PER_DAY = 24 * 60

    fun notificationBudget(stored: Int?): Int =
        (stored ?: Defaults.DAILY_NOTIFICATION_BUDGET).coerceIn(Defaults.DAILY_NOTIFICATION_BUDGET_MIN, Defaults.DAILY_NOTIFICATION_BUDGET_MAX)

    fun persistentIntervalMin(stored: Int?): Int =
        stored?.takeIf { it in Defaults.PERSISTENT_REMINDER_INTERVAL_OPTIONS_MIN } ?: Defaults.PERSISTENT_REMINDER_INTERVAL_MIN

    /** Gün içi saat, "günün dakikası" olarak saklanır (0..1439). */
    fun timeOfDay(storedMinuteOfDay: Int?, defaultMinuteOfDay: Int): LocalTime {
        val minute = storedMinuteOfDay?.takeIf { it in 0 until MINUTES_PER_DAY } ?: defaultMinuteOfDay
        return LocalTime.ofSecondOfDay(minute * 60L)
    }
}

/**
 * Gizli değer kasası (blueprint B1: Android Keystore, AES-GCM). API anahtarı gibi değerler yalnız buradan
 * okunur; günlüğe, yedeğe ve buluta girmez.
 */
interface SecretStore {
    suspend fun put(name: String, value: String)

    /** Değer yoksa ya da çözülemiyorsa (anahtar değişmiş, dosya bozulmuş) null. */
    suspend fun get(name: String): String?

    suspend fun remove(name: String)
}
