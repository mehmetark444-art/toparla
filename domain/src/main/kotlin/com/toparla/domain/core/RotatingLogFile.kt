package com.toparla.domain.core

import java.io.File
import java.io.IOException

/**
 * Dönen günlük dosyası (blueprint B1 "Timber döner günlük"): `toparla.log` dolunca `toparla.1.log` olur,
 * en eski dosya silinir. Toplam boyut yaklaşık `maxBytes × maxFiles` ile sınırlıdır.
 *
 * Günlüğe gizli değer, bildirim/ekran içeriği ve sağlık verisi **yazılmaz**; bu sınıf süzmez, çağıran yazmaz.
 */
class RotatingLogFile(private val directory: File, private val maxBytes: Long, private val maxFiles: Int) {
    init {
        require(maxBytes > 0) { "maxBytes pozitif olmalı" }
        require(maxFiles >= 1) { "maxFiles en az 1 olmalı" }
    }

    private val current: File get() = File(directory, "$BASE.$EXT")

    /** Satırı ekler. Disk hatası günlüğü durdurur ama uygulamayı çökertmez: false döner. */
    @Synchronized
    @Suppress("SwallowedException") // Günlük yazılamıyorsa hatayı yazacak yer de yoktur; sonuç false ile bildirilir.
    fun append(line: String): Boolean = try {
        if (!directory.exists() && !directory.mkdirs()) throw IOException("günlük klasörü oluşturulamadı")
        if (current.exists() && current.length() >= maxBytes) rotate()
        current.appendText(line.replace(LINE_BREAKS, " ") + "\n")
        true
    } catch (e: IOException) {
        false
    }

    private fun rotate() {
        if (maxFiles == 1) {
            current.delete()
            return
        }
        numbered(maxFiles - 1).delete()
        for (i in maxFiles - 2 downTo 1) {
            val from = numbered(i)
            if (from.exists()) from.renameTo(numbered(i + 1))
        }
        current.renameTo(numbered(1))
    }

    private fun numbered(index: Int) = File(directory, "$BASE.$index.$EXT")

    private companion object {
        const val BASE = "toparla"
        const val EXT = "log"
        val LINE_BREAKS = Regex("[\\r\\n]+")
    }
}
