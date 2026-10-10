package com.toparla.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.toparla.domain.Defaults
import com.toparla.domain.FeatureFlag
import com.toparla.domain.SettingsRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

/** Kullanıcı ayarlarının geçerli (kurallardan geçmiş) görünümü. */
data class UserSettings(
    val notificationBudget: Int,
    val persistentIntervalMin: Int,
    val sleepStart: LocalTime,
    val sleepEnd: LocalTime,
)

/**
 * Görünüm ayarlarının ham hâli (Ayarlar → Görünüm). Enum adları olarak saklanır; geçerliliği arayüz katmanı
 * (`AppearanceChoices.parse`) denetler, çünkü tema türleri orada tanımlıdır.
 */
data class AppearanceSettings(
    val themeMode: String?,
    val accent: String?,
    val textSize: String?,
    val reduceMotion: Boolean,
    val haptics: Boolean,
)

/**
 * Ayarlar ve özellik anahtarları (DataStore Preferences). Ham değer her okumada [SettingsRules]'tan geçer;
 * bozuk ya da aralık dışı değer sessizce varsayılana döner.
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<UserSettings> = dataStore.data.map { p ->
        UserSettings(
            notificationBudget = SettingsRules.notificationBudget(p[NOTIFICATION_BUDGET]),
            persistentIntervalMin = SettingsRules.persistentIntervalMin(p[PERSISTENT_INTERVAL_MIN]),
            sleepStart = SettingsRules.timeOfDay(p[SLEEP_START], Defaults.SLEEP_START_MINUTE_OF_DAY),
            sleepEnd = SettingsRules.timeOfDay(p[SLEEP_END], Defaults.SLEEP_END_MINUTE_OF_DAY),
        )
    }

    val appearance: Flow<AppearanceSettings> = dataStore.data.map { p ->
        AppearanceSettings(
            themeMode = p[THEME_MODE],
            accent = p[ACCENT],
            textSize = p[TEXT_SIZE],
            reduceMotion = p[REDUCE_MOTION] ?: false,
            haptics = p[HAPTICS] ?: true,
        )
    }

    /**
     * HyperOS otomatik başlatma izni uygulama içinden okunamaz (F1 bulgusu). Yalnız Kullanıcı sihirbazda "açtım"
     * dediğinde true olur (sınamanın geçmesi kanıt sayılmaz; proje beyni H38).
     */
    val autoStartConfirmed: Flow<Boolean> = dataStore.data.map { it[AUTO_START_CONFIRMED] ?: false }

    suspend fun setAutoStartConfirmed(value: Boolean) {
        dataStore.edit { it[AUTO_START_CONFIRMED] = value }
    }

    /** Süren "Hatırlatmaları sına" denemesinin teslim anahtarı; uygulama kapatılıp açılınca sonuç buradan bulunur. */
    val selfTestKey: Flow<String?> = dataStore.data.map { it[SELF_TEST_KEY] }

    suspend fun setSelfTestKey(value: String?) {
        dataStore.edit { if (value == null) it.remove(SELF_TEST_KEY) else it[SELF_TEST_KEY] = value }
    }

    /** Nabzın en son uyardığı eksikler (`HealthCheck` adları): aynı eksik için yeniden uyarılmaz. */
    val heartbeatWarned: Flow<Set<String>> = dataStore.data.map { it[HEARTBEAT_WARNED].orEmpty() }

    suspend fun setHeartbeatWarned(value: Set<String>) {
        dataStore.edit { it[HEARTBEAT_WARNED] = value }
    }

    fun flag(flag: FeatureFlag): Flow<Boolean> = dataStore.data.map { it[flagKey(flag)] ?: flag.defaultOn }

    suspend fun setNotificationBudget(value: Int) {
        dataStore.edit { it[NOTIFICATION_BUDGET] = SettingsRules.notificationBudget(value) }
    }

    suspend fun setPersistentIntervalMin(value: Int) {
        dataStore.edit { it[PERSISTENT_INTERVAL_MIN] = SettingsRules.persistentIntervalMin(value) }
    }

    suspend fun setSleepWindow(start: LocalTime, end: LocalTime) {
        dataStore.edit {
            it[SLEEP_START] = start.toSecondOfDay() / SECONDS_PER_MINUTE
            it[SLEEP_END] = end.toSecondOfDay() / SECONDS_PER_MINUTE
        }
    }

    suspend fun setFlag(flag: FeatureFlag, on: Boolean) {
        dataStore.edit { it[flagKey(flag)] = on }
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60
        val NOTIFICATION_BUDGET = intPreferencesKey("notification_budget")
        val PERSISTENT_INTERVAL_MIN = intPreferencesKey("persistent_interval_min")
        val SLEEP_START = intPreferencesKey("sleep_start_minute")
        val SLEEP_END = intPreferencesKey("sleep_end_minute")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val TEXT_SIZE = stringPreferencesKey("text_size")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HAPTICS = booleanPreferencesKey("haptics")
        val AUTO_START_CONFIRMED = booleanPreferencesKey("auto_start_confirmed")
        val SELF_TEST_KEY = stringPreferencesKey("self_test_key")
        val HEARTBEAT_WARNED = stringSetPreferencesKey("heartbeat_warned")

        fun flagKey(flag: FeatureFlag) = booleanPreferencesKey("flag_${flag.name.lowercase()}")
    }
}
