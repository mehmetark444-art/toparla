package com.toparla.ui.theme

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
internal fun rememberViewHaptics(enabled: Boolean): ToparlaHaptics {
    val view = LocalView.current
    return remember(view, enabled) { if (enabled) ViewHaptics(view) else NoHaptics }
}

private class ViewHaptics(private val view: View) : ToparlaHaptics {
    override fun confirm() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    override fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}
