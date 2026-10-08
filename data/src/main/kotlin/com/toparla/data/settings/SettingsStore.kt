package com.toparla.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

        fun flagKey(flag: FeatureFlag) = booleanPreferencesKey("flag_${flag.name.lowercase()}")
    }
}
