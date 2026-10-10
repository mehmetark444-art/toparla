package com.toparla.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.toparla.app.reminder.AppReminderIntents
import com.toparla.data.core.DefaultDispatcherProvider
import com.toparla.data.core.SecureRandomSource
import com.toparla.data.core.SystemClock
import com.toparla.data.core.UuidGenerator
import com.toparla.data.db.DeliveryInsights
import com.toparla.data.db.ReminderStore
import com.toparla.data.db.ToparlaDatabase
import com.toparla.data.secret.KeystoreSecretStore
import com.toparla.data.settings.SettingsStore
import com.toparla.domain.SecretStore
import com.toparla.domain.core.Clock
import com.toparla.domain.core.DispatcherProvider
import com.toparla.domain.core.IdGenerator
import com.toparla.domain.core.RandomSource
import com.toparla.reminders.ReminderIntents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/** Uygulama ömrü boyunca tek olan altyapı nesneleri (blueprint B1: çekirdek arayüzler enjekte edilir). */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val SETTINGS_FILE = "settings"
    private const val SECRETS_DIR = "secrets"

    @Provides @Singleton
    fun clock(): Clock = SystemClock()

    @Provides @Singleton
    fun idGenerator(): IdGenerator = UuidGenerator()

    @Provides @Singleton
    fun randomSource(): RandomSource = SecureRandomSource()

    @Provides @Singleton
    fun dispatchers(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides @Singleton
    fun database(@ApplicationContext context: Context, dispatchers: DispatcherProvider): ToparlaDatabase =
        ToparlaDatabase.open(context, dispatchers.io)

    @Provides @Singleton
    fun reminderStore(db: ToparlaDatabase): ReminderStore = ReminderStore(db)

    @Provides @Singleton
    fun deliveryInsights(db: ToparlaDatabase): DeliveryInsights = DeliveryInsights(db)

    @Provides @Singleton
    fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(SETTINGS_FILE) }

    @Provides @Singleton
    fun settingsStore(dataStore: DataStore<Preferences>): SettingsStore = SettingsStore(dataStore)

    /** Bildirimlerin açtığı ekranlar bu modüldedir; `:reminders` yalnız arayüzü bilir. */
    @Provides @Singleton
    fun reminderIntents(@ApplicationContext context: Context): ReminderIntents = AppReminderIntents(context)

    /** Gizli değerler yedeğe girmeyen klasörde durur (`noBackupFilesDir`). */
    @Provides @Singleton
    fun secretStore(@ApplicationContext context: Context, dispatchers: DispatcherProvider): SecretStore =
        KeystoreSecretStore(File(context.noBackupFilesDir, SECRETS_DIR), dispatchers.io)
}
