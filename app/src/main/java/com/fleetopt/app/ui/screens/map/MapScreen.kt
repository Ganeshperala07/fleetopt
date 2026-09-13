package com.fleetopt.app.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.fleetopt.app.ui.components.CngGeoMapView
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

enum class MapEngineMode {
    GEO_MAP,       // Interactive OpenStreetMap / Leaflet (Default, 100% works, zero API key)
    RADAR_CANVAS,  // Offline Tactical Vector Radar Canvas
    GOOGLE_MAP     // Google Play Services Maps Provider
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: FleetOptUiState,
    onNavigateToRecommendation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var engineMode by remember { mutableStateOf(MapEngineMode.GEO_MAP) }
    var selectedStation by remember { mutableStateOf<StationEntity?>(null) }
    var isTrafficEnabled by remember { mutableStateOf(true) }

    val cgsLatLng = remember { LatLng(17.4526, 78.3312) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cgsLatLng, 9.2f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        when (engineMode) {
            MapEngineMode.GEO_MAP -> {
                // Interactive Geographic Map (OpenStreetMap / Leaflet) with 100 km Radius Circle
                CngGeoMapView(
                    stations = uiState.rawStations,
                    tankers = uiState.fleet,
                    themeMode = uiState.themeMode,
                    onStationClick = { selectedStation = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
            MapEngineMode.RADAR_CANVAS -> {
                // Tactical Offline Vector Radar Canvas with 100 km Perimeter Rings
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
            MapEngineMode.GOOGLE_MAP -> {
                // Google Maps Provider with 100km corridor polylines
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
                    Marker(
                        state = MarkerState(position = cgsLatLng),
                        title = "CGS - Shamshabad Mother Hub",
                        snippet = "Fast-Fill Hub | 100 km Corridor Supply Radius",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )

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

                    uiState.fleet.filter { it.status.equals("on_trip", ignoreCase = true) }.forEach { tanker ->
                        Marker(
                            state = MarkerState(position = LatLng(tanker.lat, tanker.lng)),
                            title = "Tanker ${tanker.registration}",
                            snippet = "Driver: ${tanker.driverName} | ${tanker.currentLocation}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
                        )
                    }
                }
            }
        }

        // Top Control Overlay: 100 KM Radius Badge + Engine Switcher
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 100 KM Corridor Perimeter Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.5f)),
                shadowElevation = 4.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ShareLocation, contentDescription = null, tint = Cyan500, modifier = Modifier.size(16.dp))
                    Text(
                        text = "100 KM HYDERABAD CORRIDOR CIRCLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                    DataProvenanceBadge(provenance = Provenance.REAL)
                }
            }

            // Multi-Engine Switcher Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = engineMode == MapEngineMode.GEO_MAP,
                        onClick = { engineMode = MapEngineMode.GEO_MAP },
                        label = { Text("🗺️ 100km Map", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colorScheme.surface.copy(alpha = 0.9f),
                            selectedContainerColor = colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = colorScheme.primary
                        )
                    )

                    FilterChip(
                        selected = engineMode == MapEngineMode.RADAR_CANVAS,
                        onClick = { engineMode = MapEngineMode.RADAR_CANVAS },
                        label = { Text("📡 Radar", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colorScheme.surface.copy(alpha = 0.9f),
                            selectedContainerColor = colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = colorScheme.primary
                        )
                    )

                    FilterChip(
                        selected = engineMode == MapEngineMode.GOOGLE_MAP,
                        onClick = { engineMode = MapEngineMode.GOOGLE_MAP },
                        label = { Text("🌐 Google", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colorScheme.surface.copy(alpha = 0.9f),
                            selectedContainerColor = colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = colorScheme.primary
                        )
                    )
                }

                // Traffic Filter toggle
                if (engineMode == MapEngineMode.GOOGLE_MAP) {
                    FilterChip(
                        selected = isTrafficEnabled,
                        onClick = { isTrafficEnabled = !isTrafficEnabled },
                        label = { Text(if (isTrafficEnabled) "🚦 On" else "Off", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colorScheme.surface.copy(alpha = 0.9f),
                            selectedContainerColor = Emerald500.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        // Station Details Bottom Card
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
                            Text(text = "${station.distanceKm.toInt()} km corridor from CGS (within 100 km circle)", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
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
