package com.toparla.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.toparla.ui.preview.GunesSkeletonGallery
import com.toparla.ui.preview.NowSkeletonGallery
import com.toparla.ui.theme.ThemeMode
import com.toparla.ui.theme.ToparlaTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * F2.24: gezinme iskeleti, telefon boyunda (412 × 915 dp). Onaylı taslakla
 * (`docs/tasarim/2026-10-09-ikon-iskelet/`) karşılaştırılır.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xhdpi")
class NavigationScreenshotTest {

    @Test fun iskelet_simdi_koyu() = capture("iskelet_simdi", ThemeMode.DARK, NORMAL) { NowSkeletonGallery() }

    @Test fun iskelet_simdi_acik_iki_kat() = capture("iskelet_simdi", ThemeMode.LIGHT, DOUBLE) { NowSkeletonGallery() }

    @Test fun iskelet_gunes_koyu() = capture("iskelet_gunes", ThemeMode.DARK, NORMAL) { GunesSkeletonGallery() }

    @Test fun iskelet_gunes_amoled() = capture("iskelet_gunes", ThemeMode.AMOLED, NORMAL) { GunesSkeletonGallery() }

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
