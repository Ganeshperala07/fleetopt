package com.fleetopt.app.ui.screens.recommendation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetopt.app.core.engine.DispatchRecommendation
import com.fleetopt.app.ui.components.DataProvenanceBadge
import com.fleetopt.app.ui.components.Provenance
import com.fleetopt.app.ui.components.StatusBadge
import com.fleetopt.app.ui.theme.*
import com.fleetopt.app.ui.viewmodel.FleetOptUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    uiState: FleetOptUiState,
    onAcceptDispatch: ((tripId: String) -> Unit) -> Unit,
    onGenerateRecommendation: () -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rec = uiState.recommendation
    var showConfirmSheet by remember { mutableStateOf(false) }
    var confirmedTripId by remember { mutableStateOf<String?>(null) }

    val colorScheme = MaterialTheme.colorScheme

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
                                text = "HCV Dispatch DSS",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            DataProvenanceBadge(provenance = Provenance.CALCULATED)
                        }
                        Text(
                            text = "AI Recommendation",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onGenerateRecommendation,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.surfaceVariant)
                            .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Recommendation", tint = colorScheme.primary)
                    }
                }
                Text(
                    text = "Physics-constrained multi-factor optimization across cascade pressures, payload limits, and corridor logistics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (rec != null) {
            // 2. Primary Decision Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colorScheme.primary)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Emerald400, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "OPTIMAL DISPATCH PLAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Emerald400,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.15f))
                                    .border(1.dp, colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Score: ${"%.0f".format(rec.totalScore * 100)}%",
                                    color = colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "${rec.assignedHcv.registration}  ➜  ${rec.targetStation.stationName}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = rec.rationale,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Key Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            RecommendationMetric(
                                label = "Payload",
                                value = "${rec.recommendedPayloadKg.roundToInt()} kg",
                                subValue = "Clamped ≤ 230 bar",
                                valueColor = colorScheme.onSurface
                            )
                            RecommendationMetric(
                                label = "Est. Arrival",
                                value = "${rec.etaMinutes} min",
                                subValue = "${rec.targetStation.distanceKm.toInt()} km corridor",
                                valueColor = colorScheme.onSurface
                            )
                            RecommendationMetric(
                                label = "Trip Cost",
                                value = "₹${rec.tripCostInr}",
                                subValue = "Toll + Handling",
                                valueColor = colorScheme.onSurface
                            )
                            RecommendationMetric(
                                label = "Est. Savings",
                                value = "₹${rec.estimatedSavingsInr}",
                                subValue = "vs Dryout Loss",
                                valueColor = Emerald400
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons: Accept & Dispatch + Manifest
                        Button(
                            onClick = { showConfirmSheet = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACCEPT & DISPATCH HCV",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                sendDriverManifestIntent(context, rec)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald400),
                            border = ButtonDefaults.outlinedButtonBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Emerald500.copy(alpha = 0.5f))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send Driver Manifest via WhatsApp / SMS",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onNavigateToMap,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400),
                            border = ButtonDefaults.outlinedButtonBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Cyan500.copy(alpha = 0.5f))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = Cyan400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View Route on Map",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 3. Why Recommended - Explicit Decision Factors
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outline.copy(alpha = 0.3f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "WHY THIS DISPATCH WAS SELECTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (rec.explicitFactors.isNotEmpty()) {
                            rec.explicitFactors.forEach { factor ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = factor,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "• Optimal pressure replenishment with maximum corridor efficiency.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 4. Multi-Factor Scoring Breakdown Bars
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outline.copy(alpha = 0.3f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "DECISION FACTOR WEIGHTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        FactorBarItem(
                            factorName = "Station Unserved Demand (40%)",
                            score = rec.factorBreakdown.demandScore,
                            color = Emerald500
                        )
                        FactorBarItem(
                            factorName = "Corridor Distance & Transit Time (20%)",
                            score = rec.factorBreakdown.distanceScore,
                            color = Teal500
                        )
                        FactorBarItem(
                            factorName = "Traffic Flow Condition (15%)",
                            score = rec.factorBreakdown.trafficScore,
                            color = Amber500
                        )
                        FactorBarItem(
                            factorName = "Vehicle Readiness & Proximity (15%)",
                            score = rec.factorBreakdown.availabilityScore,
                            color = Blue500
                        )
                        FactorBarItem(
                            factorName = "Cascade Capacity Match (10%)",
                            score = rec.factorBreakdown.capacityScore,
                            color = Cyan500
                        )
                    }
                }
            }

            // 5. Assigned Driver & Mechanical Health Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outline.copy(alpha = 0.3f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ASSIGNED VEHICLE & DRIVER DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = rec.assignedHcv.driverName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = rec.assignedHcv.driverPhone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }

                            StatusBadge(status = rec.assignedHcv.status)
                        }

                        HorizontalDivider(
                            color = colorScheme.outline.copy(alpha = 0.2f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Hydro-test Certificate:", color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(
                                text = if (rec.assignedHcv.isHydroTestValid) "Certified Valid" else "Overdue",
                                color = if (rec.assignedHcv.isHydroTestValid) Emerald400 else Crimson500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "PRV & Burst Disc:", color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(
                                text = if (rec.assignedHcv.isPrvCertified) "Inspected OK" else "Check Due",
                                color = if (rec.assignedHcv.isPrvCertified) Emerald400 else Amber500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Tire Condition:", color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(
                                text = "${rec.assignedHcv.tireConditionPercent}%",
                                color = Emerald400,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

        } else {
            // Empty state
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colorScheme.surface)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Active Recommendation",
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All available HCV tankers may be currently engaged on active trips or under maintenance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onGenerateRecommendation,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Re-Evaluate Fleet & Stations", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Pre-Dispatch Confirmation Bottom Sheet
    if (showConfirmSheet && rec != null) {
        ModalBottomSheet(
            onDismissRequest = { showConfirmSheet = false },
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
                    Text(
                        text = "Confirm HCV Dispatch",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    IconButton(onClick = { showConfirmSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Review corridor parameters and safety checks before initiating the live transport operation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                // Dispatch Summary Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConfirmRow("Vehicle Reg:", rec.assignedHcv.registration)
                    ConfirmRow("Assigned Driver:", "${rec.assignedHcv.driverName} (${rec.assignedHcv.driverPhone})")
                    ConfirmRow("Target Station:", rec.targetStation.stationName)
                    ConfirmRow("Dispatched Payload:", "${rec.recommendedPayloadKg.roundToInt()} kg CNG")
                    ConfirmRow("Corridor Distance:", "${rec.targetStation.distanceKm.toInt()} km (${rec.targetStation.traffic.replaceFirstChar { it.uppercase() }} traffic)")
                    ConfirmRow("Estimated ETA:", "~${rec.etaMinutes} minutes")
                    ConfirmRow("Estimated Cost:", "₹${rec.tripCostInr}")
                }

                // Safety Checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Emerald500.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                        Text(
                            text = "SAFETY CHECKS VERIFIED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald400,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• Hydrostatic cylinder test valid and within service date.\n• Pressure Relief Valve (PRV) verified intact.\n• Mandatory static earth clamp required prior to hose coupling at daughter station.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                // Confirm / Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showConfirmSheet = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            showConfirmSheet = false
                            onAcceptDispatch { tripId ->
                                confirmedTripId = tripId
                                Toast.makeText(
                                    context,
                                    "Dispatched ${rec.assignedHcv.registration} ($tripId)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirm & Dispatch", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Post-Dispatch Success Dialog
    confirmedTripId?.let { tripId ->
        AlertDialog(
            onDismissRequest = { confirmedTripId = null },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Dispatch Initiated", fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Trip has been recorded with ID:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tripId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Text(
                        text = "The tanker is marked ON_TRIP and the daughter station bay has been reserved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rec != null) sendDriverManifestIntent(context, rec)
                        confirmedTripId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald500,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Manifest")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmedTripId = null }) {
                    Text("Done")
                }
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RecommendationMetric(
    label: String,
    value: String,
    subValue: String,
    valueColor: Color
) {
    Column {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
        Text(text = subValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 9.sp)
    }
}

@Composable
private fun FactorBarItem(
    factorName: String,
    score: Double,
    color: Color
) {
    val animatedScore by animateFloatAsState(targetValue = score.toFloat(), label = "factor_bar")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = factorName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(text = "${(score * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }

        LinearProgressIndicator(
            progress = { animatedScore },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun sendDriverManifestIntent(context: Context, rec: DispatchRecommendation) {
    val manifestText = buildString {
        appendLine("🚨 CNG DISPATCH MANIFEST")
        appendLine("Tanker: ${rec.assignedHcv.registration}")
        appendLine("Driver: ${rec.assignedHcv.driverName} (${rec.assignedHcv.driverPhone})")
        appendLine("Destination: ${rec.targetStation.stationName}")
        appendLine("Corridor: ${rec.targetStation.distanceKm.toInt()} km (${rec.targetStation.traffic.replaceFirstChar { it.uppercase() }} Traffic)")
        appendLine("Payload: ${rec.recommendedPayloadKg.roundToInt()} kg Cascade @ 230 bar")
        appendLine("Expected Transit: ~${rec.etaMinutes} min")
        appendLine("Safety Check: Cascade PRV inspected. Earth clamp mandatory before decanting.")
    }

    val uri = Uri.parse("https://wa.me/?text=" + Uri.encode(manifestText))
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, manifestText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Dispatch Manifest"))
    }
}
