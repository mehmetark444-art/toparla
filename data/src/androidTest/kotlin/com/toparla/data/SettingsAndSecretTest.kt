package com.toparla.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.toparla.data.core.DefaultDispatcherProvider
import com.toparla.data.db.ToparlaDatabase
import com.toparla.data.secret.KeystoreSecretStore
import com.toparla.data.settings.SettingsStore
import com.toparla.domain.FeatureFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalTime

/** F2.12 (şema dışa aktarımı), F2.13 (ayarlar, özellik anahtarları) ve F2.14 (gizli değer kasası); gerçek cihazda. */
@RunWith(AndroidJUnit4::class)
class SettingsAndSecretTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val io = DefaultDispatcherProvider().io

    @Test
    fun disaAktarilanSemaIlkSurumuKurar() {
        val file = File(context.cacheDir, "schema-test.db").apply { delete() }
        val helper = MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = file,
            driver = BundledSQLiteDriver(),
            databaseClass = ToparlaDatabase::class,
        )
        val connection = runBlocking { helper.createDatabase(ToparlaDatabase.CURRENT_VERSION) }
        val tables = ArrayList<String>()
        connection.prepare("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name").use {
            while (it.step()) tables += it.getText(0)
        }
        connection.close()

        assertTrue(tables.containsAll(listOf("DeliveryLog", "Reminder", "ReminderOccurrence", "ScheduledAlarm")))
    }

    @Test
    fun ayarlarVarsayilanlaBaslarGecersizDegerKuraldanGecer() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "settings-test.preferences_pb").apply { delete() }
        val store = SettingsStore(PreferenceDataStoreFactory.create(scope = scope) { file })

        val initial = store.settings.first()
        assertEquals(10, initial.notificationBudget)
        assertEquals(30, initial.persistentIntervalMin)
        assertEquals(LocalTime.of(23, 30), initial.sleepStart)
        assertFalse(store.flag(FeatureFlag.MEDICATION).first())

        store.setNotificationBudget(99)
        store.setPersistentIntervalMin(45)
        store.setSleepWindow(LocalTime.of(0, 15), LocalTime.of(8, 0))
        store.setFlag(FeatureFlag.MEDICATION, true)

        val changed = store.settings.first()
        assertEquals(20, changed.notificationBudget)
        assertEquals(30, changed.persistentIntervalMin)
        assertEquals(LocalTime.of(0, 15), changed.sleepStart)
        assertEquals(LocalTime.of(8, 0), changed.sleepEnd)
        assertTrue(store.flag(FeatureFlag.MEDICATION).first())
        assertFalse(store.flag(FeatureFlag.TRUSTED_CONTACT).first())
        scope.cancel()
    }

    @Test
    fun gizliDegerSifreliYazilirGeriOkunurSilinir() = runBlocking {
        val dir = File(context.noBackupFilesDir, "secrets-test").apply { deleteRecursively() }
        val store = KeystoreSecretStore(dir, io, keyAlias = "toparla_secret_test")
        val secret = "deneme-degeri-12345"

        assertNull(store.get("gemini_api_key"))
        store.put("gemini_api_key", secret)

        assertEquals(secret, store.get("gemini_api_key"))
        val onDisk = File(dir, "gemini_api_key.bin").readBytes()
        assertFalse("düz metin diskte görünmemeli", String(onDisk, Charsets.ISO_8859_1).contains(secret))

        // Aynı değer yeniden yazılınca şifreli hâli değişir (her yazmada yeni IV).
        store.put("gemini_api_key", secret)
        assertNotEquals(onDisk.toList(), File(dir, "gemini_api_key.bin").readBytes().toList())

        store.remove("gemini_api_key")
        assertNull(store.get("gemini_api_key"))
    }

    @Test
    fun bozulmusYaDaBaskaAdaTasinmisDosyaCozulmezNullDoner() = runBlocking {
        val dir = File(context.noBackupFilesDir, "secrets-test").apply { deleteRecursively() }
        val store = KeystoreSecretStore(dir, io, keyAlias = "toparla_secret_test")
        store.put("a", "birinci")

        // Dosya başka bir ada kopyalanırsa (ad, doğrulama verisinin parçası) çözülmez.
        File(dir, "a.bin").copyTo(File(dir, "b.bin"))
        assertNull(store.get("b"))

        // Tek bayt değişince bütünlük denetimi tutmaz.
        val file = File(dir, "a.bin")
        val bytes = file.readBytes()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1] + 1).toByte()
        file.writeBytes(bytes)
        assertNull(store.get("a"))

        file.writeBytes(ByteArray(4))
        assertNull(store.get("a"))
    }

    @Test
    fun gecersizAdReddedilir() {
        val store = KeystoreSecretStore(File(context.noBackupFilesDir, "secrets-test"), io, keyAlias = "toparla_secret_test")

        assertThrows(IllegalArgumentException::class.java) { runBlocking { store.get("../kacis") } }
    }
}
