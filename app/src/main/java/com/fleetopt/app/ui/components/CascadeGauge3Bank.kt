package com.fleetopt.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetopt.app.ui.theme.*

/**
 * Industrial 3-Bank Cascade Cylinder Bank Visualizer (High, Med, Low banks)
 * used in CNG daughter stations for stepped pressure decanting.
 */
@Composable
fun CascadeGauge3Bank(
    currentPressureBar: Double,
    modifier: Modifier = Modifier
) {
    // Model realistic 3-bank pressure distribution
    // Low Bank: up to 100 bar
    // Med Bank: up to 170 bar
    // High Bank: up to 230 bar
    val lowBankPressure = currentPressureBar.coerceIn(50.0, 100.0)
    val medBankPressure = currentPressureBar.coerceIn(50.0, 170.0)
    val highBankPressure = currentPressureBar.coerceIn(50.0, 230.0)

    val lowFraction = ((lowBankPressure - 50.0) / 50.0).toFloat().coerceIn(0.05f, 1.0f)
    val medFraction = ((medBankPressure - 50.0) / 120.0).toFloat().coerceIn(0.05f, 1.0f)
    val highFraction = ((highBankPressure - 50.0) / 180.0).toFloat().coerceIn(0.05f, 1.0f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Slate850)
            .border(1.dp, Slate700, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "3-BANK CASCADE CYLINDERS",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "${"%.1f".format(currentPressureBar)} bar Total",
                style = MaterialTheme.typography.labelSmall,
                color = if (currentPressureBar < 60.0) Red400 else if (currentPressureBar <= 120.0) Amber400 else Emerald400,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BankCylinderItem(
                bankName = "High Bank",
                pressureBar = highBankPressure,
                fraction = highFraction,
                color = if (highBankPressure < 90) Red500 else Emerald500,
                modifier = Modifier.weight(1f)
            )
            BankCylinderItem(
                bankName = "Medium Bank",
                pressureBar = medBankPressure,
                fraction = medFraction,
                color = if (medBankPressure < 70) Red500 else Teal500,
                modifier = Modifier.weight(1f)
            )
            BankCylinderItem(
                bankName = "Low Bank",
                pressureBar = lowBankPressure,
                fraction = lowFraction,
                color = if (lowBankPressure < 60) Red500 else Blue500,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BankCylinderItem(
    bankName: String,
    pressureBar: Double,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "bank_fraction"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cylinder graphic container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Slate900)
                .border(1.dp, Slate700, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Gas fill level
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFraction)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.5f))
                        )
                    )
            )

            // Pressure text inside cylinder
            Text(
                text = "${pressureBar.toInt()}b",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = bankName,
            style = MaterialTheme.typography.bodySmall,
            color = Slate400,
            fontSize = 10.sp
        )
    }
}
