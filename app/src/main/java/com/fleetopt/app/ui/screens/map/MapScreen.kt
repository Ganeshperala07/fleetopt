package com.fleetopt.app.ui.screens.map

import androidx.compose.foundation.background
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
import com.fleetopt.app.data.local.entity.StationEntity
import com.fleetopt.app.ui.components.CorridorRadarCanvas
import com.fleetopt.app.ui.components.DataProvenanceBadge
import com.fleetopt.app.ui.components.Provenance
import com.fleetopt.app.ui.theme.*
import com.fleetopt.app.ui.viewmodel.FleetOptUiState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: FleetOptUiState,
    onNavigateToRecommendation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var isRadarMode by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<StationEntity?>(null) }
    var isTrafficEnabled by remember { mutableStateOf(true) }

    val cgsLatLng = remember { LatLng(17.4526, 78.3312) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cgsLatLng, 9.8f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        if (!isRadarMode) {
            // Google Maps View
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isTrafficEnabled = isTrafficEnabled,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                // CGS Mother Station Marker
                Marker(
                    state = MarkerState(position = cgsLatLng),
                    title = "CGS - Mother Hub",
                    snippet = "Fast-Fill Hub | 250 bar pipeline supply",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )

                // Daughter Station Markers
                uiState.rawStations.forEach { station ->
                    val stationPos = LatLng(station.lat, station.lng)
                    val markerHue = when {
                        station.currentPressureBar < 60.0 -> BitmapDescriptorFactory.HUE_RED
                        station.currentPressureBar <= 120.0 -> BitmapDescriptorFactory.HUE_ORANGE
                        else -> BitmapDescriptorFactory.HUE_GREEN
                    }

                    Marker(
                        state = MarkerState(position = stationPos),
                        title = station.name,
                        snippet = "Pressure: ${station.currentPressureBar.toInt()} bar | ${station.distanceKm.toInt()} km",
                        icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                        onClick = {
                            selectedStation = station
                            true
                        }
                    )

                    // Corridor polyline to CGS
                    val polylineColor = when (station.traffic.lowercase()) {
                        "low" -> TrafficLowGreen
                        "moderate" -> TrafficModerateAmber
                        else -> TrafficHeavyRed
                    }
                    Polyline(
                        points = listOf(cgsLatLng, stationPos),
                        color = polylineColor,
                        width = 8f
                    )
                }

                // Active Tankers Markers
                uiState.fleet.filter { it.status.equals("on_trip", ignoreCase = true) }.forEach { tanker ->
                    Marker(
                        state = MarkerState(position = LatLng(tanker.lat, tanker.lng)),
                        title = "Tanker ${tanker.registration}",
                        snippet = "Driver: ${tanker.driverName} | ${tanker.currentLocation}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
                    )
                }
            }
        } else {
            // Offline Radar Canvas Mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                CorridorRadarCanvas(
                    stations = uiState.rawStations,
                    tankers = uiState.fleet,
                    onStationClick = { selectedStation = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top Control Overlay: Traffic Toggle + View Switcher
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isRadarMode,
                onClick = { isRadarMode = !isRadarMode },
                label = {
                    Text(
                        if (isRadarMode) "🗺️ Standard Map" else "📡 Radar Canvas",
                        color = colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colorScheme.surface.copy(alpha = 0.9f),
                    selectedContainerColor = colorScheme.primary.copy(alpha = 0.2f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isRadarMode,
                    borderColor = if (isRadarMode) colorScheme.primary else colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DataProvenanceBadge(
                    provenance = if (uiState.isSimulationActive) Provenance.SIMULATED else Provenance.REAL
                )

                FilterChip(
                    selected = isTrafficEnabled,
                    onClick = { isTrafficEnabled = !isTrafficEnabled },
                    label = {
                        Text(
                            if (isTrafficEnabled) "🚦 Traffic ON" else "Traffic OFF",
                            color = colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colorScheme.surface.copy(alpha = 0.9f),
                        selectedContainerColor = Emerald500.copy(alpha = 0.2f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isTrafficEnabled,
                        borderColor = if (isTrafficEnabled) Emerald500 else colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Station Details Card
        if (selectedStation != null) {
            val station = selectedStation!!
            val eval = uiState.stations.firstOrNull { it.stationId == station.id }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.96f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (station.currentPressureBar < 60) Crimson500 else colorScheme.primary
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = station.name, style = MaterialTheme.typography.titleLarge, color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text(text = "${station.distanceKm.toInt()} km corridor from CGS Hub", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        }

                        IconButton(onClick = { selectedStation = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pressure", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text("${station.currentPressureBar.toInt()} bar", style = MaterialTheme.typography.titleMedium, color = if (station.currentPressureBar < 60) Crimson500 else Emerald400, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Cover Left", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text("~${"%.1f".format(eval?.ttdHours ?: 2.0)}h", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Top-Up Needed", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text("${eval?.topUpDemandKg?.roundToInt() ?: 450} kg", style = MaterialTheme.typography.titleMedium, color = colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            selectedStation = null
                            onNavigateToRecommendation()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (station.currentPressureBar < 60) Crimson500 else colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dispatch Tanker to ${station.name}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
