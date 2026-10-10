package com.toparla.app.reminder

import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toparla.domain.core.Clock
import com.toparla.domain.reminder.HealthReport
import com.toparla.reminders.HealthProbe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(private val probe: HealthProbe, val clock: Clock) : ViewModel() {
    private val mutable = MutableStateFlow<HealthReport?>(null)
    val state: StateFlow<HealthReport?> = mutable

    /** Ayar sayfasından her dönüşte yeniden okunur; izinler uygulama dışında değişir. */
    fun refresh() {
        viewModelScope.launch { mutable.value = probe.report() }
    }
}
