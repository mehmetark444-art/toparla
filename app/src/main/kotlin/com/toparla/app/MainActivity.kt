package com.toparla.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toparla.app.nav.ToparlaRoot
import com.toparla.data.settings.SettingsStore
import com.toparla.ui.theme.AppearanceChoices
import com.toparla.ui.theme.ThemeMode
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

    /** Nabız uyarısına dokunularak gelindiyse Hatırlatma Sağlığı açılır; açılınca istek tüketilir. */
    private val openHealth = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // İlk kare pencere zemini gibi koyu (themes.xml); ayar okununca aşağıda temaya göre yeniden kurulur.
        val initial = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = initial, navigationBarStyle = initial)
        super.onCreate(savedInstanceState)
        // Yeniden oluşturmada (ör. tema değişimi) eski niyet yeniden işlenmez.
        if (savedInstanceState == null) openHealth.value = intent?.action == ACTION_OPEN_HEALTH
        askNotificationPermission()
        setContent {
            val choices by appearance.choices.collectAsState()
            val darkBars = choices.mode != ThemeMode.LIGHT
            // Durum çubuğu simgeleri telefonun değil uygulamanın temasına uysun; yoksa varsayılan koyu temada,
            // telefon açık temadayken saat ve pil koyu zeminde koyu çizilir (9 Ekim gece kontrolü).
            DisposableEffect(darkBars) {
                val style = if (darkBars) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            ToparlaTheme(
                mode = choices.mode,
                accent = choices.accent,
                textSize = choices.textSize,
                // Sistemde animasyonlar kapalıysa Toparla da azaltır (blueprint C5).
                reduceMotion = choices.reduceMotion || systemAnimationsOff(),
                hapticsEnabled = choices.haptics,
            ) {
                ToparlaRoot(openHealth = openHealth.value, onHealthOpened = { openHealth.value = false })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_OPEN_HEALTH) openHealth.value = true
    }

    /**
     * Bildirim izni olmadan hiçbir hatırlatma görünmez; ilk açılışta sorulur (blueprint B5). Reddedilirse sistem
     * bir daha sormaz: durum ve düzeltme yolu Hatırlatma Sağlığı ekranındadır (F2.35).
     */
    private fun askNotificationPermission() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun systemAnimationsOff(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, ANIMATIONS_ON) == ANIMATIONS_OFF

    companion object {
        const val ACTION_OPEN_HEALTH = "com.toparla.app.OPEN_HEALTH"
        private const val ANIMATIONS_ON = 1f
        private const val ANIMATIONS_OFF = 0f
    }
}

/** Görünüm ayarlarının ekrana hazır hâli; bozuk değer varsayılana döner (`AppearanceChoices.parse`). */
@HiltViewModel
class AppearanceViewModel @Inject constructor(settings: SettingsStore) : ViewModel() {
    val choices: StateFlow<AppearanceChoices> = settings.appearance
        .map { AppearanceChoices.parse(it.themeMode, it.accent, it.textSize, it.reduceMotion, it.haptics) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceChoices())
}
