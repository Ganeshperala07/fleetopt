package com.fleetopt.app.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.fleetopt.app.R
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
import com.fleetopt.app.core.forecasting.CalendarSurge
import com.fleetopt.app.core.simulation.SimulationScenario
import com.fleetopt.app.ui.components.*
import com.fleetopt.app.ui.theme.*
import com.fleetopt.app.ui.viewmodel.FleetOptUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: FleetOptUiState,
    onNavigateToRecommendation: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToFleet: () -> Unit,
    onNavigateToStations: () -> Unit,
    onSurgeChanged: (CalendarSurge) -> Unit,
    onScenarioChanged: (SimulationScenario) -> Unit,
    onAuthenticateAdmin: (String) -> Boolean,
    onLockAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdminPinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(FleetOptSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FleetOptSpacing.md)
    ) {
        // 1. Top Header Banner
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FleetOptSpacing.md)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "FleetOpt Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(FleetOptSpacing.radiusSm))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(FleetOptSpacing.radiusSm))
                        )
                        Column {
                            Text(
                                text = "FleetOpt CNG DSS",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Operations Command",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Admin Lock/Unlock & Data Provenance Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)
                    ) {
                        DataProvenanceBadge(
                            provenance = if (uiState.isSimulationActive) DataProvenance.SIMULATED else DataProvenance.REAL
                        )

                        IconButton(
                            onClick = {
                                if (uiState.isAdminMode) {
                                    onLockAdmin()
                                } else {
                                    showAdminPinDialog = true
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(FleetOptSpacing.radiusFull))
                                .background(
                                    if (uiState.isAdminMode) Red500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (uiState.isAdminMode) Red500.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(FleetOptSpacing.radiusFull)
                                )
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isAdminMode) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Admin Access",
                                tint = if (uiState.isAdminMode) Red400 else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Real-time virtual pipeline & cascade inventory orchestration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 2. Calendar Surge Selection Bar
        item {
            Card(
                shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FleetOptSpacing.sm)) {
                    Text(
                        text = "CALENDAR DEMAND MULTIPLIER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)
                    ) {
                        CalendarSurge.values().forEach { surge ->
                            val selected = uiState.currentSurge == surge
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(FleetOptSpacing.radiusSm))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(FleetOptSpacing.radiusSm)
                                    )
                                    .clickable { onSurgeChanged(surge) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = surge.badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. KPI Metric Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)
                ) {
                    KpiCard(
                        label = "Total Fleet",
                        value = "${uiState.kpi.totalHcvs}",
                        hint = "650 kg cascade tankers",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "Available",
                        value = "${uiState.kpi.availableHcvs}",
                        hint = "Ready for loading at CGS",
                        valueColor = Emerald400,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)
                ) {
                    KpiCard(
                        label = "Active Trips",
                        value = "${uiState.kpi.activeTrips}",
                        hint = "In corridor transit",
                        valueColor = Teal400,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "Utilization",
                        value = "${uiState.kpi.fleetUtilizationPercent}",
                        suffix = "%",
                        hint = "${uiState.kpi.totalHcvs - uiState.kpi.availableHcvs}/${uiState.kpi.totalHcvs} engaged",
                        valueColor = Amber400,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)
                ) {
                    KpiCard(
                        label = "Average ETA",
                        value = "${uiState.kpi.averageEtaMinutes}",
                        suffix = "m",
                        hint = "Transit across corridors",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "Cost Savings",
                        value = "₹${uiState.kpi.estDailyCostSavingsInr}",
                        hint = "Optimized vs. manual planning",
                        valueColor = Emerald400,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. "Next Best Action" Recommendation Preview Card
        item {
            val rec = uiState.recommendation
            Card(
                shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FleetOptSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Amber400, modifier = Modifier.size(20.dp))
                            Text(
                                text = "NEXT BEST ACTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber400,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (rec != null) {
                            Text(
                                text = "Match Score: ${"%.0f".format(rec.totalScore * 100)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                    if (rec != null) {
                        Text(
                            text = "Dispatch ${rec.assignedHcv.registration} → ${rec.targetStation.stationName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rec.rationale,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                        Button(
                            onClick = onNavigateToRecommendation,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(FleetOptSpacing.radiusSm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(FleetOptSpacing.xs))
                            Text("Open Decision Support Center", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "All stations currently operating safely above cutoff pressure, or all tankers en route.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 5. Corridor Network Preview
        item {
            Card(
                shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FleetOptSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CORRIDOR TRANSIT RADAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Virtual pipeline distribution corridors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        TextButton(onClick = onNavigateToMap) {
                            Text("Full Map", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                    CorridorRadarCanvas(
                        stations = uiState.rawStations,
                        tankers = uiState.fleet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }

        // 6. Urgent Stations Queue (< 60 bar)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STATIONS NEEDING GAS SOONEST",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ranked by lowest Time-To-Dryout (TTD)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                TextButton(onClick = onNavigateToStations) {
                    Text("View All Stations", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        }

        items(uiState.stations.take(3), key = { it.stationId }) { station ->
            val isCritical = station.currentPressureBar < 60.0
            Card(
                shape = RoundedCornerShape(FleetOptSpacing.radiusSm),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isCritical) Red500.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FleetOptSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = station.stationName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isCritical) Red500.copy(alpha = 0.15f) else Amber500.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "~${"%.1f".format(station.ttdHours)}h cover left",
                                color = if (isCritical) Red400 else Amber400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${station.demandPendingKg.roundToInt()} kg pending · ${station.distanceKm.toInt()} km · ${station.traffic} traffic",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pressure: ${"%.1f".format(station.currentPressureBar)} bar (${station.usableMassKg.roundToInt()} kg left)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCritical) Red400 else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = onNavigateToRecommendation,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isCritical) Red500 else MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(FleetOptSpacing.radiusSm),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Dispatch HCV",
                                fontSize = 11.sp,
                                color = if (isCritical) Color.White else MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(FleetOptSpacing.xxl)) }
    }

    // Admin PIN Dialog
    if (showAdminPinDialog) {
        var pin by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAdminPinDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Amber400)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin Access Authentication", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter Administrator PIN to unlock editing and deletion of HCVs, Stations, and Mother Hubs (Default PIN: 1234).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            pin = it
                            isError = false
                        },
                        label = { Text("4-Digit Admin PIN") },
                        singleLine = true,
                        isError = isError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text("Invalid PIN. Please try again.", color = Red400, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onAuthenticateAdmin(pin.trim())) {
                            showAdminPinDialog = false
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500)
                ) {
                    Text("Unlock Admin", color = Slate950, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminPinDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
