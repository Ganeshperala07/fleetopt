package com.fleetopt.app.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetopt.app.ui.theme.*
import com.fleetopt.app.ui.viewmodel.FleetOptUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    uiState: FleetOptUiState,
    onNavigateToTrips: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onUnlockAdminClick: () -> Unit,
    onLockAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTripsCount = uiState.trips.count { it.status.equals("EN_ROUTE", ignoreCase = true) }
    val criticalAlertsCount = uiState.stations.count { it.currentPressureBar < 60.0 } +
            uiState.fleet.count { it.status.equals("BREAKDOWN", ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Operations & Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                .padding(FleetOptSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FleetOptSpacing.md)
        ) {
            // Admin Security Banner / Quick Toggle
            item {
                Card(
                    shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isAdminMode) Teal500.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (uiState.isAdminMode) Teal500.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FleetOptSpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (uiState.isAdminMode) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (uiState.isAdminMode) Teal400 else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(FleetOptSpacing.sm))
                            Column {
                                Text(
                                    text = if (uiState.isAdminMode) "Admin Access Active" else "Operator Terminal (Protected)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.isAdminMode) "Full edit & delete privileges enabled" else "Tap unlock to manage stations and assets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (uiState.isAdminMode) {
                            OutlinedButton(onClick = onLockAdmin) {
                                Text("Lock", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = onUnlockAdminClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Unlock", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Hub Navigation Tiles
            item {
                MoreMenuTile(
                    title = "Trips & Dispatch History",
                    subtitle = "Track active transit, complete deliveries, and view audit trail",
                    icon = Icons.Default.History,
                    badge = if (activeTripsCount > 0) "$activeTripsCount active" else null,
                    badgeColor = Teal400,
                    onClick = onNavigateToTrips
                )
            }

            item {
                MoreMenuTile(
                    title = "Alerts & Notifications Center",
                    subtitle = "Monitor stockout risks, safety expiries, and bay interlocks",
                    icon = Icons.Default.Notifications,
                    badge = if (criticalAlertsCount > 0) "$criticalAlertsCount critical" else null,
                    badgeColor = Red400,
                    onClick = onNavigateToAlerts
                )
            }

            item {
                MoreMenuTile(
                    title = "Live Corridor Radar & Map",
                    subtitle = "Geographic transit monitoring with offline vector radar fallback",
                    icon = Icons.Default.Map,
                    badge = null,
                    badgeColor = Cyan500,
                    onClick = onNavigateToMap
                )
            }

            item {
                MoreMenuTile(
                    title = "Analytics & Diurnal Forecasting",
                    subtitle = "7-day weighted moving average and hourly burn rate profiles",
                    icon = Icons.Default.BarChart,
                    badge = null,
                    badgeColor = Emerald400,
                    onClick = onNavigateToAnalytics
                )
            }

            item {
                MoreMenuTile(
                    title = "Settings & Simulation Sandbox",
                    subtitle = "Monochromatic theme mode, simulation scenarios, and admin security",
                    icon = Icons.Default.Settings,
                    badge = null,
                    badgeColor = Slate400,
                    onClick = onNavigateToSettings
                )
            }

            item { Spacer(modifier = Modifier.height(FleetOptSpacing.xxl)) }
        }
    }
}

@Composable
private fun MoreMenuTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String?,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FleetOptSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(FleetOptSpacing.radiusSm))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(FleetOptSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(FleetOptSpacing.xs))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
