package com.fleetopt.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    // 5 Primary Bottom Navigation Destinations
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Recommendation : Screen("dispatch", "Dispatch", Icons.Default.AutoAwesome)
    object Fleet : Screen("fleet", "Fleet", Icons.Default.LocalShipping)
    object Stations : Screen("stations", "Stations", Icons.Default.EvStation)
    object More : Screen("more", "More", Icons.Default.Menu)

    // Secondary & Dedicated Screens
    object Trips : Screen("trips", "Trips History", Icons.Default.History)
    object Alerts : Screen("alerts", "Alerts Center", Icons.Default.Notifications)
    object Map : Screen("map", "Live Corridor", Icons.Default.Map)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(
            Dashboard,
            Recommendation,
            Fleet,
            Stations,
            More
        )
    }
}
