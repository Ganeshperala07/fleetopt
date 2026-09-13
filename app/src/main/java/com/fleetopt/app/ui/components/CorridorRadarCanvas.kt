package com.fleetopt.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetopt.app.data.local.entity.HcvEntity
import com.fleetopt.app.data.local.entity.StationEntity
import com.fleetopt.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive Offline Corridor Radar Map Canvas.
 * Projects CGS Hub, daughter stations, highway corridor polylines, and moving tankers.
 * Operates standalone or as a zero-dependency fallback for Google Maps.
 */
@Composable
fun CorridorRadarCanvas(
    stations: List<StationEntity>,
    tankers: List<HcvEntity>,
    onStationClick: (StationEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Animated radar pulse ring for critical stations
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Center reference (CGS Mother Station: 17.4526, 78.3312)
    val cgsLat = 17.4526
    val cgsLng = 78.3312

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Slate950)
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(stations) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val scale = minOf(size.width, size.height) * 0.40f / 0.55f

                        for (station in stations) {
                            val dx = ((station.lng - cgsLng) * scale).toFloat()
                            val dy = -((station.lat - cgsLat) * scale).toFloat()
                            val stationPos = Offset(centerX + dx, centerY + dy)
                            val distance = (tapOffset - stationPos).getDistance()
                            if (distance < 40f) {
                                onStationClick(station)
                                break
                            }
                        }
                    }
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val cgsPos = Offset(centerX, centerY)

            // Dynamic scale: span across Hyderabad / Medak corridors (~0.55 deg)
            val scale = minOf(size.width, size.height) * 0.40f / 0.55f

            // 1. Concentric radar distance rings (25 km, 50 km, 75 km, 100 km)
            val ringRadii = listOf(0.25f, 0.50f, 0.75f, 1.0f)
            ringRadii.forEachIndexed { index, fraction ->
                val r = (minOf(size.width, size.height) * 0.46f) * fraction
                drawCircle(
                    color = Slate800.copy(alpha = 0.6f),
                    radius = r,
                    center = cgsPos,
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                )
            }

            // Crosshairs
            drawLine(
                color = Slate800.copy(alpha = 0.4f),
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Slate800.copy(alpha = 0.4f),
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 1.dp.toPx()
            )

            // 2. Corridors & Daughter Station Nodes
            stations.forEach { station ->
                val dx = ((station.lng - cgsLng) * scale).toFloat()
                val dy = -((station.lat - cgsLat) * scale).toFloat()
                val stationPos = Offset(centerX + dx, centerY + dy)

                // Corridor traffic line color
                val corridorColor = when (station.traffic.lowercase()) {
                    "low" -> TrafficLowGreen.copy(alpha = 0.7f)
                    "moderate" -> TrafficModerateAmber.copy(alpha = 0.7f)
                    else -> TrafficHeavyRed.copy(alpha = 0.8f)
                }

                // Draw corridor highway polyline
                drawLine(
                    color = corridorColor,
                    start = cgsPos,
                    end = stationPos,
                    strokeWidth = 2.5.dp.toPx()
                )

                // Station node urgency color
                val stationNodeColor = when {
                    station.currentPressureBar < 60.0 -> Red500
                    station.currentPressureBar <= 120.0 -> Amber500
                    else -> Emerald500
                }

                // If critical (<60 bar), draw pulsating radar ripple
                if (station.currentPressureBar < 60.0) {
                    drawCircle(
                        color = Red500.copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = stationPos,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Node circle
                drawCircle(
                    color = Slate900,
                    radius = 9.dp.toPx(),
                    center = stationPos
                )
                drawCircle(
                    color = stationNodeColor,
                    radius = 7.dp.toPx(),
                    center = stationPos
                )

                // Station label text
                val label = "${station.name} (${station.currentPressureBar.toInt()}b)"
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(stationPos.x - 40f, stationPos.y + 14f),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // 3. Moving Tankers (En Route)
            tankers.filter { it.status == "ON_TRIP" }.forEach { tanker ->
                val dx = ((tanker.lng - cgsLng) * scale).toFloat()
                val dy = -((tanker.lat - cgsLat) * scale).toFloat()
                val tankerPos = Offset(centerX + dx, centerY + dy)

                // Tanker blip
                drawCircle(
                    color = Cyan500,
                    radius = 6.dp.toPx(),
                    center = tankerPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = tankerPos
                )

                // Tanker vehicle reg label
                drawText(
                    textMeasurer = textMeasurer,
                    text = "🚛 ${tanker.registration.takeLast(4)}",
                    topLeft = Offset(tankerPos.x + 8f, tankerPos.y - 12f),
                    style = TextStyle(
                        color = Cyan500,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // 4. CGS Mother Station (Center Node)
            drawCircle(
                color = Teal500.copy(alpha = 0.3f),
                radius = 16.dp.toPx(),
                center = cgsPos
            )
            drawCircle(
                color = Slate900,
                radius = 11.dp.toPx(),
                center = cgsPos
            )
            drawCircle(
                color = Teal500,
                radius = 8.dp.toPx(),
                center = cgsPos
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "CGS HUB (230b)",
                topLeft = Offset(cgsPos.x - 38f, cgsPos.y - 28f),
                style = TextStyle(
                    color = Teal500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Legend overlay at top left
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Slate900.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Text(
                text = "CORRIDOR RADAR",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier.size(6.dp).background(Red500, RoundedCornerShape(3.dp)))
                Text("<60b (Critical)", color = Slate300, fontSize = 9.sp)
                Box(Modifier.size(6.dp).background(Amber500, RoundedCornerShape(3.dp)))
                Text("60-120b", color = Slate300, fontSize = 9.sp)
                Box(Modifier.size(6.dp).background(Emerald500, RoundedCornerShape(3.dp)))
                Text(">120b", color = Slate300, fontSize = 9.sp)
            }
        }
    }
}
