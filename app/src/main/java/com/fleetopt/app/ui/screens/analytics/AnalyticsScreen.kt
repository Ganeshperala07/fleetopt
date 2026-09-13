package com.fleetopt.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetopt.app.ui.components.DataProvenanceBadge
import com.fleetopt.app.ui.components.KpiCard
import com.fleetopt.app.ui.components.Provenance
import com.fleetopt.app.ui.theme.*
import com.fleetopt.app.ui.viewmodel.FleetOptUiState
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    uiState: FleetOptUiState,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val totalDispatchedKg = uiState.trips.sumOf { it.dispatchedMassKg }.roundToInt()
    val totalSavings = uiState.kpi.estDailyCostSavingsInr

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Operational Intelligence",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            DataProvenanceBadge(provenance = Provenance.FORECAST)
                        }
                        Text(
                            text = "Analytics & Performance",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Historical sales demand trends, 7-day WMA forecasting, diurnal rush profiles, and DSS cost savings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 2. High-level Performance Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    label = "Stockout Avoidance",
                    value = "98.4%",
                    hint = "Zero dryouts today",
                    valueColor = Emerald400,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = "Fleet Turnaround",
                    value = "3h 45m",
                    hint = "Avg cycle duration",
                    valueColor = colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    label = "Total Dispatched",
                    value = "$totalDispatchedKg",
                    suffix = "kg",
                    hint = "Delivered CNG today",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = "Net Cost Savings",
                    value = "₹$totalSavings",
                    hint = "Avoided stockout losses",
                    valueColor = Emerald400,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Station Demand Trends: Projected vs Baseline
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outline.copy(alpha = 0.25f))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "PROJECTED VS BASELINE SALES DEMAND (KG/DAY)",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    uiState.stations.forEach { station ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = station.stationName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${station.projectedDemandKg.roundToInt()} kg (${station.baselineDemandKg.roundToInt()} base)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val fraction = (station.projectedDemandKg / 4500.0).toFloat().coerceIn(0.1f, 1f)
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = colorScheme.primary,
                                trackColor = colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Diurnal Sales Rush Profile
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outline.copy(alpha = 0.25f))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "HOURLY DIURNAL SALES RUSH PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    DiurnalWindowRow(timeWindow = "07:00 - 10:00", description = "Morning Auto/Cab Rush (Peak)", multiplier = "1.6x Baseline", color = Crimson500)
                    DiurnalWindowRow(timeWindow = "10:00 - 17:00", description = "Midday Commercial Fleet Flow", multiplier = "0.9x Baseline", color = colorScheme.primary)
                    DiurnalWindowRow(timeWindow = "17:00 - 21:00", description = "Evening Transit Rush (Peak)", multiplier = "1.7x Baseline", color = Crimson500)
                    DiurnalWindowRow(timeWindow = "21:00 - 07:00", description = "Overnight Truck Lull", multiplier = "0.4x Baseline", color = colorScheme.onSurfaceVariant)
                }
            }
        }

        // 5. Cost Savings Breakdown
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outline.copy(alpha = 0.25f))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SAVINGS BREAKDOWN (VS UNOPTIMIZED DISPATCH)",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    SavingsRow(title = "Stockout Compressor Penalty Avoidance", amount = "₹${(totalSavings * 0.58).roundToInt()}")
                    SavingsRow(title = "Corridor Fuel & Traffic Surcharge Optimization", amount = "₹${(totalSavings * 0.28).roundToInt()}")
                    SavingsRow(title = "Bay Turnaround & Idle Wait Reduction", amount = "₹${(totalSavings * 0.14).roundToInt()}")
                }
            }
        }
    }
}

@Composable
private fun DiurnalWindowRow(
    timeWindow: String,
    description: String,
    multiplier: String,
    color: Color
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = timeWindow, style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = multiplier, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SavingsRow(title: String, amount: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(text = amount, style = MaterialTheme.typography.bodyMedium, color = Emerald400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
