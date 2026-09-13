package com.fleetopt.app.ui.screens.stations

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
import com.fleetopt.app.core.engine.StationEvaluation
import com.fleetopt.app.data.local.entity.StationEntity
import com.fleetopt.app.ui.components.CascadeGauge3Bank
import com.fleetopt.app.ui.components.DataProvenanceBadge
import com.fleetopt.app.ui.components.PressureArcGauge
import com.fleetopt.app.ui.components.Provenance
import com.fleetopt.app.ui.components.TrafficBadge
import com.fleetopt.app.ui.theme.*
import com.fleetopt.app.ui.viewmodel.FleetOptUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(
    uiState: FleetOptUiState,
    onUpdatePressure: (stationId: String, newPressureBar: Double) -> Unit,
    onAddStation: (name: String, code: String, lat: Double, lng: Double, volumeL: Double, bays: Int, baselineKg: Double, distanceKm: Double, traffic: String) -> Unit,
    onUpdateStation: (StationEntity) -> Unit,
    onDeleteStation: (stationId: String) -> Unit,
    onAddMotherStation: (name: String, code: String, lat: Double, lng: Double, volumeL: Double, bays: Int, compressorCapacity: Double) -> Unit,
    onNavigateToRecommendation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    var selectedTab by remember { mutableStateOf("DAUGHTER") } // "DAUGHTER" or "MOTHER"
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddMotherDialog by remember { mutableStateOf(false) }
    var selectedStationForPressureUpdate by remember { mutableStateOf<StationEvaluation?>(null) }
    var selectedStationForDetail by remember { mutableStateOf<StationEvaluation?>(null) }
    var editingStation by remember { mutableStateOf<StationEntity?>(null) }
    var deletingStation by remember { mutableStateOf<StationEntity?>(null) }

    val filteredStations = remember(uiState.stations, searchQuery) {
        if (searchQuery.isBlank()) uiState.stations else {
            uiState.stations.filter {
                it.stationName.contains(searchQuery, ignoreCase = true) ||
                        it.stationId.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredMotherStations = remember(uiState.motherStations, searchQuery) {
        if (searchQuery.isBlank()) uiState.motherStations else {
            uiState.motherStations.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (uiState.isAdminMode) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == "DAUGHTER") {
                            showAddDialog = true
                        } else {
                            showAddMotherDialog = true
                        }
                    },
                    containerColor = if (selectedTab == "DAUGHTER") colorScheme.primary else Cyan500,
                    contentColor = colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = if (selectedTab == "DAUGHTER") "Add Daughter Station" else "Add Mother Hub"
                    )
                }
            }
        },
        containerColor = colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(paddingValues)
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
                                    text = "Infrastructure Network",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                DataProvenanceBadge(
                                    provenance = if (uiState.isSimulationActive) Provenance.SIMULATED else Provenance.REAL
                                )
                            }
                            Text(
                                text = "CNG Station Network",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Decanting daughter stations & CGS mother compression hubs with AGA-8 compressibility modeling",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (uiState.isAdminMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorScheme.primary.copy(alpha = 0.12f))
                                .border(1.dp, colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ADMIN MODE ACTIVE: Full Edit & Delete privileges enabled",
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Tab Selector: Daughter vs Mother Stations
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "DAUGHTER" to "Daughter Stations (${uiState.stations.size})",
                            "MOTHER" to "Mother Hubs / CGS (${uiState.motherStations.size})"
                        ).forEach { (key, label) ->
                            val isSelected = selectedTab == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) (if (key == "DAUGHTER") colorScheme.primary else Cyan500) else Color.Transparent
                                    )
                                    .clickable { selectedTab = key }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Search Bar
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (selectedTab == "DAUGHTER") "Search daughter stations..." else "Search mother hubs...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colorScheme.onSurfaceVariant) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colorScheme.surface,
                            unfocusedContainerColor = colorScheme.surface,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Tab Content: DAUGHTER STATIONS
            if (selectedTab == "DAUGHTER") {
                if (filteredStations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surface)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Daughter Stations found", color = colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text("Tap '+' below to add a new CNG daughter station", color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }

                items(filteredStations, key = { it.stationId }) { station ->
                    val rawEntity = uiState.rawStations.firstOrNull { it.id == station.stationId }
                    val volumeLiters = rawEntity?.geometricVolumeLiters ?: 3500.0
                    val isCritical = station.currentPressureBar < 60.0
                    val motherName = rawEntity?.motherStationName ?: "CGS Shamshabad Mother Hub"

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isCritical) Crimson500.copy(alpha = 0.8f) else colorScheme.outline.copy(alpha = 0.25f)
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStationForDetail = station }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Title row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = station.stationName,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(colorScheme.surfaceVariant)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = rawEntity?.id ?: station.stationId,
                                                fontSize = 10.sp,
                                                color = colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Hub,
                                            contentDescription = null,
                                            tint = Cyan500,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Supplied by: $motherName (${station.distanceKm.toInt()} km)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TrafficBadge(traffic = station.traffic)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Gauge + Key Inventory Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PressureArcGauge(
                                    pressureBar = station.currentPressureBar,
                                    sizeDp = 95
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    InventoryRow(label = "Geometric Volume:", value = "${volumeLiters.toInt()} L")
                                    InventoryRow(
                                        label = "Usable Gas Left:",
                                        value = "${station.usableMassKg.roundToInt()} kg",
                                        valueColor = if (isCritical) Crimson500 else Emerald400
                                    )
                                    InventoryRow(label = "Top-Up Demand:", value = "${station.topUpDemandKg.roundToInt()} kg")
                                    InventoryRow(
                                        label = "Time To Dryout (TTD):",
                                        value = "~${"%.1f".format(station.ttdHours)} hours",
                                        valueColor = if (isCritical) Crimson500 else Amber500
                                    )
                                    InventoryRow(label = "Decanting Bays:", value = "${station.openBays}/${station.totalBays} Open")
                                }
                            }

                            // Bay Interlock Warning
                            if (station.isBayInterlocked) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Amber500.copy(alpha = 0.15f))
                                        .border(1.dp, Amber500.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "BAY INTERLOCK ACTIVE: All ${station.totalBays} bays occupied. Tankers must hold.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Amber500,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // 3-Bank Cascade Cylinder Bank Visualizer
                            Spacer(modifier = Modifier.height(12.dp))
                            CascadeGauge3Bank(currentPressureBar = station.currentPressureBar)

                            // Action buttons: Update Pressure & Dispatch HCV
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { selectedStationForPressureUpdate = station },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.primary),
                                    border = ButtonDefaults.outlinedButtonBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(colorScheme.primary.copy(alpha = 0.5f))
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Telemetry Entry", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = onNavigateToRecommendation,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCritical) Crimson500 else colorScheme.primary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Dispatch HCV",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Admin Edit & Delete Row
                            if (uiState.isAdminMode && rawEntity != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { editingStation = rawEntity },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface),
                                        border = ButtonDefaults.outlinedButtonBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outline.copy(alpha = 0.4f))
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit Station", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { deletingStation = rawEntity },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Crimson500),
                                        border = ButtonDefaults.outlinedButtonBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(Crimson500.copy(alpha = 0.4f))
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = Crimson500)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Delete", fontSize = 11.sp, color = Crimson500)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 3. Tab Content: MOTHER STATIONS / CGS
                if (filteredMotherStations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surface)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Hub, contentDescription = null, tint = Cyan500, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Mother Stations (CGS) Configured", color = colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text("Tap '+' below to configure City Gate / Mother Hubs", color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }

                items(filteredMotherStations, key = { it.id }) { mother ->
                    val suppliedDaughters = uiState.rawStations.filter {
                        it.stationType == "DAUGHTER" && (it.motherStationId == mother.id || it.motherStationName == mother.name)
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Cyan500.copy(alpha = 0.4f))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Hub, contentDescription = null, tint = Cyan500, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = mother.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Primary High-Pressure Filling Hub • ID: ${mother.id}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Cyan500.copy(alpha = 0.15f))
                                        .border(1.dp, Cyan500.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ONLINE / CGS",
                                        color = Cyan500,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Mother Hub Specs Grid
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                InventoryRow(
                                    label = "Compressor Capacity:",
                                    value = "${mother.compressorCapacityScmh.roundToInt()} SCMH (m³/h)",
                                    valueColor = Cyan500
                                )
                                InventoryRow(
                                    label = "Fast-Fill Loading Bays:",
                                    value = "${mother.totalBays} Dedicated Bays",
                                    valueColor = colorScheme.onSurface
                                )
                                InventoryRow(
                                    label = "Header Supply Pressure:",
                                    value = "250 bar (Continuous Pipeline Booster)",
                                    valueColor = Emerald400
                                )
                                InventoryRow(
                                    label = "Station Cascade Storage:",
                                    value = "${mother.geometricVolumeLiters.roundToInt()} Liters",
                                    valueColor = colorScheme.onSurfaceVariant
                                )
                                InventoryRow(
                                    label = "GPS Coordinates:",
                                    value = "${"%.4f".format(mother.lat)}, ${"%.4f".format(mother.lng)}",
                                    valueColor = colorScheme.onSurfaceVariant
                                )
                            }

                            // Linked Daughter Stations
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Supplied Daughter Stations (${suppliedDaughters.size}):",
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (suppliedDaughters.isEmpty()) {
                                Text(
                                    text = "None assigned yet (default fallback)",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (daughter in suppliedDaughters) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(colorScheme.surfaceVariant)
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = daughter.name.replace(" CNG Daughter", "").replace(" FS", ""),
                                                fontSize = 10.sp,
                                                color = colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // Admin Edit & Delete Row
                            if (uiState.isAdminMode) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { editingStation = mother },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface),
                                        border = ButtonDefaults.outlinedButtonBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outline.copy(alpha = 0.4f))
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Hub", modifier = Modifier.size(14.dp), tint = Cyan500)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit Hub", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { deletingStation = mother },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Crimson500),
                                        border = ButtonDefaults.outlinedButtonBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(Crimson500.copy(alpha = 0.4f))
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Hub", modifier = Modifier.size(14.dp), tint = Crimson500)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Delete Hub", fontSize = 11.sp, color = Crimson500)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Station Detail Bottom Sheet
    selectedStationForDetail?.let { station ->
        val rawEntity = uiState.rawStations.firstOrNull { it.id == station.stationId }
        val volumeLiters = rawEntity?.geometricVolumeLiters ?: 3500.0
        val isCritical = station.currentPressureBar < 60.0

        ModalBottomSheet(
            onDismissRequest = { selectedStationForDetail = null },
            containerColor = colorScheme.surface,
            contentColor = colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = station.stationName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Station Code: ${station.stationId} • Corridor: ${station.distanceKm.toInt()} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    TrafficBadge(traffic = station.traffic)
                }

                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.2f))

                // Arc Gauge + 3-Bank Visualizer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PressureArcGauge(pressureBar = station.currentPressureBar, sizeDp = 100)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Decanting Cascade", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                        CascadeGauge3Bank(currentPressureBar = station.currentPressureBar)
                    }
                }

                // Inventory & Operational Metrics
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InventoryRow("Current Pressure:", "${station.currentPressureBar.roundToInt()} bar", if (isCritical) Crimson500 else Emerald400)
                    InventoryRow("Usable CNG Remaining:", "${station.usableMassKg.roundToInt()} kg")
                    InventoryRow("Top-up Needed (to 230 bar):", "${station.topUpDemandKg.roundToInt()} kg")
                    InventoryRow("Estimated Time to Dryout (TTD):", "~${"%.1f".format(station.ttdHours)} hours", if (isCritical) Crimson500 else Amber500)
                    InventoryRow("Decanting Bays Status:", "${station.openBays} of ${station.totalBays} Open")
                    InventoryRow("Geometric Vessel Volume:", "${volumeLiters.toInt()} Liters")
                    InventoryRow("Baseline Daily Demand:", "${station.baselineDemandKg.roundToInt()} kg/day")
                    InventoryRow("Projected Daily Demand:", "${station.projectedDemandKg.roundToInt()} kg/day")
                    rawEntity?.motherStationName?.let {
                        InventoryRow("Supplying CGS Hub:", it)
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val st = station
                            selectedStationForDetail = null
                            selectedStationForPressureUpdate = st
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update Gauge")
                    }

                    Button(
                        onClick = {
                            selectedStationForDetail = null
                            onNavigateToRecommendation()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCritical) Crimson500 else colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Find Tanker", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Manual Pressure Slider Dialog
    if (selectedStationForPressureUpdate != null) {
        val target = selectedStationForPressureUpdate!!
        var sliderPressure by remember { mutableFloatStateOf(target.currentPressureBar.toFloat()) }

        AlertDialog(
            onDismissRequest = { selectedStationForPressureUpdate = null },
            title = {
                Text(
                    text = "Update Gauge: ${target.stationName}",
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Simulate or enter telemetry gauge reading (Cutoff: 50 bar, Max: 230 bar):",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Current Reading:", color = colorScheme.onSurface, fontSize = 13.sp)
                        Text(
                            text = "${sliderPressure.roundToInt()} bar",
                            color = if (sliderPressure < 60) Crimson500 else if (sliderPressure <= 120) Amber500 else Emerald400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Slider(
                        value = sliderPressure,
                        onValueChange = { sliderPressure = it },
                        valueRange = 50f..230f,
                        colors = SliderDefaults.colors(
                            thumbColor = colorScheme.primary,
                            activeTrackColor = colorScheme.primary,
                            inactiveTrackColor = colorScheme.surfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePressure(target.stationId, sliderPressure.toDouble())
                        selectedStationForPressureUpdate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
                ) {
                    Text("Apply Pressure", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedStationForPressureUpdate = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surface
        )
    }

    // Add New Daughter Station Dialog (Admin)
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var latStr by remember { mutableStateOf("17.400") }
        var lngStr by remember { mutableStateOf("78.500") }
        var volumeStr by remember { mutableStateOf("3500") }
        var baysStr by remember { mutableStateOf("2") }
        var baselineKgStr by remember { mutableStateOf("2500") }
        var distanceKmStr by remember { mutableStateOf("50") }
        var traffic by remember { mutableStateOf("moderate") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("+ Add CNG Daughter Station", color = colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Station Name (e.g. Kompally FS)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Station Code (e.g. kompally)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = latStr,
                                onValueChange = { latStr = it },
                                label = { Text("Latitude") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lngStr,
                                onValueChange = { lngStr = it },
                                label = { Text("Longitude") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = volumeStr,
                                onValueChange = { volumeStr = it },
                                label = { Text("Cascade Liters") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = baysStr,
                                onValueChange = { baysStr = it },
                                label = { Text("Decanting Bays") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = baselineKgStr,
                                onValueChange = { baselineKgStr = it },
                                label = { Text("Sales (kg/day)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = distanceKmStr,
                                onValueChange = { distanceKmStr = it },
                                label = { Text("Distance (km)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = latStr.toDoubleOrNull() ?: 17.400
                        val lng = lngStr.toDoubleOrNull() ?: 78.500
                        val volume = volumeStr.toDoubleOrNull() ?: 3500.0
                        val bays = baysStr.toIntOrNull() ?: 2
                        val baseline = baselineKgStr.toDoubleOrNull() ?: 2500.0
                        val distance = distanceKmStr.toDoubleOrNull() ?: 50.0

                        if (name.isNotBlank() && code.isNotBlank()) {
                            onAddStation(name, code, lat, lng, volume, bays, baseline, distance, traffic)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
                ) {
                    Text("Create Station", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surface
        )
    }

    // Add New Mother Station Dialog (Admin)
    if (showAddMotherDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var latStr by remember { mutableStateOf("17.250") }
        var lngStr by remember { mutableStateOf("78.450") }
        var volumeStr by remember { mutableStateOf("12000") }
        var baysStr by remember { mutableStateOf("4") }
        var compressorCapacityStr by remember { mutableStateOf("1800") }

        AlertDialog(
            onDismissRequest = { showAddMotherDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = Cyan500, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+ Add CGS Mother Station", color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Mother Hub Name (e.g. CGS Ghatkesar)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Hub Code (e.g. cgs-ghatkesar)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = compressorCapacityStr,
                                onValueChange = { compressorCapacityStr = it },
                                label = { Text("Compressor (SCMH)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = baysStr,
                                onValueChange = { baysStr = it },
                                label = { Text("Fast-Fill Bays") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = volumeStr,
                            onValueChange = { volumeStr = it },
                            label = { Text("Cascade Buffer Liters (e.g. 12000)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = latStr,
                                onValueChange = { latStr = it },
                                label = { Text("Latitude") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lngStr,
                                onValueChange = { lngStr = it },
                                label = { Text("Longitude") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = latStr.toDoubleOrNull() ?: 17.250
                        val lng = lngStr.toDoubleOrNull() ?: 78.450
                        val volume = volumeStr.toDoubleOrNull() ?: 12000.0
                        val bays = baysStr.toIntOrNull() ?: 4
                        val compressor = compressorCapacityStr.toDoubleOrNull() ?: 1800.0

                        if (name.isNotBlank() && code.isNotBlank()) {
                            onAddMotherStation(name, code, lat, lng, volume, bays, compressor)
                            showAddMotherDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Color.White)
                ) {
                    Text("Create Mother Hub", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMotherDialog = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surface
        )
    }

    // Edit Station Dialog (Admin)
    if (editingStation != null) {
        val target = editingStation!!
        val isMother = target.stationType == "MOTHER"

        var editName by remember { mutableStateOf(target.name) }
        var editLatStr by remember { mutableStateOf(target.lat.toString()) }
        var editLngStr by remember { mutableStateOf(target.lng.toString()) }
        var editVolumeStr by remember { mutableStateOf(target.geometricVolumeLiters.roundToInt().toString()) }
        var editBaysStr by remember { mutableStateOf(target.totalBays.toString()) }
        var editBaselineKgStr by remember { mutableStateOf(target.baselineDailyDemandKg.roundToInt().toString()) }
        var editDistanceKmStr by remember { mutableStateOf(target.distanceKm.roundToInt().toString()) }
        var editTraffic by remember { mutableStateOf(target.traffic) }
        var editCompressorStr by remember { mutableStateOf(target.compressorCapacityScmh.roundToInt().toString()) }
        var editMotherStationName by remember { mutableStateOf(target.motherStationName ?: "CGS Shamshabad Mother Hub") }

        AlertDialog(
            onDismissRequest = { editingStation = null },
            title = {
                Text(
                    text = if (isMother) "Edit Mother Station: ${target.name}" else "Edit Daughter Station: ${target.name}",
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Station Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = target.id,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Station ID (Immutable)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (!isMother) {
                        item {
                            OutlinedTextField(
                                value = editMotherStationName,
                                onValueChange = { editMotherStationName = it },
                                label = { Text("Supplying Mother Station (CGS)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editVolumeStr,
                                onValueChange = { editVolumeStr = it },
                                label = { Text("Cascade Liters") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editBaysStr,
                                onValueChange = { editBaysStr = it },
                                label = { Text("Bays") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (isMother) {
                        item {
                            OutlinedTextField(
                                value = editCompressorStr,
                                onValueChange = { editCompressorStr = it },
                                label = { Text("Compressor Capacity (SCMH)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editBaselineKgStr,
                                    onValueChange = { editBaselineKgStr = it },
                                    label = { Text("Sales (kg/day)") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = editDistanceKmStr,
                                    onValueChange = { editDistanceKmStr = it },
                                    label = { Text("Distance (km)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editLatStr,
                                onValueChange = { editLatStr = it },
                                label = { Text("Latitude") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editLngStr,
                                onValueChange = { editLngStr = it },
                                label = { Text("Longitude") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = editLatStr.toDoubleOrNull() ?: target.lat
                        val lng = editLngStr.toDoubleOrNull() ?: target.lng
                        val volume = editVolumeStr.toDoubleOrNull() ?: target.geometricVolumeLiters
                        val bays = editBaysStr.toIntOrNull() ?: target.totalBays
                        val baseline = editBaselineKgStr.toDoubleOrNull() ?: target.baselineDailyDemandKg
                        val distance = editDistanceKmStr.toDoubleOrNull() ?: target.distanceKm
                        val compressor = editCompressorStr.toDoubleOrNull() ?: target.compressorCapacityScmh

                        val updated = target.copy(
                            name = editName,
                            lat = lat,
                            lng = lng,
                            geometricVolumeLiters = volume,
                            totalBays = bays,
                            baselineDailyDemandKg = baseline,
                            distanceKm = distance,
                            traffic = editTraffic,
                            motherStationName = if (!isMother) editMotherStationName else null,
                            compressorCapacityScmh = if (isMother) compressor else 1200.0
                        )
                        onUpdateStation(updated)
                        editingStation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingStation = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surface
        )
    }

    // Delete Station Confirmation Dialog (Admin)
    if (deletingStation != null) {
        val target = deletingStation!!
        AlertDialog(
            onDismissRequest = { deletingStation = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Crimson500, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Confirm Deletion", color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete '${target.name}' (${target.id})?\n\nThis will remove it from real-time monitoring and routing optimization.",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStation(target.id)
                        deletingStation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson500)
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingStation = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surface
        )
    }
}

@Composable
private fun InventoryRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(text = value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}
