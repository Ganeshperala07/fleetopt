package com.fleetopt.app.core.forecasting

import kotlin.math.max

/**
 * Calendar and Seasonality Surge Multipliers for CNG Demand.
 */
enum class CalendarSurge(val multiplier: Double, val displayName: String, val badgeText: String) {
    WEEKDAY(1.0, "Normal Weekday", "1.0x Baseline"),
    WEEKEND(1.2, "Weekend Surge", "+20% Demand"),
    FESTIVAL(1.5, "Major Festival / Holiday", "+50% Demand")
}

/**
 * Historical Sales Velocity & Time-Series Demand Forecasting Engine for CNG Stations.
 */
object CngDemandForecastingEngine {

    // 7-Day Weighted Moving Average weights:
    // Index 0 = Yesterday (d-1), Index 6 = Same day last week (d-7)
    // Same day last week has the highest weight (0.30) due to weekly commuting patterns.
    val WMA_WEIGHTS_7_DAYS = doubleArrayOf(0.20, 0.15, 0.12, 0.10, 0.08, 0.05, 0.30)

    const val TELEMETRY_ALPHA_ACTIVE: Double = 0.65
    const val TELEMETRY_ALPHA_FALLBACK: Double = 0.00

    /**
     * Calculates diurnal rush hour multiplier for a specific hour of the day (0 to 23).
     *
     * - 07:00 to 10:00 (Morning auto/cab/bus rush): 1.6x
     * - 10:00 to 17:00 (Midday commercial delivery & intercity): 0.9x
     * - 17:00 to 21:00 (Evening transit & carpool rush): 1.7x
     * - 21:00 to 07:00 (Overnight lull / long-haul trucks): 0.4x
     */
    fun getDiurnalMultiplier(hourOfDay: Int): Double {
        return when (hourOfDay % 24) {
            in 7..9 -> 1.60
            in 10..16 -> 0.90
            in 17..20 -> 1.70
            else -> 0.40
        }
    }

    /**
     * Computes baseline daily demand (D_baseline) using 7-day Weighted Moving Average.
     * If fewer than 7 days are provided, falls back to simple average.
     */
    fun computeBaselineDailyDemand(pastDailySalesKg: List<Double>): Double {
        if (pastDailySalesKg.isEmpty()) return 2000.0 // Reasonable fallback default
        if (pastDailySalesKg.size < 7) {
            return pastDailySalesKg.average()
        }
        val recent7 = pastDailySalesKg.takeLast(7)
        var weightedSum = 0.0
        var totalWeight = 0.0
        for (i in 0 until 7) {
            val weight = WMA_WEIGHTS_7_DAYS[i]
            weightedSum += recent7[i] * weight
            totalWeight += weight
        }
        return if (totalWeight > 0.0) weightedSum / totalWeight else recent7.average()
    }

    /**
     * Expected Daily Projected Demand (D_projected) scaled by Calendar Multiplier:
     * D_projected = D_baseline * F_calendar
     */
    fun computeProjectedDailyDemand(
        baselineDailyDemandKg: Double,
        surge: CalendarSurge
    ): Double {
        return max(0.0, baselineDailyDemandKg * surge.multiplier)
    }

    /**
     * Historical Hourly Sales Velocity for a given hour of day:
     * m_dot_historical = (D_baseline / 24) * f_diurnal
     */
    fun computeHistoricalHourlyBurnRate(
        baselineDailyDemandKg: Double,
        hourOfDay: Int
    ): Double {
        val averageHourlyRate = max(1.0, baselineDailyDemandKg / 24.0)
        val diurnalFactor = getDiurnalMultiplier(hourOfDay)
        return averageHourlyRate * diurnalFactor
    }

    /**
     * Blended Burn Rate Model:
     * m_dot_forecast(t) = alpha * m_dot_telemetry(t) + (1 - alpha) * m_dot_historical(t, hour, day)
     *
     * @param telemetryBurnRateKgPerHour Real-time instantaneous mass flow rate from SCADA / pressure delta
     * @param isTelemetryActive Whether live pressure telemetry is currently streaming and fresh (< 30 min)
     * @param baselineDailyDemandKg Station baseline 7-day demand
     * @param hourOfDay Current hour (0 to 23)
     */
    fun computeBlendedForecastBurnRate(
        telemetryBurnRateKgPerHour: Double,
        isTelemetryActive: Boolean,
        baselineDailyDemandKg: Double,
        hourOfDay: Int,
        surge: CalendarSurge = CalendarSurge.WEEKDAY
    ): Double {
        val alpha = if (isTelemetryActive) TELEMETRY_ALPHA_ACTIVE else TELEMETRY_ALPHA_FALLBACK
        val historicalRate = computeHistoricalHourlyBurnRate(baselineDailyDemandKg, hourOfDay) * surge.multiplier
        val effectiveTelemetry = if (telemetryBurnRateKgPerHour > 0.0) telemetryBurnRateKgPerHour else historicalRate
        val blended = (alpha * effectiveTelemetry) + ((1.0 - alpha) * historicalRate)
        return max(5.0, blended) // Minimum baseline consumption floor
    }
}
