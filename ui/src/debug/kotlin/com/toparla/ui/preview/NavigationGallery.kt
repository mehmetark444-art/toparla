package com.toparla.ui.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.toparla.ui.R
import com.toparla.ui.components.CaptureActions
import com.toparla.ui.components.EmptyShape
import com.toparla.ui.components.EmptyState
import com.toparla.ui.components.GunesBubble
import com.toparla.ui.components.NavTab
import com.toparla.ui.components.TopBarActions
import com.toparla.ui.components.ToparlaScaffold
import com.toparla.ui.components.VoiceTextBar
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ThemeMode
import com.toparla.ui.theme.ToparlaTheme

private const val TAB_NOW = 0
private const val TAB_GUNES = 4

@Composable
private fun previewTabs() = listOf(
    NavTab(stringResource(R.string.preview_tab_now), ToparlaIcons.Now),
    NavTab(stringResource(R.string.preview_tab_inbox), ToparlaIcons.Inbox, hasNew = true),
    NavTab(stringResource(R.string.preview_tab_plan), ToparlaIcons.Plan),
    NavTab(stringResource(R.string.preview_tab_flow), ToparlaIcons.Flow),
    NavTab(stringResource(R.string.preview_tab_gunes), ToparlaIcons.Gunes),
)

/** Onaylı iskelet taslağı, Şimdi sekmesi: bütün eylemler görünür (yakalama, Güneş, profil). */
@Composable
fun NowSkeletonGallery() {
    ToparlaScaffold(
        title = stringResource(R.string.preview_tab_now),
        tabs = previewTabs(),
        selectedTab = TAB_NOW,
        onTabSelected = {},
        topBarActions = TopBarActions(onGunes = {}, onProfile = {}),
        capture = CaptureActions(onVoice = {}, onText = {}),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            EmptyState(
                shape = EmptyShape.RING,
                message = stringResource(R.string.preview_empty),
                actionLabel = stringResource(R.string.preview_empty_action),
                onAction = {},
            )
        }
    }
}

/** Onaylı iskelet taslağı, Güneş sekmesi: yakalama düğmesi gizli, yerine yazı/ses çubuğu. */
@Composable
fun GunesSkeletonGallery() {
    ToparlaScaffold(
        title = stringResource(R.string.preview_tab_gunes),
        tabs = previewTabs(),
        selectedTab = TAB_GUNES,
        onTabSelected = {},
        topBarActions = TopBarActions(onProfile = {}),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GunesBubble(text = stringResource(R.string.preview_gunes_hello), modifier = Modifier.padding(Spacing.screenEdge).weight(1f))
            VoiceTextBar(value = "", onValueChange = {}, onSpeak = {}, onSend = {})
        }
    }
}

@Preview(name = "İskelet · Şimdi · koyu", widthDp = 412, heightDp = 915)
@Composable
internal fun NowSkeletonDarkPreview() = ToparlaTheme(mode = ThemeMode.DARK) { NowSkeletonGallery() }

@Preview(name = "İskelet · Güneş · açık", widthDp = 412, heightDp = 915)
@Composable
internal fun GunesSkeletonLightPreview() = ToparlaTheme(mode = ThemeMode.LIGHT) { GunesSkeletonGallery() }
