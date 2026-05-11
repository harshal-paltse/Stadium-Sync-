package com.stadiumsync.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.stadiumsync.app.presentation.screen.analytics.AnalyticsScreen
import com.stadiumsync.app.presentation.screen.crowd.CrowdHeatmapScreen
import com.stadiumsync.app.presentation.screen.home.HomeScreen
import com.stadiumsync.app.presentation.screen.login.LoginScreen
import com.stadiumsync.app.presentation.screen.match.LiveMatchScreen
import com.stadiumsync.app.presentation.screen.notifications.NotificationsScreen
import com.stadiumsync.app.presentation.screen.offline.OfflineModeScreen
import com.stadiumsync.app.presentation.screen.route.RouteSuggestionScreen
import com.stadiumsync.app.presentation.screen.settings.SettingsScreen
import com.stadiumsync.app.presentation.screen.transit.TransitControlScreen
import com.stadiumsync.app.presentation.screen.ticket.TicketScannerScreen

// ─── Routes ──────────────────────────────────────────────
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val MATCH = "match"
    const val CROWD = "crowd"
    const val TRANSIT = "transit"
    const val ROUTE = "route"
    const val NOTIFICATIONS = "notifications"
    const val OFFLINE = "offline"
    const val SETTINGS = "settings"
    const val ANALYTICS = "analytics"
    const val TICKET = "ticket"
}

data class BottomNavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Routes.MATCH, "Match", Icons.Filled.SportsCricket, Icons.Outlined.SportsCricket),
    BottomNavItem(Routes.TICKET, "Ticket", Icons.Filled.ConfirmationNumber, Icons.Outlined.ConfirmationNumber),
    BottomNavItem(Routes.TRANSIT, "Transit", Icons.Filled.Train, Icons.Outlined.Train),
    BottomNavItem(Routes.SETTINGS, "More", Icons.Filled.Menu, Icons.Outlined.Menu)
)

// ─── NavHost ─────────────────────────────────────────────
@Composable
fun StadiumSyncNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route } ||
            currentRoute in listOf(Routes.CROWD, Routes.ROUTE, Routes.OFFLINE, Routes.ANALYTICS, Routes.NOTIFICATIONS)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Routes.LOGIN, Modifier.padding(innerPadding)) {
            composable(Routes.LOGIN) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onMatchClick = { navController.navigate(Routes.MATCH) },
                    onCrowdClick = { navController.navigate(Routes.CROWD) },
                    onTransitClick = { navController.navigate(Routes.TRANSIT) },
                    onAlertsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOfflineClick = { navController.navigate(Routes.OFFLINE) },
                    onTicketClick = { navController.navigate(Routes.TICKET) }
                )
            }
            composable(Routes.MATCH) { LiveMatchScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.CROWD) { CrowdHeatmapScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.TRANSIT) { TransitControlScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ROUTE) { RouteSuggestionScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.OFFLINE) { OfflineModeScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } }
                )
            }
            composable(Routes.ANALYTICS) { AnalyticsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.TICKET) { TicketScannerScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
