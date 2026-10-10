package com.toparla.app.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.toparla.app.MainActivity
import com.toparla.app.R
import com.toparla.domain.core.Clock
import com.toparla.domain.reminder.ReminderAction
import com.toparla.domain.reminder.ReminderEngine
import com.toparla.domain.reminder.ReminderRepository
import com.toparla.reminders.ReminderIntents
import com.toparla.ui.components.PrimaryButton
import com.toparla.ui.components.SecondaryButton
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Kritik hatırlatma kartı (blueprint D25, G1; görsel dil onayı 9 Ekim, `docs/tasarim/2026-10-09-gorsel-dil`).
 * Kilit ekranının üstünde açılır ve ekranı uyandırır. Yalnız bu teslimi gösterir; kilitliyken başka veri görünmez.
 * Kırmızı (`critical`) uygulamada yalnız burada ve kriz ekranında kullanılır.
 */
@AndroidEntryPoint
class ReminderFullScreenActivity : ComponentActivity() {
    @Inject lateinit var engine: ReminderEngine

    @Inject lateinit var repository: ReminderRepository

    @Inject lateinit var clock: Clock

    private var card by mutableStateOf<CardContent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val key = intent.getStringExtra(EXTRA_OCCURRENCE_KEY)
        if (key == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            val occurrence = repository.occurrence(key)
            val info = occurrence?.let { repository.info(it.reminderId) }
            if (occurrence == null || info == null) {
                finish()
            } else {
                card = CardContent(TIME.format(occurrence.plannedAt.atZone(clock.zone())), info.title, info.body)
            }
        }
        setContent {
            ToparlaTheme {
                card?.let { content ->
                    CriticalCard(
                        content = content,
                        onDone = { respond(key, ReminderAction.DONE) },
                        onSnooze = { respond(key, ReminderAction.SNOOZE) },
                    )
                }
            }
        }
    }

    private fun respond(key: String, action: ReminderAction) {
        lifecycleScope.launch {
            engine.onAction(key, action, clock.now())
            finish()
        }
    }

    companion object {
        const val EXTRA_OCCURRENCE_KEY = "occurrenceKey"
        private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

data class CardContent(val time: String, val title: String, val body: String)

/** Üst yarı bilgi, alt yarı eylem; tek baskın eylem "Yaptım" (blueprint C4, C9). */
@Composable
fun CriticalCard(content: CardContent, onDone: () -> Unit, onSnooze: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.screenEdge),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Spacer(Modifier.height(Spacing.xl))
                Text(text = content.time, style = ToparlaTheme.type.displayNow, color = ToparlaTheme.extended.critical)
                Spacer(Modifier.height(Spacing.xs))
                Text(text = content.title, style = ToparlaTheme.type.displayNow)
                if (content.body.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.s))
                    Text(text = content.body, style = ToparlaTheme.type.bodyL, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                PrimaryButton(text = stringResource(R.string.reminder_done), onClick = onDone, critical = true)
                SecondaryButton(text = stringResource(R.string.reminder_snooze), onClick = onSnooze)
            }
        }
    }
}

/** Hatırlatma bildirimlerinin açtığı ekranlar. */
class AppReminderIntents(private val context: Context) : ReminderIntents {
    override fun openApp(): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    override fun fullScreen(occurrenceKey: String): PendingIntent = PendingIntent.getActivity(
        context,
        occurrenceKey.hashCode(),
        Intent(context, ReminderFullScreenActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            .putExtra(ReminderFullScreenActivity.EXTRA_OCCURRENCE_KEY, occurrenceKey),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun openHealth(): PendingIntent = PendingIntent.getActivity(
        context,
        HEALTH_REQUEST,
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_HEALTH)
            // Uygulama açıksa yeni kopya açılmaz; var olan ekran sağlık sayfasına geçer.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val HEALTH_REQUEST = 1
    }
}
