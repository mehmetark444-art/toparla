package com.toparla.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.toparla.ui.preview.ActionGallery
import com.toparla.ui.preview.RowGallery
import com.toparla.ui.theme.ThemeMode
import com.toparla.ui.theme.ToparlaTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * F2.23: ortak bileşenlerin ekran görüntüsü (3 tema × 2 yazı ölçeği; blueprint C7, `android.md` madde 5).
 * Görüntüler `src/test/screenshots/` altına yazılır; taslakla (`docs/tasarim/2026-10-09-gorsel-dil/`)
 * karşılaştırılır. Kayıt: `recordRoborazziDebug`, karşılaştırma: `verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h2000dp-xhdpi")
class ComponentScreenshotTest {

    @Test fun eylemler_koyu_normal() = capture("eylemler", ThemeMode.DARK, NORMAL) { ActionGallery() }

    @Test fun eylemler_acik_normal() = capture("eylemler", ThemeMode.LIGHT, NORMAL) { ActionGallery() }

    @Test fun eylemler_amoled_normal() = capture("eylemler", ThemeMode.AMOLED, NORMAL) { ActionGallery() }

    @Test fun eylemler_koyu_iki_kat() = capture("eylemler", ThemeMode.DARK, DOUBLE) { ActionGallery() }

    @Test fun eylemler_acik_iki_kat() = capture("eylemler", ThemeMode.LIGHT, DOUBLE) { ActionGallery() }

    @Test fun eylemler_amoled_iki_kat() = capture("eylemler", ThemeMode.AMOLED, DOUBLE) { ActionGallery() }

    @Test fun satirlar_koyu_normal() = capture("satirlar", ThemeMode.DARK, NORMAL) { RowGallery() }

    @Test fun satirlar_acik_normal() = capture("satirlar", ThemeMode.LIGHT, NORMAL) { RowGallery() }

    @Test fun satirlar_amoled_normal() = capture("satirlar", ThemeMode.AMOLED, NORMAL) { RowGallery() }

    @Test fun satirlar_koyu_iki_kat() = capture("satirlar", ThemeMode.DARK, DOUBLE) { RowGallery() }

    @Test fun satirlar_acik_iki_kat() = capture("satirlar", ThemeMode.LIGHT, DOUBLE) { RowGallery() }

    @Test fun satirlar_amoled_iki_kat() = capture("satirlar", ThemeMode.AMOLED, DOUBLE) { RowGallery() }

    private fun capture(name: String, mode: ThemeMode, fontScale: Float, content: @Composable () -> Unit) {
        val scaleName = if (fontScale == NORMAL) "yazi100" else "yazi200"
        captureRoboImage(filePath = "src/test/screenshots/${name}_${mode.name.lowercase()}_$scaleName.png") {
            val system = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = system.density, fontScale = fontScale)) {
                ToparlaTheme(mode = mode, hapticsEnabled = false) { content() }
            }
        }
    }

    private companion object {
        const val NORMAL = 1f
        const val DOUBLE = 2f
    }
}
