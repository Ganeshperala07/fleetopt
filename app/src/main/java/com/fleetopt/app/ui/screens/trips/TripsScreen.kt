package com.fleetopt.app.ui.screens.trips

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
import com.fleetopt.app.data.local.entity.DispatchTripEntity
import com.fleetopt.app.ui.components.StatusBadge
import com.fleetopt.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    trips: List<DispatchTripEntity>,
    onCompleteTrip: (tripId: String) -> Unit,
    onCancelTrip: (tripId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var tripToComplete by remember { mutableStateOf<DispatchTripEntity?>(null) }
    var tripToCancel by remember { mutableStateOf<DispatchTripEntity?>(null) }

    val filteredTrips = remember(trips, searchQuery, selectedFilter) {
        trips.filter { trip ->
            val matchesFilter = when (selectedFilter) {
                "EN_ROUTE" -> trip.status.equals("EN_ROUTE", ignoreCase = true)
                "COMPLETED" -> trip.status.equals("COMPLETED", ignoreCase = true)
                "CANCELLED" -> trip.status.equals("CANCELLED", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    trip.tripId.contains(searchQuery, ignoreCase = true) ||
                    trip.hcvRegistration.contains(searchQuery, ignoreCase = true) ||
                    trip.driverName.contains(searchQuery, ignoreCase = true) ||
                    trip.stationName.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Trips & Dispatch History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${trips.size} total trips logged",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // 1. Search Bar
            item {
                Spacer(modifier = Modifier.height(FleetOptSpacing.xs))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by Trip ID, Tanker, Driver, or Station...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
                    singleLine = true
                )
            }

            // 2. Status Filter Chips
            item {
                val enRouteCount = trips.count { it.status.equals("EN_ROUTE", ignoreCase = true) }
                val completedCount = trips.count { it.status.equals("COMPLETED", ignoreCase = true) }
                val cancelledCount = trips.count { it.status.equals("CANCELLED", ignoreCase = true) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)
                ) {
                    listOf(
                        "ALL" to "All (${trips.size})",
                        "EN_ROUTE" to "Active ($enRouteCount)",
                        "COMPLETED" to "Done ($completedCount)",
                        "CANCELLED" to "Cancelled ($cancelledCount)"
                    ).forEach { (filterKey, label) ->
                        val isSelected = selectedFilter == filterKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(FleetOptSpacing.radiusSm))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedFilter = filterKey }
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

            // 3. Trips List
            if (filteredTrips.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FleetOptSpacing.xxl),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(FleetOptSpacing.sm))
                            Text("No trips found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Trips will appear here once dispatched from the DSS Engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredTrips, key = { it.tripId }) { trip ->
                    TripCard(
                        trip = trip,
                        onCompleteClick = { tripToComplete = trip },
                        onCancelClick = { tripToCancel = trip }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(FleetOptSpacing.xxl)) }
        }
    }

    // Complete Trip Confirmation Dialog
    if (tripToComplete != null) {
        val target = tripToComplete!!
        AlertDialog(
            onDismissRequest = { tripToComplete = null },
            title = { Text("Confirm Trip Delivery", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    text = "Mark trip '${target.tripId}' as COMPLETED?\n\nThis will:\n• Return tanker '${target.hcvRegistration}' to AVAILABLE status at CGS Yard\n• Release decanting bay at ${target.stationName}\n• Credit ${target.dispatchedMassKg.roundToInt()} kg delivered CNG to station inventory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCompleteTrip(target.tripId)
                        tripToComplete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("Complete Delivery", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToComplete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Cancel Trip Confirmation Dialog
    if (tripToCancel != null) {
        val target = tripToCancel!!
        AlertDialog(
            onDismissRequest = { tripToCancel = null },
            title = { Text("Cancel Trip Dispatch", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    text = "Are you sure you want to cancel trip '${target.tripId}'?\n\nThe tanker will return to AVAILABLE status and the reserved decanting bay will be freed without adding delivered gas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelTrip(target.tripId)
                        tripToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    Text("Cancel Trip", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToCancel = null }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun TripCard(
    trip: DispatchTripEntity,
    onCompleteClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val timeFormatted = remember(trip.departureTime) { dateFormat.format(Date(trip.departureTime)) }
    val isEnRoute = trip.status.equals("EN_ROUTE", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isEnRoute) Teal500.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(FleetOptSpacing.md)) {
            // Header Row: Trip ID + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = trip.tripId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Departed: $timeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = trip.status)
            }

            Spacer(modifier = Modifier.height(FleetOptSpacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

            // Details Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "DESTINATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(text = trip.stationName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "PAYLOAD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(text = "${trip.dispatchedMassKg.roundToInt()} kg CNG", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(FleetOptSpacing.xs))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "TANKER / DRIVER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(text = "${trip.hcvRegistration} (${trip.driverName})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "ESTIMATED SAVINGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(text = "₹${trip.estimatedSavingsInr} vs Manual", style = MaterialTheme.typography.bodySmall, color = Emerald400, fontWeight = FontWeight.SemiBold)
                }
            }

            if (trip.isSplitMilkRun && trip.secondaryStationName != null) {
                Spacer(modifier = Modifier.height(FleetOptSpacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(FleetOptSpacing.radiusSm))
                        .background(Teal500.copy(alpha = 0.1f))
                        .padding(horizontal = FleetOptSpacing.sm, vertical = FleetOptSpacing.xs)
                ) {
                    Text(
                        text = "Split Milk-Run: Dropping ${trip.secondaryMassKg.roundToInt()} kg to ${trip.secondaryStationName}",
                        fontSize = 11.sp,
                        color = Teal400,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action buttons if active
            if (isEnRoute) {
                Spacer(modifier = Modifier.height(FleetOptSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)
                ) {
                    Button(
                        onClick = onCompleteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        shape = RoundedCornerShape(FleetOptSpacing.radiusSm),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(FleetOptSpacing.xs))
                        Text("Complete Trip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCancelClick,
                        shape = RoundedCornerShape(FleetOptSpacing.radiusSm),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
                        border = ButtonDefaults.outlinedButtonBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Red500.copy(alpha = 0.5f))
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Red400, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
