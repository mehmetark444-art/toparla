package com.toparla.app.nav

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.toparla.app.R
import com.toparla.app.reminder.AddReminderScreen
import com.toparla.app.reminder.AddReminderViewModel
import com.toparla.app.reminder.AutoStartWizardScreen
import com.toparla.app.reminder.HealthScreen
import com.toparla.app.reminder.HealthViewModel
import com.toparla.app.reminder.PlanScreen
import com.toparla.app.reminder.PlanViewModel
import com.toparla.app.reminder.SelfTestScreen
import com.toparla.app.reminder.SelfTestViewModel
import com.toparla.ui.components.EmptyShape
import com.toparla.ui.components.EmptyState
import com.toparla.ui.components.NavTab
import com.toparla.ui.components.TopBarActions
import com.toparla.ui.components.ToparlaScaffold
import com.toparla.ui.icons.ToparlaIcons
import kotlinx.serialization.Serializable

/** Rotalar (blueprint D0): tür güvenli, `@Serializable` nesneler. Parametreli rotalar ait oldukları fazda eklenir. */
sealed interface Route {
    @Serializable data object Now : Route

    @Serializable data object Inbox : Route

    @Serializable data object Plan : Route

    @Serializable data object Flow : Route

    @Serializable data object Gunes : Route

    /** Sekmeli ana yüzey; aşağıdaki tam ekran akışlar bunun üstünde, alt çubuksuz açılır. */
    @Serializable data object Home : Route

    @Serializable data object AddReminder : Route

    @Serializable data object Health : Route

    @Serializable data object AutoStartWizard : Route

    @Serializable data object SelfTest : Route
}

/** Alt çubuğun 5 sekmesi (blueprint C9): Şimdi · Gelen · Plan · Akış · Güneş. */
enum class Tab(val route: Route, @StringRes val label: Int, val icon: ImageVector) {
    NOW(Route.Now, R.string.tab_now, ToparlaIcons.Now),
    INBOX(Route.Inbox, R.string.tab_inbox, ToparlaIcons.Inbox),
    PLAN(Route.Plan, R.string.tab_plan, ToparlaIcons.Plan),
    FLOW(Route.Flow, R.string.tab_flow, ToparlaIcons.Flow),
    GUNES(Route.Gunes, R.string.tab_gunes, ToparlaIcons.Gunes),
}

/**
 * Gezinme iskeleti (F2.24, onaylı taslak `docs/tasarim/2026-10-09-ikon-iskelet/`). Sekmelerin asıl ekranları
 * ait oldukları fazlarda gelir; Plan sekmesi hatırlatma listesini gösterir (F2.38), ötekiler sakin boş durumunu.
 *
 * Henüz işlevi olmayan eylemler gösterilmez (yarım özellik telefona girmez): yakalama düğmesi M2 Yakala (F3),
 * Ben/Ayarlar ekranı ve Güneş yazışması (F6) gelince bağlanır.
 */
@Composable
fun ToparlaRoot() {
    // ViewModel'ler Activity kapsamında: Hilt fabrikası Activity'dedir, gezinme girişinde değil.
    val plan: PlanViewModel = viewModel()
    val add: AddReminderViewModel = viewModel()
    val health: HealthViewModel = viewModel()
    val selfTest: SelfTestViewModel = viewModel()
    val root = rememberNavController()
    val back: () -> Unit = { root.popBackStack() }

    // Sınama sırasında uygulama kapatıldıysa (sınamanın istediği de budur) açılışta sonuca dönülür.
    LaunchedEffect(Unit) { if (selfTest.hasPending()) root.navigate(Route.SelfTest) { launchSingleTop = true } }

    NavHost(navController = root, startDestination = Route.Home) {
        composable<Route.Home> { HomeTabs(plan, onAdd = { root.navigate(Route.AddReminder) }, onHealth = { root.navigate(Route.Health) }) }
        composable<Route.AddReminder> { FlowSurface { AddReminderScreen(add, onClose = back) } }
        composable<Route.Health> {
            FlowSurface {
                HealthScreen(health, onBack = back, onWizard = { root.navigate(Route.AutoStartWizard) }, onSelfTest = { root.navigate(Route.SelfTest) })
            }
        }
        composable<Route.AutoStartWizard> { FlowSurface { AutoStartWizardScreen(health, onClose = back) } }
        composable<Route.SelfTest> {
            FlowSurface { SelfTestScreen(selfTest, onClose = back, onFix = { root.navigate(Route.AutoStartWizard) }) }
        }
    }
}

@Composable
private fun HomeTabs(plan: PlanViewModel, onAdd: () -> Unit, onHealth: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = Tab.entries.firstOrNull { tab ->
        entry?.destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    } ?: Tab.NOW

    ToparlaScaffold(
        title = stringResource(current.label),
        tabs = Tab.entries.map { NavTab(label = stringResource(it.label), icon = it.icon) },
        selectedTab = current.ordinal,
        onTabSelected = { nav.navigateToTab(Tab.entries[it]) },
        topBarActions = TopBarActions(onGunes = if (current == Tab.GUNES) null else ({ nav.navigateToTab(Tab.GUNES) })),
    ) { padding ->
        NavHost(navController = nav, startDestination = Route.Now, modifier = Modifier.padding(padding)) {
            composable<Route.Now> {
                TabPlaceholder(EmptyShape.RING, R.string.empty_now, R.string.empty_now_action) { nav.navigateToTab(Tab.INBOX) }
            }
            composable<Route.Inbox> { TabPlaceholder(EmptyShape.LEAF, R.string.empty_inbox) }
            composable<Route.Plan> { PlanScreen(plan, onAdd = onAdd, onHealth = onHealth) }
            composable<Route.Flow> { TabPlaceholder(EmptyShape.WAVE, R.string.empty_flow) }
            composable<Route.Gunes> { TabPlaceholder(EmptyShape.RING, R.string.empty_gunes) }
        }
    }
}

/** Tam ekran akışın zemini: sistem çubukları ve klavye payı bırakılır. */
@Composable
private fun FlowSurface(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) { content() }
}

/** Sekmeler arası geçiş: her sekme kendi yığınını korur, aynı sekmeye ikinci dokunuş yeni kopya açmaz. */
private fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun TabPlaceholder(shape: EmptyShape, @StringRes message: Int, @StringRes action: Int? = null, onAction: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            shape = shape,
            message = stringResource(message),
            actionLabel = action?.let { stringResource(it) },
            onAction = onAction,
        )
    }
}
