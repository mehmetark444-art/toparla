package com.toparla.app.medication

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toparla.data.db.MedicationStore
import com.toparla.data.settings.SettingsStore
import com.toparla.domain.FeatureFlag
import com.toparla.domain.core.Clock
import com.toparla.domain.core.IdGenerator
import com.toparla.domain.medication.DoseHistory
import com.toparla.domain.medication.DoseRow
import com.toparla.domain.medication.DoseState
import com.toparla.domain.medication.DoubleDoseCheck
import com.toparla.domain.medication.Medication
import com.toparla.domain.medication.MedicationForm
import com.toparla.domain.medication.MedicationInput
import com.toparla.domain.medication.MedicationRecorder
import com.toparla.domain.medication.TodayDose
import com.toparla.domain.reminder.ReminderAction
import com.toparla.domain.reminder.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

/** Geçmişin bir satırı: dozun durumu ve ilacın adı (silinmiş ilacın adı da). */
data class HistoryRow(val row: DoseRow, val name: String)

/** Çift doz sorusu (blueprint M9): hangi doz, son kayıttan bu yana geçen süre. */
data class DoubleDosePrompt(val occurrenceKey: String, val since: Duration)

/** Plan sekmesindeki giriş kartının özeti: sıradaki doz ve bugün kaydedilmeyen doz sayısı. */
data class MedicationSummary(val next: LocalTime?, val notLogged: Int)

data class MedicationsUi(
    val loaded: Boolean = false,
    val enabled: Boolean = false,
    val locked: Boolean = false,
    val medications: List<Medication> = emptyList(),
    val today: List<TodayDose> = emptyList(),
    val history: List<HistoryRow> = emptyList(),
    val prompt: DoubleDosePrompt? = null,
) {
    val summary: MedicationSummary
        get() = MedicationSummary(
            next = today.firstOrNull { it.row.state == DoseState.UPCOMING }?.time,
            notLogged = today.count { it.row.state == DoseState.NOT_LOGGED },
        )
}

/**
 * İlaç ekranlarının (liste, geçmiş, dışa aktarım, kilit) durumu. Kayıt yalnız motordan yazılır (`ReminderEngine`);
 * ekran "Aldım" demeden önce çift dozu kendisi sorar (bildirimdeki soru yerine uygulama içi diyalog).
 */
@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val store: MedicationStore,
    private val engine: ReminderEngine,
    private val settings: SettingsStore,
    private val exporter: MedicationExporter,
    private val lock: MedicationLock,
    val clock: Clock,
) : ViewModel() {
    private val recorder = MedicationRecorder(store)
    private val since: Instant = clock.now().atZone(clock.zone()).toLocalDate().minusDays(HISTORY_DAYS).atStartOfDay(clock.zone()).toInstant()
    private val tick = MutableStateFlow(0)
    private val prompt = MutableStateFlow<DoubleDosePrompt?>(null)
    private val unlocked = MutableStateFlow(false)
    private val names = HashMap<String, String>()

    /** Son dışa aktarımın sonucu: true kaydedildi, false kaydedilemedi; gösterilince temizlenir. */
    val exportResult = MutableStateFlow<Boolean?>(null)

    private val gate = combine(settings.flag(FeatureFlag.MEDICATION), settings.medicationLock, unlocked, prompt) { on, wantsLock, open, p ->
        Gate(on, wantsLock && !open && lock.available(), p)
    }

    private val data = combine(store.observeMedications(), store.observePlannedDoses(since), store.observeLogsSince(since), tick) { meds, doses, logs, _ ->
        Triple(meds, doses, logs)
    }

    val state: StateFlow<MedicationsUi> = combine(gate, data) { g, (meds, doses, logs) ->
        val now = clock.now()
        meds.forEach { names[it.id] = it.name }
        val history = DoseHistory.rows(doses, logs, now).map { HistoryRow(it, nameOf(it.medicationId)) }
        MedicationsUi(
            loaded = true,
            enabled = g.enabled,
            locked = g.locked,
            medications = meds,
            today = DoseHistory.today(meds, doses, logs, now, clock.zone()),
            history = history,
            prompt = g.prompt,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), MedicationsUi())

    private data class Gate(val enabled: Boolean, val locked: Boolean, val prompt: DoubleDosePrompt?)

    private suspend fun nameOf(id: String): String = names[id] ?: store.nameOf(id).orEmpty().also { names[id] = it }

    /** Saat ilerledikçe durumlar ("Sırada" → "Bekliyor" → "Kaydedilmedi") ekrana dönüşte yeniden hesaplanır. */
    fun refresh() = tick.update { it + 1 }

    fun unlock() {
        unlocked.value = true
    }

    /** Uygulama arka plana gidince kilit yeniden kapanır. */
    fun relock() {
        unlocked.value = false
    }

    /** "Aldım": asgari aralık dolmadıysa önce sorulur. */
    fun take(row: DoseRow) {
        val key = row.occurrenceKey ?: return
        viewModelScope.launch {
            when (val check = recorder.check(row.medicationId, clock.now())) {
                DoubleDoseCheck.Clear -> engine.onAction(key, ReminderAction.DONE, clock.now())
                is DoubleDoseCheck.TooSoon -> prompt.value = DoubleDosePrompt(key, check.since)
            }
        }
    }

    fun confirmTaken() {
        val key = prompt.value?.occurrenceKey ?: return
        prompt.value = null
        viewModelScope.launch { engine.onAction(key, ReminderAction.CONFIRM_TAKEN, clock.now()) }
    }

    fun dismissPrompt() {
        prompt.value = null
    }

    /** "Geç aldım": kayıt geç alındı olarak düşer; telafi dozu önerilmez. */
    fun logLate(row: DoseRow) = act(row, ReminderAction.CONFIRM_TAKEN)

    fun skip(row: DoseRow) = act(row, ReminderAction.NOT_TODAY)

    private fun act(row: DoseRow, action: ReminderAction) {
        val key = row.occurrenceKey ?: return
        viewModelScope.launch { engine.onAction(key, action, clock.now()) }
    }

    fun export(range: ExportRange, format: ExportFormat, target: Uri) {
        viewModelScope.launch { exportResult.value = exporter.write(range, format, target) }
    }

    fun exportShown() {
        exportResult.value = null
    }

    fun suggestedName(format: ExportFormat): String = exporter.suggestedName(format)

    /** Sistemin parmak izi / yüz / ekran kilidi penceresi; başarılıysa ekranlar açılır. */
    fun requestUnlock(activity: Activity) = lock.prompt(activity, ::unlock)

    private companion object {
        const val STOP_AFTER_MS = 5_000L
        const val HISTORY_DAYS = 30L
    }
}

/** İlaç formunun ekrandaki hâli. */
data class MedicationFormUi(
    val id: String? = null,
    val input: MedicationInput = EMPTY_INPUT,
    val problems: Set<MedicationForm.Problem> = emptySet(),
    val loaded: Boolean = false,
) {
    val editing: Boolean get() = id != null
    val everyDay: Boolean get() = input.days.size == DayOfWeek.entries.size

    companion object {
        val EMPTY_INPUT = MedicationInput(
            name = "", doseText = "", times = emptyList(), days = DayOfWeek.entries.toSet(), minGapHours = DEFAULT_GAP_HOURS,
            stock = "", perDose = "", lowStock = "", notes = "", hideName = false,
        )
        private const val DEFAULT_GAP_HOURS = 4
    }
}

/** Formun eylemleri (durumsuz içerik bunları çağırır; ekran görüntüsü testi boş eylemlerle çizer). */
data class MedicationFormActions(
    val onClose: () -> Unit,
    val onEdit: ((MedicationInput) -> MedicationInput) -> Unit,
    val onAddTime: () -> Unit,
    val onRemoveTime: (LocalTime) -> Unit,
    val onEveryDay: (Boolean) -> Unit,
    val onToggleDay: (DayOfWeek) -> Unit,
    val onDelete: () -> Unit,
    val onSave: () -> Unit,
) {
    companion object {
        val None = MedicationFormActions({}, {}, {}, {}, {}, {}, {}, {})
    }
}

@HiltViewModel
class MedicationFormViewModel @Inject constructor(
    private val store: MedicationStore,
    private val engine: ReminderEngine,
    private val ids: IdGenerator,
    private val clock: Clock,
) : ViewModel() {
    private val mutable = MutableStateFlow(MedicationFormUi())
    val state: StateFlow<MedicationFormUi> = mutable
    private var existing: Medication? = null

    /** Yeni ilaç için boş form; düzenlemede kayıtlı ilaç. */
    fun open(id: String?) {
        existing = null
        mutable.value = MedicationFormUi(id = id, loaded = id == null)
        if (id == null) return
        viewModelScope.launch {
            val med = store.medication(id)
            existing = med
            mutable.value = if (med == null) MedicationFormUi(loaded = true) else MedicationFormUi(id = id, input = MedicationInput.of(med), loaded = true)
        }
    }

    fun edit(change: (MedicationInput) -> MedicationInput) = mutable.update { it.copy(input = change(it.input), problems = emptySet()) }

    fun addTime(time: LocalTime) = edit { it.copy(times = Medication.normalizeTimes(it.times + time)) }

    fun removeTime(time: LocalTime) = edit { it.copy(times = it.times - time) }

    fun everyDay(on: Boolean) = edit { it.copy(days = if (on) DayOfWeek.entries.toSet() else emptySet()) }

    fun toggleDay(day: DayOfWeek) = edit { it.copy(days = if (day in it.days) it.days - day else it.days + day) }

    fun save(onSaved: () -> Unit) {
        val ui = mutable.value
        val now = clock.now()
        val today = now.atZone(clock.zone()).toLocalDate()
        when (val result = MedicationForm.build(ui.id ?: ids.newId(), ui.input, today, clock.zone(), existing)) {
            is MedicationForm.Result.Invalid -> mutable.update { it.copy(problems = result.problems) }
            is MedicationForm.Result.Ok -> viewModelScope.launch {
                store.save(result.medication, now)
                engine.replan(now)
                onSaved()
            }
        }
    }

    /** Kullanıcı'nın isteğiyle: ilaç ve hatırlatmaları kalkar, kayıtları kalır. */
    fun delete(onDeleted: () -> Unit) {
        val id = mutable.value.id ?: return
        viewModelScope.launch {
            store.delete(id, clock.now())
            engine.replan(clock.now())
            onDeleted()
        }
    }
}

data class SettingsUi(val medication: Boolean = false, val medicationLock: Boolean = false)

/** Ayarlar'ın gezinme eylemleri. */
data class SettingsActions(
    val onBack: () -> Unit,
    val onMedication: (Boolean) -> Unit,
    val onMedicationLock: (Boolean) -> Unit,
    val onMedications: () -> Unit,
    val onHealth: () -> Unit,
)

/** Ayarlar → Modüller (blueprint D20, K18): ilaç modülü kapalı başlar; açılıp kapanınca pencere yeniden planlanır. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val engine: ReminderEngine,
    private val lock: MedicationLock,
    private val clock: Clock,
) : ViewModel() {
    /** Telefonda ekran kilidi yoksa ilaç kilidi sunulmaz. */
    fun lockAvailable(): Boolean = lock.available()

    val state: StateFlow<SettingsUi> = combine(settings.flag(FeatureFlag.MEDICATION), settings.medicationLock) { on, lock -> SettingsUi(on, lock) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUi())

    /** Açılınca ilaç dozları kurulur; kapanınca kurulu dozlar iptal olur ve açık doz kapanır (veri silinmez). */
    fun setMedication(on: Boolean) {
        viewModelScope.launch {
            settings.setFlag(FeatureFlag.MEDICATION, on)
            engine.replan(clock.now())
        }
    }

    fun setMedicationLock(on: Boolean) {
        viewModelScope.launch { settings.setMedicationLock(on) }
    }
}
