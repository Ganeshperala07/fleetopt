package com.fleetopt.app.ui.screens.fleet

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetopt.app.data.local.entity.HcvEntity
import com.fleetopt.app.ui.components.DataProvenanceBadge
import com.fleetopt.app.ui.components.Provenance
import com.fleetopt.app.ui.components.StatusBadge
import com.fleetopt.app.ui.components.VehicleHealthBadge
import com.fleetopt.app.ui.theme.*
import com.fleetopt.app.ui.viewmodel.FleetOptUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetScreen(
    uiState: FleetOptUiState,
    onReportBreakdown: (registration: String, reason: String) -> Unit,
    onClearMaintenance: (registration: String) -> Unit,
    onAddHcv: (
        registration: String,
        driverName: String,
        driverPhone: String,
        capacityKg: Double,
        status: String,
        isHydroTestValid: Boolean,
        hydroTestExpiryDate: String,
        isPrvCertified: Boolean,
        tireConditionPercent: Int,
        heelPressureBar: Double
    ) -> Unit,
    onUpdateHcv: (HcvEntity) -> Unit,
    onDeleteHcv: (registration: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var breakdownTargetHcv by remember { mutableStateOf<HcvEntity?>(null) }
    var selectedHcvForDetail by remember { mutableStateOf<HcvEntity?>(null) }
    var showAddHcvDialog by remember { mutableStateOf(false) }
    var editingHcv by remember { mutableStateOf<HcvEntity?>(null) }
    var deletingHcv by remember { mutableStateOf<HcvEntity?>(null) }

    val filteredFleet = remember(uiState.fleet, selectedFilter, searchQuery) {
        uiState.fleet.filter { hcv ->
            val matchesFilter = when (selectedFilter) {
                "AVAILABLE" -> hcv.status.equals("available", ignoreCase = true)
                "ON_TRIP" -> hcv.status.equals("on_trip", ignoreCase = true)
                "MAINTENANCE" -> hcv.status.equals("maintenance", ignoreCase = true) ||
                        hcv.status.equals("breakdown", ignoreCase = true)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                hcv.registration.contains(searchQuery, ignoreCase = true) ||
                        hcv.driverName.contains(searchQuery, ignoreCase = true) ||
                        hcv.homeMotherStationName.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    val availableCount = uiState.fleet.count { it.status.equals("available", ignoreCase = true) }
    val onTripCount = uiState.fleet.count { it.status.equals("on_trip", ignoreCase = true) }
    val maintenanceCount = uiState.fleet.count {
        it.status.equals("maintenance", ignoreCase = true) || it.status.equals("breakdown", ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            if (uiState.isAdminMode) {
                ExtendedFloatingActionButton(
                    onClick = { showAddHcvDialog = true },
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add HCV") },
                    text = { Text("Add HCV", fontWeight = FontWeight.Bold) }
                )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                    text = "Fleet Operations",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                DataProvenanceBadge(provenance = Provenance.REAL)
                            }
                            Text(
                                text = "HCV Cascade Tankers",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Live telemetry, mechanical health, and driver assignments for all CNG heavy commercial vehicles",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 2. Summary Counters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FleetCounterCard(
                        title = "Available",
                        count = availableCount,
                        color = Emerald400,
                        modifier = Modifier.weight(1f)
                    )
                    FleetCounterCard(
                        title = "On Trip",
                        count = onTripCount,
                        color = Blue500,
                        modifier = Modifier.weight(1f)
                    )
                    FleetCounterCard(
                        title = "Service/Issue",
                        count = maintenanceCount,
                        color = Amber500,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Reg, Driver, or Mother Station...") },
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

            // 4. Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All (${uiState.fleet.size})",
                        "AVAILABLE" to "Available ($availableCount)",
                        "ON_TRIP" to "On Trip ($onTripCount)",
                        "MAINTENANCE" to "Issue ($maintenanceCount)"
                    ).forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = colorScheme.primary,
                                containerColor = colorScheme.surface,
                                labelColor = colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) colorScheme.primary else colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // 5. HCV Cards
            if (filteredFleet.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.surface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No HCV tankers match \"$searchQuery\"" else "No HCV tankers found in this category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(filteredFleet, key = { it.registration }) { hcv ->
                val isBroken = hcv.status.equals("breakdown", ignoreCase = true)
                val isMaintenance = hcv.status.equals("maintenance", ignoreCase = true)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            when {
                                isBroken -> Crimson500.copy(alpha = 0.7f)
                                isMaintenance -> Amber500.copy(alpha = 0.5f)
                                else -> colorScheme.outline.copy(alpha = 0.25f)
                            }
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedHcvForDetail = hcv }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header Row: Reg + Status + Admin Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
                                Text(
                                    text = hcv.registration,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusBadge(status = hcv.status)
                                if (uiState.isAdminMode) {
                                    IconButton(
                                        onClick = { editingHcv = hcv },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit HCV", tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { deletingHcv = hcv },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete HCV", tint = Crimson500, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Driver info, Home Mother Station & Quick Call
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hcv.driverName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${hcv.driverPhone}  •  ${hcv.homeMotherStationName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hcv.driverPhone}"))
                                    context.startActivity(callIntent)
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorScheme.surfaceVariant)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call Driver", tint = Emerald400, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats: Heel pressure, Capacity, Trips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Capacity", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text("${hcv.capacityKg.toInt()} kg", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Residual Heel", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text("${hcv.heelPressureBar.toInt()} bar", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Trips Today", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text("${hcv.tripsToday}", style = MaterialTheme.typography.titleMedium, color = colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "📍 Location: ${hcv.currentLocation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        // Mechanical & Safety Compliance Badges
                        Spacer(modifier = Modifier.height(10.dp))
                        VehicleHealthBadge(
                            isHydroTestValid = hcv.isHydroTestValid,
                            isPrvCertified = hcv.isPrvCertified,
                            tirePercent = hcv.tireConditionPercent
                        )

                        // Breakdown Reason Warning Banner
                        if (!hcv.breakdownReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Crimson500.copy(alpha = 0.12f))
                                    .border(1.dp, Crimson500.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Crimson500, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Incident Report: ${hcv.breakdownReason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Crimson500,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Maintenance / Breakdown Action Buttons
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isBroken || isMaintenance) {
                                Button(
                                    onClick = { onClearMaintenance(hcv.registration) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear Maintenance", fontSize = 12.sp, color = Color.White)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { breakdownTargetHcv = hcv },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber500),
                                    border = ButtonDefaults.outlinedButtonBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(Amber500.copy(alpha = 0.5f))
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Report Issue / Breakdown", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Vehicle Detail Bottom Sheet
    selectedHcvForDetail?.let { hcv ->
        ModalBottomSheet(
            onDismissRequest = { selectedHcvForDetail = null },
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
                            text = hcv.registration,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Assigned to ${hcv.homeMotherStationName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(status = hcv.status)
                }

                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.2f))

                // Detailed Specs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow("Driver Name:", hcv.driverName)
                    DetailRow("Driver Phone:", hcv.driverPhone)
                    DetailRow("Cascade Payload Capacity:", "${hcv.capacityKg.toInt()} kg CNG")
                    DetailRow("Residual Heel Pressure:", "${hcv.heelPressureBar.toInt()} bar")
                    DetailRow("Completed Trips Today:", "${hcv.tripsToday}")
                    DetailRow("Current Location:", hcv.currentLocation)
                    DetailRow("Hydro-test Certificate:", if (hcv.isHydroTestValid) "Valid (Expires ${hcv.hydroTestExpiryDate})" else "OVERDUE EXPIRED")
                    DetailRow("PRV Safety Certification:", if (hcv.isPrvCertified) "Certified OK" else "Inspection Required")
                    DetailRow("Tire Condition Index:", "${hcv.tireConditionPercent}%")
                }

                // Quick Dial Driver Action
                Button(
                    onClick = {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hcv.driverPhone}"))
                        context.startActivity(callIntent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Driver (${hcv.driverName})", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Breakdown Reporting Dialog
    if (breakdownTargetHcv != null) {
        val target = breakdownTargetHcv!!
        var reason by remember { mutableStateOf("Tire Puncture / Slow Leak on Highway") }

        AlertDialog(
            onDismissRequest = { breakdownTargetHcv = null },
            title = { Text("Report Breakdown: ${target.registration}", color = colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Flagging this HCV will immediately remove it from AI dispatch recommendations and alert the fleet controller.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Breakdown Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportBreakdown(target.registration, reason)
                        breakdownTargetHcv = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson500)
                ) {
                    Text("Confirm Breakdown", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { breakdownTargetHcv = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surface
        )
    }

    // Add HCV Dialog (Admin)
    if (showAddHcvDialog) {
        var registration by remember { mutableStateOf("HCV-10 (TS-09-UB-4491)") }
        var driverName by remember { mutableStateOf("Ramesh Kumar") }
        var driverPhone by remember { mutableStateOf("+91 98490 11223") }
        var capacityKgStr by remember { mutableStateOf("650.0") }
        var hydroDate by remember { mutableStateOf("2027-12-31") }
        var isHydroValid by remember { mutableStateOf(true) }
        var isPrvOk by remember { mutableStateOf(true) }
        var tirePercentStr by remember { mutableStateOf("92") }

        AlertDialog(
            onDismissRequest = { showAddHcvDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add CNG Cascade Tanker", color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = registration,
                        onValueChange = { registration = it },
                        label = { Text("Registration / ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = driverName,
                            onValueChange = { driverName = it },
                            label = { Text("Driver Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = driverPhone,
                            onValueChange = { driverPhone = it },
                            label = { Text("Driver Phone") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = capacityKgStr,
                            onValueChange = { capacityKgStr = it },
                            label = { Text("Capacity (kg)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tirePercentStr,
                            onValueChange = { tirePercentStr = it },
                            label = { Text("Tire Health (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = hydroDate,
                        onValueChange = { hydroDate = it },
                        label = { Text("Hydro-Test Expiry") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hydro-Test Valid", color = colorScheme.onSurface, fontSize = 13.sp)
                        Switch(
                            checked = isHydroValid,
                            onCheckedChange = { isHydroValid = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PRV Safety Valve Certified", color = colorScheme.onSurface, fontSize = 13.sp)
                        Switch(
                            checked = isPrvOk,
                            onCheckedChange = { isPrvOk = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cap = capacityKgStr.toDoubleOrNull() ?: 650.0
                        val tire = tirePercentStr.toIntOrNull() ?: 90
                        if (registration.isNotBlank() && driverName.isNotBlank()) {
                            onAddHcv(
                                registration,
                                driverName,
                                driverPhone,
                                cap,
                                "AVAILABLE",
                                isHydroValid,
                                hydroDate,
                                isPrvOk,
                                tire,
                                25.0
                            )
                            showAddHcvDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
                ) {
                    Text("Add Tanker", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHcvDialog = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surface
        )
    }

    // Edit HCV Dialog (Admin)
    if (editingHcv != null) {
        val target = editingHcv!!
        var driverName by remember(target) { mutableStateOf(target.driverName) }
        var driverPhone by remember(target) { mutableStateOf(target.driverPhone) }
        var capacityKgStr by remember(target) { mutableStateOf(target.capacityKg.toString()) }
        var tirePercentStr by remember(target) { mutableStateOf(target.tireConditionPercent.toString()) }
        var hydroDate by remember(target) { mutableStateOf(target.hydroTestExpiryDate) }
        var isHydroValid by remember(target) { mutableStateOf(target.isHydroTestValid) }
        var isPrvOk by remember(target) { mutableStateOf(target.isPrvCertified) }
        var selectedMotherHubId by remember(target) { mutableStateOf(target.homeMotherStationId) }

        val availableMothers = uiState.motherStations

        AlertDialog(
            onDismissRequest = { editingHcv = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Tanker: ${target.registration}", color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = driverName,
                            onValueChange = { driverName = it },
                            label = { Text("Driver Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = driverPhone,
                            onValueChange = { driverPhone = it },
                            label = { Text("Driver Phone") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = capacityKgStr,
                            onValueChange = { capacityKgStr = it },
                            label = { Text("Capacity (kg)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tirePercentStr,
                            onValueChange = { tirePercentStr = it },
                            label = { Text("Tire Health (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = hydroDate,
                        onValueChange = { hydroDate = it },
                        label = { Text("Hydro-Test Expiry") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (availableMothers.isNotEmpty()) {
                        Text("Assigned Mother Hub:", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableMothers.forEach { mother ->
                                val isSelected = mother.id == selectedMotherHubId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMotherHubId = mother.id },
                                    label = { Text(mother.name.replace("CGS ", ""), fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hydro-Test Valid", color = colorScheme.onSurface, fontSize = 13.sp)
                        Switch(
                            checked = isHydroValid,
                            onCheckedChange = { isHydroValid = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PRV Safety Valve Certified", color = colorScheme.onSurface, fontSize = 13.sp)
                        Switch(
                            checked = isPrvOk,
                            onCheckedChange = { isPrvOk = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cap = capacityKgStr.toDoubleOrNull() ?: target.capacityKg
                        val tire = tirePercentStr.toIntOrNull() ?: target.tireConditionPercent
                        val motherName = availableMothers.firstOrNull { it.id == selectedMotherHubId }?.name ?: target.homeMotherStationName

                        val updated = target.copy(
                            driverName = driverName.trim(),
                            driverPhone = driverPhone.trim(),
                            capacityKg = cap,
                            tireConditionPercent = tire,
                            hydroTestExpiryDate = hydroDate.trim(),
                            isHydroTestValid = isHydroValid,
                            isPrvCertified = isPrvOk,
                            homeMotherStationId = selectedMotherHubId,
                            homeMotherStationName = motherName
                        )
                        onUpdateHcv(updated)
                        editingHcv = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingHcv = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surface
        )
    }

    // Delete HCV Confirmation Dialog (Admin)
    if (deletingHcv != null) {
        val target = deletingHcv!!
        AlertDialog(
            onDismissRequest = { deletingHcv = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Crimson500)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete HCV: ${target.registration}", color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete tanker '${target.registration}' driven by ${target.driverName}? This will remove it from all AI dispatches and tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteHcv(target.registration)
                        deletingHcv = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson500)
                ) {
                    Text("Delete Tanker", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingHcv = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surface
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FleetCounterCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "$count", style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}
