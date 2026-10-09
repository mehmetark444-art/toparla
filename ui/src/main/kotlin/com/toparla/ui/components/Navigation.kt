package com.toparla.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.toparla.ui.R
import com.toparla.ui.icons.ToparlaIcons
import com.toparla.ui.theme.Elevation
import com.toparla.ui.theme.Sizes
import com.toparla.ui.theme.Spacing
import com.toparla.ui.theme.ToparlaTheme

/** Alt çubuk sekmesi. `hasNew`: yenilik varsa nötr nokta (sayı değil; blueprint C9). */
@Immutable
data class NavTab(val label: String, val icon: ImageVector, val hasNew: Boolean = false)

/** Üst çubuğun sağındaki eylemler; `null` olan gösterilmez (ör. Güneş sekmesinde avatar). */
@Immutable
data class TopBarActions(val onGunes: (() -> Unit)? = null, val onProfile: (() -> Unit)? = null)

/** Yakalama düğmesi eylemleri (blueprint C7 `CaptureFab`): kısa dokunuş ses, uzun basma yazı. */
@Immutable
data class CaptureActions(val onVoice: () -> Unit, val onText: () -> Unit)

/**
 * Uygulama iskeleti (blueprint C9, F2.24 taslağı): üst çubuk, 5 sekmeli alt çubuk, sağ altta yakalama düğmesi.
 * Kenardan kenara çizilir; sistem çubuklarının boşluğu `content`'e verilen dolgudadır.
 * `capture = null`: düğme gizli (Güneş sekmesinde ve yakalama hazır olmadan önce).
 */
@Composable
fun ToparlaScaffold(
    title: String,
    tabs: List<NavTab>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: TopBarActions = TopBarActions(),
    capture: CaptureActions? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = { ToparlaTopBar(title = title, actions = topBarActions) },
        bottomBar = { ToparlaNavBar(tabs = tabs, selected = selectedTab, onSelect = onTabSelected) },
        floatingActionButton = { if (capture != null) CaptureFab(actions = capture) },
        content = content,
    )
}

/** Üst çubuk (blueprint C9): solda ekran başlığı, sağda Güneş avatarı ve Ben/Ayarlar. */
@Composable
fun ToparlaTopBar(title: String, modifier: Modifier = Modifier, actions: TopBarActions = TopBarActions()) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .heightIn(min = Sizes.topBar)
            .padding(start = Spacing.screenEdge, end = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = ToparlaTheme.type.headline, modifier = Modifier.weight(1f).semantics { heading() })
        actions.onGunes?.let { onGunes ->
            val label = stringResource(R.string.cd_gunes)
            IconButton(onClick = onGunes, modifier = Modifier.semantics { contentDescription = label }) {
                GunesAvatar(size = Sizes.avatarMedium)
            }
        }
        actions.onProfile?.let { onProfile ->
            IconButton(onClick = onProfile) {
                Icon(
                    imageVector = ToparlaIcons.Person,
                    contentDescription = stringResource(R.string.cd_profile),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Alt çubuk (blueprint C9): etiketli 5 sekme, seçili sekmede `primaryContainer` hap. */
@Composable
fun ToparlaNavBar(tabs: List<NavTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val newLabel = stringResource(R.string.cd_new_items)
    NavigationBar(modifier = modifier, containerColor = colors.surfaceDim, tonalElevation = Elevation.none) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(index) },
                icon = {
                    BadgedBox(badge = { if (tab.hasNew) Badge(containerColor = colors.onSurfaceVariant, modifier = Modifier.semantics { contentDescription = newLabel }) }) {
                        Icon(imageVector = tab.icon, contentDescription = null)
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        style = ToparlaTheme.type.caption,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onPrimaryContainer,
                    selectedTextColor = colors.onSurface,
                    indicatorColor = colors.primaryContainer,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Yakalama düğmesi (blueprint C7, K22): 64 dp; kısa dokunuş ses, uzun basma yazı. Yanında küçük klavye rozeti
 * yazı yolunun da var olduğunu gösterir. Dokununca `CONFIRM` titreşimi.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CaptureFab(actions: CaptureActions, modifier: Modifier = Modifier) {
    val haptics = ToparlaTheme.haptics
    val ext = ToparlaTheme.extended
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.cd_capture)
    Box(modifier = modifier.size(Sizes.captureFab)) {
        Box(
            modifier = Modifier
                .size(Sizes.captureFab)
                .shadow(elevation = Elevation.small, shape = CircleShape, ambientColor = ext.cardShadow, spotColor = ext.cardShadow)
                .background(colors.primaryContainer, CircleShape)
                .combinedClickable(
                    role = Role.Button,
                    onLongClick = {
                        haptics.confirm()
                        actions.onText()
                    },
                    onClick = {
                        haptics.confirm()
                        actions.onVoice()
                    },
                )
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = ToparlaIcons.Mic, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(Sizes.fabIcon))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = -Spacing.xxs, y = Spacing.xxs)
                .size(Sizes.avatarSmall)
                .background(colors.surface, CircleShape)
                .border(Elevation.border, colors.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = ToparlaIcons.Keyboard, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(Sizes.badgeIcon))
        }
    }
}
