package com.toparla.data.secret

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.toparla.domain.SecretStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gizli değerleri Android Keystore'daki AES-256-GCM anahtarıyla şifreleyip dosyada tutar (blueprint B1).
 * Anahtar cihazdan çıkmaz; dosyada yalnız `IV | şifreli metin` durur. Değer adı, GCM'in ek doğrulama verisidir:
 * bir değerin dosyası başka bir adla okunamaz. `security-crypto` kullanılmaz.
 *
 * @param directory uygulamaya özel, yedeğe girmeyen klasör (ör. `noBackupFilesDir/secrets`)
 */
class KeystoreSecretStore(
    private val directory: File,
    private val io: CoroutineDispatcher,
    private val keyAlias: String = DEFAULT_ALIAS,
) : SecretStore {
    override suspend fun put(name: String, value: String) = withContext(io) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(name.toByteArray())
        val encrypted = cipher.doFinal(value.toByteArray())
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Gizli değer klasörü oluşturulamadı")
        // Önce geçici dosya, sonra ad değiştirme: yarım yazılmış dosya eski değeri bozmaz.
        val target = fileFor(name)
        val temp = File(directory, "${target.name}.tmp")
        temp.writeBytes(cipher.iv + encrypted)
        if (!temp.renameTo(target)) throw IOException("Gizli değer yazılamadı")
    }

    override suspend fun get(name: String): String? = withContext(io) {
        val file = fileFor(name)
        if (!file.exists()) return@withContext null
        try {
            val bytes = file.readBytes()
            if (bytes.size <= IV_BYTES) return@withContext null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
            cipher.updateAAD(name.toByteArray())
            String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES))
        } catch (e: GeneralSecurityException) {
            // Anahtar değişmiş ya da dosya bozulmuş: değer yok sayılır, Kullanıcı yeniden girer.
            Timber.w("Gizli değer çözülemedi (%s): %s", name, e.javaClass.simpleName)
            null
        } catch (e: IOException) {
            Timber.w("Gizli değer okunamadı (%s): %s", name, e.javaClass.simpleName)
            null
        }
    }

    override suspend fun remove(name: String) {
        withContext(io) { fileFor(name).delete() }
    }

    private fun fileFor(name: String): File {
        require(NAME.matches(name)) { "Geçersiz gizli değer adı" }
        return File(directory, "$name.bin")
    }

    /** Eşzamanlı ilk kullanımda iki ayrı anahtar üretilmesin diye kilitli. */
    @Synchronized
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        const val DEFAULT_ALIAS = "toparla_secret_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_BITS = 256
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private val NAME = Regex("[a-z0-9_]{1,40}")
    }
}
