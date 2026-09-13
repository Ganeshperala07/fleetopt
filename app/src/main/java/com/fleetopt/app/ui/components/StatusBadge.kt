package com.fleetopt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetopt.app.ui.theme.*

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val normalized = status.uppercase().trim()
    val (badgeColor, textLabel) = when (normalized) {
        "AVAILABLE" -> Emerald400 to "AVAILABLE"
        "ON_TRIP", "EN_ROUTE" -> Teal400 to "EN ROUTE"
        "DECANTING" -> Cyan500 to "DECANTING"
        "COMPLETED" -> Emerald400 to "COMPLETED"
        "MAINTENANCE" -> Amber400 to "MAINTENANCE"
        "BREAKDOWN" -> Red400 to "BREAKDOWN"
        "CANCELLED" -> Slate400 to "CANCELLED"
        "MOTHER" -> Cyan500 to "CGS HUB"
        "CRITICAL" -> Red400 to "CRITICAL"
        "NORMAL" -> Emerald400 to "NORMAL"
        else -> Slate400 to normalized
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.15f))
            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = textLabel,
            color = badgeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}
