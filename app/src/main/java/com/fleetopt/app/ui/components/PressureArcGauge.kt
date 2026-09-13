package com.fleetopt.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetopt.app.ui.theme.*

@Composable
fun PressureArcGauge(
    pressureBar: Double,
    modifier: Modifier = Modifier,
    sizeDp: Int = 110
) {
    val clamped = pressureBar.coerceIn(50.0, 230.0)
    val fraction = ((clamped - 50.0) / 180.0).toFloat()

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600),
        label = "pressure_gauge"
    )

    val statusColor = when {
        pressureBar < 60.0 -> Red500
        pressureBar <= 120.0 -> Amber500
        else -> Emerald500
    }

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 10.dp.toPx()
            val startAngle = 135f
            val sweepAngleTotal = 270f

            // Background track arc
            drawArc(
                color = Slate700,
                startAngle = startAngle,
                sweepAngle = sweepAngleTotal,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )

            // Active pressure fill arc
            drawArc(
                color = statusColor,
                startAngle = startAngle,
                sweepAngle = sweepAngleTotal * animatedFraction,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${pressureBar.toInt()}",
                style = MaterialTheme.typography.titleLarge,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = "BAR",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
