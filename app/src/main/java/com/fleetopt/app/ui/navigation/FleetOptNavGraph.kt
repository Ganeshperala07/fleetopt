package com.fleetopt.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.fleetopt.app.ui.screens.alerts.AlertsScreen
import com.fleetopt.app.ui.screens.analytics.AnalyticsScreen
import com.fleetopt.app.ui.screens.dashboard.DashboardScreen
import com.fleetopt.app.ui.screens.fleet.FleetScreen
import com.fleetopt.app.ui.screens.map.MapScreen
import com.fleetopt.app.ui.screens.more.MoreScreen
import com.fleetopt.app.ui.screens.recommendation.RecommendationScreen
import com.fleetopt.app.ui.screens.settings.SettingsScreen
import com.fleetopt.app.ui.screens.stations.StationsScreen
import com.fleetopt.app.ui.screens.trips.TripsScreen
import com.fleetopt.app.ui.theme.*
import com.fleetopt.app.ui.viewmodel.FleetOptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetOptNavGraph(
    navController: NavHostController,
    viewModel: FleetOptViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            if (uiState.isSimulationActive) {
                Surface(
                    color = Amber500.copy(alpha = 0.95f),
                    contentColor = Slate950,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = Slate950, modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = "SIMULATION SANDBOX ACTIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate950
                                )
                                Text(
                                    text = "Live telemetry decay is overlaid in-memory. Database is protected.",
                                    fontSize = 10.sp,
                                    color = Slate900
                                )
                            }
                        }

                        TextButton(
                            onClick = { viewModel.toggleSimulation(false) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Slate950)
                        ) {
                            Text("Stop", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = colorScheme.surface,
                contentColor = colorScheme.onSurface,
                tonalElevation = 6.dp
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val isSelected = currentDestination?.route == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colorScheme.primary,
                            selectedTextColor = colorScheme.primary,
                            indicatorColor = colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = colorScheme.onSurfaceVariant,
                            unselectedTextColor = colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = modifier.padding(innerPadding)
        ) {
            // 1. Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    uiState = uiState,
                    onNavigateToRecommendation = { navController.navigate(Screen.Recommendation.route) },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onNavigateToFleet = { navController.navigate(Screen.Fleet.route) },
                    onNavigateToStations = { navController.navigate(Screen.Stations.route) },
                    onNavigateToMore = { navController.navigate(Screen.More.route) },
                    onSurgeChanged = { viewModel.setCalendarSurge(it) },
                    onScenarioChanged = { viewModel.setScenario(it) },
                    onAuthenticateAdmin = { viewModel.authenticateAdmin(it) },
                    onLockAdmin = { viewModel.lockAdmin() }
                )
            }

            // 2. Recommendation / Dispatch DSS
            composable(Screen.Recommendation.route) {
                RecommendationScreen(
                    uiState = uiState,
                    onAcceptDispatch = { callback -> viewModel.acceptDispatch(callback) },
                    onGenerateRecommendation = { viewModel.generateRecommendation() },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) }
                )
            }

            // 3. Fleet Board
            composable(Screen.Fleet.route) {
                FleetScreen(
                    uiState = uiState,
                    onReportBreakdown = { reg, reason -> viewModel.reportHcvBreakdown(reg, reason) },
                    onClearMaintenance = { reg -> viewModel.clearHcvMaintenance(reg) },
                    onAddHcv = { reg, driver, phone, cap, status, isHydro, hydroDate, isPrv, tire, heel ->
                        viewModel.addHcv(reg, driver, phone, cap, status, isHydro, hydroDate, isPrv, tire, heel)
                    },
                    onUpdateHcv = { viewModel.updateHcv(it) },
                    onDeleteHcv = { viewModel.deleteHcv(it) }
                )
            }

            // 4. Stations Network
            composable(Screen.Stations.route) {
                StationsScreen(
                    uiState = uiState,
                    onUpdatePressure = { id, pressure -> viewModel.updateStationPressure(id, pressure) },
                    onAddStation = { name, code, lat, lng, vol, bays, base, dist, traffic ->
                        viewModel.addStation(name, code, lat, lng, vol, bays, base, dist, traffic)
                    },
                    onUpdateStation = { viewModel.updateStation(it) },
                    onDeleteStation = { viewModel.deleteStation(it) },
                    onAddMotherStation = { name, code, lat, lng, vol, bays, compressor ->
                        viewModel.addMotherStation(name, code, lat, lng, vol, bays, compressor)
                    },
                    onNavigateToRecommendation = { navController.navigate(Screen.Recommendation.route) }
                )
            }

            // 5. More Operations Hub
            composable(Screen.More.route) {
                MoreScreen(
                    uiState = uiState,
                    onNavigateToTrips = { navController.navigate(Screen.Trips.route) },
                    onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onUnlockAdminClick = {
                        // Open admin dialog or direct to settings
                        navController.navigate(Screen.Settings.route)
                    },
                    onLockAdmin = { viewModel.lockAdmin() }
                )
            }

            // 6. Secondary: Trips Management
            composable(Screen.Trips.route) {
                TripsScreen(
                    trips = uiState.trips,
                    onCompleteTrip = { tripId -> viewModel.completeTrip(tripId) },
                    onCancelTrip = { tripId -> viewModel.cancelTrip(tripId) },
                    onBack = { navController.popBackStack() }
                )
            }

            // 7. Secondary: Alerts Center
            composable(Screen.Alerts.route) {
                AlertsScreen(
                    uiState = uiState,
                    onNavigateToRecommendation = { navController.navigate(Screen.Recommendation.route) },
                    onNavigateToFleet = { navController.navigate(Screen.Fleet.route) },
                    onNavigateToStations = { navController.navigate(Screen.Stations.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // 8. Secondary: Live Corridor Map
            composable(Screen.Map.route) {
                MapScreen(
                    uiState = uiState,
                    onNavigateToRecommendation = { navController.navigate(Screen.Recommendation.route) }
                )
            }

            // 9. Secondary: Analytics & Performance
            composable(Screen.Analytics.route) {
                AnalyticsScreen(uiState = uiState)
            }

            // 10. Secondary: Settings & Configuration
            composable(Screen.Settings.route) {
                SettingsScreen(
                    currentThemeMode = uiState.themeMode,
                    onThemeModeChanged = { viewModel.setThemeMode(it) },
                    isSimulationActive = uiState.isSimulationActive,
                    currentScenario = uiState.currentScenario,
                    onToggleSimulation = { viewModel.toggleSimulation(it) },
                    onScenarioChanged = { viewModel.setScenario(it) },
                    onResetSimulation = { viewModel.resetSimulation() },
                    isAdminMode = uiState.isAdminMode,
                    onChangePin = { oldPin, newPin -> viewModel.changeAdminPin(oldPin, newPin) },
                    onLockAdmin = { viewModel.lockAdmin() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
