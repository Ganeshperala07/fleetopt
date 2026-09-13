package com.fleetopt.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.fleetopt.app.core.simulation.SimulationScenario
import com.fleetopt.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    isSimulationActive: Boolean,
    currentScenario: SimulationScenario,
    onToggleSimulation: (Boolean) -> Unit,
    onScenarioChanged: (SimulationScenario) -> Unit,
    onResetSimulation: () -> Unit,
    isAdminMode: Boolean,
    onChangePin: (oldPin: String, newPin: String) -> Boolean,
    onLockAdmin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangePinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                .padding(FleetOptSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FleetOptSpacing.lg)
        ) {
            // 1. Appearance / Theme Mode Section
            item {
                SectionHeader(title = "Appearance & Theme")
                Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                Card(
                    shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FleetOptSpacing.md)) {
                        Text(
                            text = "Monochromatic Theme Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Restrained enterprise visual system optimized for operational readability",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(FleetOptSpacing.md))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)
                        ) {
                            listOf(
                                ThemeMode.SYSTEM to ("System" to Icons.Default.BrightnessAuto),
                                ThemeMode.LIGHT to ("Light" to Icons.Default.LightMode),
                                ThemeMode.DARK to ("Dark" to Icons.Default.DarkMode)
                            ).forEach { (mode, pair) ->
                                val (label, icon) = pair
                                val isSelected = currentThemeMode == mode

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(FleetOptSpacing.radiusSm))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(FleetOptSpacing.radiusSm)
                                        )
                                        .clickable { onThemeModeChanged(mode) }
                                        .padding(vertical = FleetOptSpacing.md),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Simulation Sandbox Mode
            item {
                SectionHeader(title = "Simulation Sandbox")
                Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                Card(
                    shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FleetOptSpacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Simulation Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Simulate demand spikes and pressure draws without modifying actual records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = isSimulationActive,
                                onCheckedChange = onToggleSimulation,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Teal500
                                )
                            )
                        }

                        if (isSimulationActive) {
                            Spacer(modifier = Modifier.height(FleetOptSpacing.md))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                            Text(
                                text = "Active Simulation Scenario:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(FleetOptSpacing.xs))

                            SimulationScenario.values().forEach { scenario ->
                                val isChosen = currentScenario == scenario
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(FleetOptSpacing.radiusSm))
                                        .background(if (isChosen) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { onScenarioChanged(scenario) }
                                        .padding(horizontal = FleetOptSpacing.sm, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isChosen,
                                        onClick = { onScenarioChanged(scenario) }
                                    )
                                    Spacer(modifier = Modifier.width(FleetOptSpacing.xs))
                                    Column {
                                        Text(
                                            text = scenario.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = scenario.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                            OutlinedButton(
                                onClick = onResetSimulation,
                                shape = RoundedCornerShape(FleetOptSpacing.radiusSm),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(FleetOptSpacing.xs))
                                Text("Reset Simulation to Baseline")
                            }
                        }
                    }
                }
            }

            // 3. Security & Admin Configuration
            item {
                SectionHeader(title = "Security & Admin Access")
                Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                Card(
                    shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                                    text = "Admin Mode Status",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAdminMode) "Unlocked (Full privileges)" else "Locked (Operator safe mode)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAdminMode) Teal400 else Slate400
                                )
                            }

                            if (isAdminMode) {
                                OutlinedButton(onClick = onLockAdmin) {
                                    Text("Lock Terminal")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(FleetOptSpacing.md))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                        Button(
                            onClick = { showChangePinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(FleetOptSpacing.radiusSm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(FleetOptSpacing.xs))
                            Text("Change Admin PIN", color = MaterialTheme.colorScheme.onPrimary)
                        }

                        Spacer(modifier = Modifier.height(FleetOptSpacing.xs))
                        Text(
                            text = "Admin credentials are protected by salted SHA-256 cryptographic hashing.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. About & Compliance
            item {
                SectionHeader(title = "About FleetOpt DSS")
                Spacer(modifier = Modifier.height(FleetOptSpacing.sm))

                Card(
                    shape = RoundedCornerShape(FleetOptSpacing.radiusMd),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(FleetOptSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InfoRow(label = "Application Version", value = "v1.1.0 Production (Build 2)")
                        InfoRow(label = "Physics Model", value = "AGA-8 Compressibility / Joule-Thomson")
                        InfoRow(label = "Dryout Cutoff", value = "50 bar (BIS / PNGRB Standard)")
                        InfoRow(label = "Demand Forecasting", value = "7-Day WMA + Diurnal Hourly Curve")
                        InfoRow(label = "Persistence", value = "Room SQLite Local Encrypted Sandbox")
                        InfoRow(label = "Data Safety", value = "100% Offline-resilient, Zero tracking")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(FleetOptSpacing.xxl)) }
        }
    }

    // Change Admin PIN Dialog
    if (showChangePinDialog) {
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Update Admin PIN", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(FleetOptSpacing.sm)) {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { oldPin = it },
                        label = { Text("Current PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("New PIN (min 4 digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it },
                        label = { Text("Confirm New PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (errorMessage != null) {
                        Text(text = errorMessage!!, color = Red400, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin != confirmPin) {
                            errorMessage = "New PIN and confirmation do not match"
                            return@Button
                        }
                        if (newPin.trim().length < 4) {
                            errorMessage = "PIN must be at least 4 digits"
                            return@Button
                        }
                        val success = onChangePin(oldPin, newPin)
                        if (success) {
                            showChangePinDialog = false
                        } else {
                            errorMessage = "Current PIN is incorrect"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save PIN", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
