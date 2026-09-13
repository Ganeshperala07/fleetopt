package com.fleetopt.app.core.forecasting

import org.junit.Assert.*
import org.junit.Test

class CngDemandForecastingEngineTest {

    @Test
    fun testBaselineDemandWma7Days() {
        // 7 days of sales: [2000, 2000, 2000, 2000, 2000, 2000, 3000] (day 7 spike)
        val sales = listOf(2000.0, 2000.0, 2000.0, 2000.0, 2000.0, 2000.0, 3000.0)
        val baseline = CngDemandForecastingEngine.computeBaselineDailyDemand(sales)

        // Since day 7 has 30% weight, baseline should be noticeably pulled up:
        // (0.70 * 2000) + (0.30 * 3000) = 1400 + 900 = 2300
        assertEquals(2300.0, baseline, 1.0)
    }

    @Test
    fun testDiurnalRushProfiles() {
        // Morning rush (8 AM) -> 1.6x
        assertEquals(1.60, CngDemandForecastingEngine.getDiurnalMultiplier(8), 0.01)

        // Midday steady (12 PM) -> 0.9x
        assertEquals(0.90, CngDemandForecastingEngine.getDiurnalMultiplier(12), 0.01)

        // Evening transit rush (18 PM / 6 PM) -> 1.7x
        assertEquals(1.70, CngDemandForecastingEngine.getDiurnalMultiplier(18), 0.01)

        // Overnight lull (3 AM) -> 0.4x
        assertEquals(0.40, CngDemandForecastingEngine.getDiurnalMultiplier(3), 0.01)
    }

    @Test
    fun testCalendarSurgeMultipliers() {
        val baseDemand = 2000.0

        val weekdayDemand = CngDemandForecastingEngine.computeProjectedDailyDemand(baseDemand, CalendarSurge.WEEKDAY)
        assertEquals(2000.0, weekdayDemand, 0.01)

        val weekendDemand = CngDemandForecastingEngine.computeProjectedDailyDemand(baseDemand, CalendarSurge.WEEKEND)
        assertEquals(2400.0, weekendDemand, 0.01) // +20%

        val festivalDemand = CngDemandForecastingEngine.computeProjectedDailyDemand(baseDemand, CalendarSurge.FESTIVAL)
        assertEquals(3000.0, festivalDemand, 0.01) // +50%
    }

    @Test
    fun testBlendedBurnRateTelemetryBlending() {
        val baseDaily = 2400.0 // 100 kg/h average
        val hour = 12 // 0.9x -> 90 kg/h historical

        // When telemetry active (alpha = 0.65) with live reading of 150 kg/h
        val blended = CngDemandForecastingEngine.computeBlendedForecastBurnRate(
            telemetryBurnRateKgPerHour = 150.0,
            isTelemetryActive = true,
            baselineDailyDemandKg = baseDaily,
            hourOfDay = hour
        )
        // 0.65 * 150 + 0.35 * 90 = 97.5 + 31.5 = 129.0
        assertEquals(129.0, blended, 0.1)

        // When telemetry drops (isTelemetryActive = false, alpha = 0.0) -> pure historical 90 kg/h
        val fallback = CngDemandForecastingEngine.computeBlendedForecastBurnRate(
            telemetryBurnRateKgPerHour = 150.0,
            isTelemetryActive = false,
            baselineDailyDemandKg = baseDaily,
            hourOfDay = hour
        )
        assertEquals(90.0, fallback, 0.1)
    }
}
