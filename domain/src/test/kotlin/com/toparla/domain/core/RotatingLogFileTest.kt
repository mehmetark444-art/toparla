package com.toparla.domain.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RotatingLogFileTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `satirlar sirayla tek dosyaya eklenir`() {
        val log = RotatingLogFile(dir, maxBytes = 1000, maxFiles = 3)

        log.append("bir")
        log.append("iki")

        assertEquals(listOf("bir", "iki"), File(dir, "toparla.log").readLines())
    }

    @Test
    fun `sinir asilinca dosya doner en eski dosya silinir`() {
        val log = RotatingLogFile(dir, maxBytes = 20, maxFiles = 3)

        repeat(12) { log.append("satir-numara-$it") } // her satır ~15 bayt: her iki satırda bir döner

        val names = dir.listFiles().orEmpty().map { it.name }.sorted()
        assertEquals(listOf("toparla.1.log", "toparla.2.log", "toparla.log"), names)
        assertTrue(File(dir, "toparla.log").readText().contains("satir-numara-11"))
        // En eski satırlar artık hiçbir dosyada yok.
        assertFalse(dir.listFiles().orEmpty().any { it.readText().contains("satir-numara-0\n") })
    }

    @Test
    fun `klasor yoksa olusturulur ve toplam boyut sinirli kalir`() {
        val nested = File(dir, "a/b")
        val log = RotatingLogFile(nested, maxBytes = 100, maxFiles = 2)

        repeat(200) { log.append("x".repeat(30)) }

        val total = nested.listFiles().orEmpty().sumOf { it.length() }
        assertTrue(total <= 2 * (100 + 31), "toplam $total")
    }

    @Test
    fun `satir sonu iceren ileti tek satira indirilir`() {
        val log = RotatingLogFile(dir, maxBytes = 1000, maxFiles = 2)

        log.append("ilk\nikinci\r\nucuncu")

        assertEquals(1, File(dir, "toparla.log").readLines().size)
    }
}
