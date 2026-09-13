package com.fleetopt.app.ui.screens.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.fleetopt.app.ui.components.StatusBadge
import com.fleetopt.app.ui.theme.*
import com.fleetopt.app.ui.viewmodel.FleetOptUiState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchLogScreen(
    uiState: FleetOptUiState,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTrips = remember(uiState.trips, selectedFilter) {
        when (selectedFilter) {
            "EN_ROUTE" -> uiState.trips.filter { it.status == "EN_ROUTE" || it.status == "DECANTING" }
            "COMPLETED" -> uiState.trips.filter { it.status == "COMPLETED" }
            else -> uiState.trips
        }
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm · dd MMM", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        item {
            Column {
                Text(
                    text = "Dispatch Audit Log",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Historical audit trail of all heavy commercial vehicle dispatches",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 2. Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "All Dispatches", "EN_ROUTE" to "Active Trips", "COMPLETED" to "Completed").forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Teal500.copy(alpha = 0.2f),
                            selectedLabelColor = Teal500,
                            containerColor = Slate850,
                            labelColor = Slate300
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Teal500 else Slate700
                        )
                    )
                }
            }
        }

        // 3. Trips List
        if (filteredTrips.isNotEmpty()) {
            items(filteredTrips, key = { it.tripId }) { trip ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Slate800)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = trip.tripId,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Teal500,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateFormat.format(Date(trip.departureTime)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                    fontSize = 11.sp
                                )
                            }

                            StatusBadge(status = trip.status)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${trip.hcvRegistration} → ${trip.stationName}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Driver: ${trip.driverName} (${trip.driverPhone})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Dispatched", style = MaterialTheme.typography.bodySmall, color = Slate500, fontSize = 10.sp)
                                Text("${trip.dispatchedMassKg.roundToInt()} kg", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Decanted", style = MaterialTheme.typography.bodySmall, color = Slate500, fontSize = 10.sp)
                                Text(if (trip.decantedMassKg > 0) "${trip.decantedMassKg.roundToInt()} kg" else "Pending", style = MaterialTheme.typography.bodyMedium, color = Slate300, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Trip Cost", style = MaterialTheme.typography.bodySmall, color = Slate500, fontSize = 10.sp)
                                Text("₹${trip.routeCostInr}", style = MaterialTheme.typography.bodyMedium, color = Slate300, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Savings", style = MaterialTheme.typography.bodySmall, color = Slate500, fontSize = 10.sp)
                                Text("₹${trip.estimatedSavingsInr}", style = MaterialTheme.typography.bodyMedium, color = Emerald400, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (trip.isSplitMilkRun && !trip.secondaryStationName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Teal500.copy(alpha = 0.12f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Split Milk-Run: Continuing to ${trip.secondaryStationName} with ${trip.secondaryMassKg.roundToInt()} kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Teal500
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No dispatch records matching filter.", color = Slate500)
                }
            }
        }
    }
}
