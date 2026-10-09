package com.toparla.app

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toparla.app.nav.ToparlaRoot
import com.toparla.data.settings.SettingsStore
import com.toparla.ui.theme.AppearanceChoices
import com.toparla.ui.theme.ToparlaTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Tek Activity (blueprint B3, F2.24). Kenardan kenara çizer; geri hareketi Android'in öngörülü geri animasyonuyla
 * çalışır (manifest `enableOnBackInvokedCallback`). Görünüm ayarları DataStore'dan gelir.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appearance: AppearanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val choices by appearance.choices.collectAsState()
            ToparlaTheme(
                mode = choices.mode,
                accent = choices.accent,
                textSize = choices.textSize,
                // Sistemde animasyonlar kapalıysa Toparla da azaltır (blueprint C5).
                reduceMotion = choices.reduceMotion || systemAnimationsOff(),
                hapticsEnabled = choices.haptics,
            ) {
                ToparlaRoot()
            }
        }
    }

    private fun systemAnimationsOff(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, ANIMATIONS_ON) == ANIMATIONS_OFF

    private companion object {
        const val ANIMATIONS_ON = 1f
        const val ANIMATIONS_OFF = 0f
    }
}

/** Görünüm ayarlarının ekrana hazır hâli; bozuk değer varsayılana döner (`AppearanceChoices.parse`). */
@HiltViewModel
class AppearanceViewModel @Inject constructor(settings: SettingsStore) : ViewModel() {
    val choices: StateFlow<AppearanceChoices> = settings.appearance
        .map { AppearanceChoices.parse(it.themeMode, it.accent, it.textSize, it.reduceMotion, it.haptics) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceChoices())
}
