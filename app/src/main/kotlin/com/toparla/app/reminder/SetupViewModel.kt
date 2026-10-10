package com.toparla.app.reminder

import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toparla.data.settings.SettingsStore
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.HealthReport
import com.toparla.domain.reminder.SetupProgress
import com.toparla.domain.reminder.SetupStep
import com.toparla.domain.reminder.SetupWizard
import com.toparla.reminders.HealthProbe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sihirbazın ekran durumu.
 *
 * @param step gösterilen adım; null ise genel bakış ya da ([finished]) bitiş ekranı
 * @param single Hatırlatma Sağlığı ya da sınama rehberinden tek adım için açıldı: adım bitince kapanır
 * @param pageOpened okunamayan ayarda (otomatik başlatma) ayar sayfası açıldı: eylem "Açtım"a döner
 * @param firstTime ilk açılıştaki kendiliğinden gösterim ("Başlayalım"); sonrakilerde "Sürdür"
 */
data class SetupUi(
    val progress: SetupProgress? = null,
    val report: HealthReport? = null,
    val step: SetupStep? = null,
    val single: Boolean = false,
    val pageOpened: Boolean = false,
    val finished: Boolean = false,
    val firstTime: Boolean = false,
)

enum class SetupEffect { CLOSE }

/**
 * Kurulum sihirbazı (F2.37, blueprint D1-4 ve G3). Adımların hangileri olduğu ve sırası [SetupWizard]'dadır; burada
 * yalnız "şu an hangi adım gösteriliyor" tutulur. Okunabilen ayar, ayar sayfasından dönüşte kendiliğinden tamam
 * sayılır; okunamayan iki ayarı (otomatik başlatma, uygulama kilidi) yalnız Kullanıcı onaylar (proje beyni H38).
 */
@HiltViewModel
class SetupViewModel @Inject constructor(private val probe: HealthProbe, private val settings: SettingsStore) : ViewModel() {
    private val mutable = MutableStateFlow(SetupUi())
    val state: StateFlow<SetupUi> = mutable
    private val effectChannel = Channel<SetupEffect>(Channel.BUFFERED)
    val effects: Flow<SetupEffect> = effectChannel.receiveAsFlow()

    /** İlk açılışta bir kez: kurulum eksikse sihirbaz kendiliğinden gösterilir. Sonrası Şimdi ekranındaki karttandır. */
    suspend fun shouldIntroduce(): Boolean {
        if (settings.setupIntroSeen.first()) return false
        settings.setSetupIntroSeen(true)
        val remaining = load().progress?.remaining ?: 0
        if (remaining > 0) mutable.update { it.copy(step = null, single = false, finished = false, firstTime = true) }
        return remaining > 0
    }

    /** Sınama kapandıktan sonra: kurulum az önce tamamlandıysa bitiş ekranı bir kez gösterilir. */
    suspend fun shouldCelebrate(): Boolean {
        val done = load().progress?.done == true && !settings.setupDoneSeen.first()
        if (done) mutable.update { it.copy(step = null, single = false, finished = true) }
        return done
    }

    fun openOverview() {
        mutable.update { it.copy(step = null, single = false, pageOpened = false, finished = false, firstTime = false) }
        refresh()
    }

    fun openSingle(step: SetupStep) {
        mutable.update { it.copy(step = step, single = true, pageOpened = false, finished = false) }
        refresh()
    }

    /** Ayar sayfasından her dönüşte: okunabilen ayar yerine geldiyse adım kendiliğinden biter. */
    fun refresh() {
        viewModelScope.launch {
            val ui = load()
            val check = ui.step?.check ?: return@launch
            if (check != HealthCheck.AUTO_START && check in ui.report?.ok.orEmpty()) advance(ui)
        }
    }

    fun start() {
        val next = mutable.value.progress?.next
        if (next == null) viewModelScope.launch { advance(load()) } else mutable.update { it.copy(step = next, pageOpened = false) }
    }

    fun pageOpened() = mutable.update { it.copy(pageOpened = true) }

    /** Okunamayan ayarın Kullanıcı onayı. */
    fun confirm() {
        viewModelScope.launch {
            when (mutable.value.step) {
                SetupStep.AUTO_START -> settings.setAutoStartConfirmed(true)
                SetupStep.RECENTS_LOCK -> settings.setRecentsLockConfirmed(true)
                else -> Unit
            }
            advance(load())
        }
    }

    /** "Şimdi değil": adım eksik kalır, sıradakine geçilir. */
    fun skip() {
        viewModelScope.launch { advance(mutable.value) }
    }

    fun finish() {
        viewModelScope.launch {
            settings.setSetupDoneSeen(true)
            effectChannel.send(SetupEffect.CLOSE)
        }
    }

    private suspend fun load(): SetupUi {
        val report = probe.report()
        val progress = SetupWizard.progress(report, settings.recentsLockConfirmed.first())
        return mutable.updateAndGet { it.copy(report = report, progress = progress) }
    }

    private suspend fun advance(ui: SetupUi) {
        val next = ui.step?.let { ui.progress?.after(it) }
        when {
            ui.single -> effectChannel.send(SetupEffect.CLOSE)
            next != null -> mutable.update { it.copy(step = next, pageOpened = false) }
            ui.progress?.done == true -> mutable.update { it.copy(step = null, finished = true) }
            // Atlanan adımlar kaldı: sihirbaz kapanır, kalanlar Şimdi ekranındaki kartta bekler.
            else -> effectChannel.send(SetupEffect.CLOSE)
        }
    }

    private fun MutableStateFlow<SetupUi>.updateAndGet(change: (SetupUi) -> SetupUi): SetupUi {
        update(change)
        return value
    }
}
