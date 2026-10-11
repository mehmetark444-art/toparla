package com.toparla.app.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.toparla.app.R
import com.toparla.app.reminder.FlowHeader
import com.toparla.app.reminder.GroupLabel
import com.toparla.ui.components.SettingRow
import com.toparla.ui.components.ToparlaCard
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, lockAvailable: Boolean, actions: SettingsActions) {
    val ui by viewModel.state.collectAsState()
    SettingsContent(ui, lockAvailable, actions)
}

/**
 * Ayarlar (blueprint D20; onaylı taslak `docs/tasarim/2026-10-11-ilac/Ayarlar.dc.html`). Yalnız çalışan satırlar
 * görünür (karar 0018): Modüller → İlaç takibi (kapalı başlar, K18), açıkken İlaçlar ve kilit; Hatırlatma Sağlığı.
 */
@Composable
fun SettingsContent(ui: SettingsUi, lockAvailable: Boolean, actions: SettingsActions) {
    Column(modifier = Modifier.fillMaxSize()) {
        FlowHeader(stringResource(R.string.settings_title), ToparlaIcons.Back, stringResource(R.string.back), actions.onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenEdge, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            GroupLabel(stringResource(R.string.settings_modules))
            SettingsCard {
                SettingRow(
                    title = stringResource(R.string.settings_medication),
                    description = stringResource(R.string.settings_medication_desc),
                    onClick = { actions.onMedication(!ui.medication) },
                    trailing = { Switch(checked = ui.medication, onCheckedChange = null) },
                )
                if (ui.medication) {
                    RowDivider()
                    SettingRow(title = stringResource(R.string.settings_medications), onClick = actions.onMedications)
                    RowDivider()
                    SettingRow(
                        title = stringResource(R.string.settings_medication_lock),
                        description = stringResource(
                            if (lockAvailable) R.string.settings_medication_lock_desc else R.string.settings_medication_lock_unavailable,
                        ),
                        onClick = if (lockAvailable) ({ actions.onMedicationLock(!ui.medicationLock) }) else null,
                        trailing = { Switch(checked = ui.medicationLock && lockAvailable, onCheckedChange = null, enabled = lockAvailable) },
                    )
                }
            }
            GroupLabel(stringResource(R.string.settings_reminders))
            SettingsCard {
                SettingRow(title = stringResource(R.string.health_title), onClick = actions.onHealth)
            }
            Text(text = stringResource(R.string.settings_medication_note), style = ToparlaTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Ayar satırlarının kartı: satır kendi iç boşluğunu taşıdığı için kartın yan boşluğu dar (toplam 24 dp). */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    ToparlaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = Spacing.xxs)) {
        Column { content() }
    }
}
