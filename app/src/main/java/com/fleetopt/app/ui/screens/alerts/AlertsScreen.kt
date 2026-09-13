package com.fleetopt.app.ui.screens.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.fleetopt.app.ui.theme.*
import com.fleetopt.app.ui.viewmodel.FleetOptUiState

data class OperationalAlert(
    val id: String,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val timestamp: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

enum class AlertSeverity {
    CRITICAL,
    WARNING,
    INFO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    uiState: FleetOptUiState,
    onNavigateToRecommendation: () -> Unit,
    onNavigateToFleet: () -> Unit,
    onNavigateToStations: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Synthesize active real-time operational alerts from live state
    val alerts = remember(uiState) {
        val list = mutableListOf<OperationalAlert>()

        // 1. Critical Stations (< 60 bar)
        uiState.stations.filter { it.currentPressureBar < 60.0 }.forEach { station ->
            list.add(
                OperationalAlert(
                    id = "alert-crit-${station.stationId}",
                    severity = AlertSeverity.CRITICAL,
                    title = "Critical Stockout Risk: ${station.stationName}",
                    message = "Cascade pressure dropped to ${station.currentPressureBar.toInt()} bar (50 bar cutoff). TTD is ~${"%.1f".format(station.ttdHours)}h.",
                    timestamp = "Real-time telemetry",
                    actionLabel = "Dispatch Tanker",
                    onAction = onNavigateToRecommendation
                )
            )
        }

        // 2. Broken down vehicles
        uiState.fleet.filter { it.status.equals("BREAKDOWN", ignoreCase = true) }.forEach { hcv ->
            list.add(
                OperationalAlert(
                    id = "alert-breakdown-${hcv.registration}",
                    severity = AlertSeverity.CRITICAL,
                    title = "Tanker Breakdown: ${hcv.registration}",
                    message = "Driver ${hcv.driverName} reported: '${hcv.breakdownReason ?: "Mechanical Failure"}'. Vehicle offline.",
                    timestamp = "Maintenance gate",
                    actionLabel = "Inspect Fleet",
                    onAction = onNavigateToFleet
                )
            )
        }

        // 3. Warning Stations (60 - 90 bar)
        uiState.stations.filter { it.currentPressureBar in 60.0..90.0 }.forEach { station ->
            list.add(
                OperationalAlert(
                    id = "alert-warn-${station.stationId}",
                    severity = AlertSeverity.WARNING,
                    title = "Pressure Advisory: ${station.stationName}",
                    message = "Pressure at ${station.currentPressureBar.toInt()} bar. Approaching replenishment window (~${"%.1f".format(station.ttdHours)}h cover left).",
                    timestamp = "Station monitoring",
                    actionLabel = "View Station",
                    onAction = onNavigateToStations
                )
            )
        }

        // 4. Bay Interlocks
        uiState.stations.filter { it.isBayInterlocked }.forEach { station ->
            list.add(
                OperationalAlert(
                    id = "alert-bay-${station.stationId}",
                    severity = AlertSeverity.WARNING,
                    title = "Bay Interlock Active: ${station.stationName}",
                    message = "All ${station.totalBays} decanting bays occupied. Dispatched tankers will hold in staging corridor.",
                    timestamp = "Bay telemetry",
                    actionLabel = "View Station",
                    onAction = onNavigateToStations
                )
            )
        }

        // 5. Active En Route Trips
        uiState.trips.filter { it.status.equals("EN_ROUTE", ignoreCase = true) }.forEach { trip ->
            list.add(
                OperationalAlert(
                    id = "alert-trip-${trip.tripId}",
                    severity = AlertSeverity.INFO,
                    title = "Active Transit: ${trip.tripId}",
                    message = "Tanker ${trip.hcvRegistration} in transit to ${trip.stationName} with ${trip.dispatchedMassKg.toInt()} kg payload.",
                    timestamp = "ETA ~${trip.etaMinutes} min",
                    actionLabel = null,
                    onAction = null
                )
            )
        }

        list
    }

    val filteredAlerts = remember(alerts, selectedFilter) {
        when (selectedFilter) {
            "CRITICAL" -> alerts.filter { it.severity == AlertSeverity.CRITICAL }
            "WARNING" -> alerts.filter { it.severity == AlertSeverity.WARNING }
            "INFO" -> alerts.filter { it.severity == AlertSeverity.INFO }
            else -> alerts
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Alerts & Notifications Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${alerts.count { it.severity == AlertSeverity.CRITICAL }} critical, ${alerts.count { it.severity == AlertSeverity.WARNING }} warnings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = FleetOptSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FleetOptSpacing.md)
        ) {
            // Filter Chips
            item {
                Spacer(modifier = Modifier.height(FleetOptSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)
                ) {
                    listOf(
                        "ALL" to "All (${alerts.size})",
                        "CRITICAL" to "Critical (${alerts.count { it.severity == AlertSeverity.CRITICAL }})",
                        "WARNING" to "Warnings (${alerts.count { it.severity == AlertSeverity.WARNING }})",
                        "INFO" to "Info (${alerts.count { it.severity == AlertSeverity.INFO }})"
                    ).forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(FleetOptSpacing.radiusSm))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedFilter = key }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Alerts List
            if (filteredAlerts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FleetOptSpacing.xxl),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = Emerald400)
                            Spacer(modifier = Modifier.height(FleetOptSpacing.sm))
                            Text("No alerts in this category", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("All CNG telemetry and fleet assets operating nominally", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredAlerts, key = { it.id }) { alert ->
                    AlertItemCard(alert = alert)
                }
            }

            item { Spacer(modifier = Modifier.height(FleetOptSpacing.xxl)) }
        }
    }
}

@Composable
private fun AlertItemCard(
    alert: OperationalAlert,
    modifier: Modifier = Modifier
) {
    val (accentColor, icon) = when (alert.severity) {
        AlertSeverity.CRITICAL -> Red400 to Icons.Default.Warning
        AlertSeverity.WARNING -> Amber400 to Icons.Default.WarningAmber
        AlertSeverity.INFO -> Cyan500 to Icons.Default.Info
    }

    Card(
        shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(accentColor.copy(alpha = 0.5f))
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(FleetOptSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(FleetOptSpacing.sm))
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = alert.severity.name,
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alert.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )

                if (alert.actionLabel != null && alert.onAction != null) {
                    TextButton(
                        onClick = alert.onAction,
                        colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                    ) {
                        Text(alert.actionLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
