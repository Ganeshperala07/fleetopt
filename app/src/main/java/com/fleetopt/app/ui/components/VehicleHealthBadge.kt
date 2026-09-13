package com.fleetopt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetopt.app.ui.theme.*


@Composable
fun VehicleHealthBadge(
    isHydroTestValid: Boolean,
    isPrvCertified: Boolean,
    tirePercent: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hydro-test compliance badge
        SafetyTag(
            label = if (isHydroTestValid) "HYDRO OK" else "HYDRO DUE",
            isOk = isHydroTestValid
        )

        // PRV valve safety badge
        SafetyTag(
            label = if (isPrvCertified) "PRV CERT" else "PRV DUE",
            isOk = isPrvCertified
        )

        // Tire health tag
        val tireOk = tirePercent >= 60
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (tireOk) Slate800 else Red500.copy(alpha = 0.15f))
                .border(1.dp, if (tireOk) Slate700 else Red500.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "TIRE $tirePercent%",
                color = if (tireOk) Slate300 else Red400,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SafetyTag(
    label: String,
    isOk: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isOk) Emerald500.copy(alpha = 0.12f) else Red500.copy(alpha = 0.18f))
            .border(1.dp, if (isOk) Emerald500.copy(alpha = 0.35f) else Red500.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = if (isOk) Emerald400 else Red400,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
