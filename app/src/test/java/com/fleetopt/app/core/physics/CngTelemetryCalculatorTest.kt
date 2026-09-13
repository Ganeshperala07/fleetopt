package com.fleetopt.app.core.physics

import org.junit.Assert.*
import org.junit.Test

class CngTelemetryCalculatorTest {

    @Test
    fun testCompressibilityFactorZ() {
        // At 50 bar, Z should be ≈ 0.93
        val z50 = CngTelemetryCalculator.calculateCompressibilityZ(50.0)
        assertEquals(0.93, z50, 0.01)

        // At 230 bar, Z should be ≈ 0.83
        val z230 = CngTelemetryCalculator.calculateCompressibilityZ(230.0)
        assertEquals(0.83, z230, 0.01)

        // At intermediate 140 bar, Z should be ≈ 0.88
        val z140 = CngTelemetryCalculator.calculateCompressibilityZ(140.0)
        assertTrue("Z at 140 bar should be between 0.83 and 0.93", z140 in 0.85..0.91)
    }

    @Test
    fun testUsableMassAtDryoutThreshold() {
        // At or below 50 bar dryout cutoff, usable mass MUST be 0 kg
        val usableAt50 = CngTelemetryCalculator.calculateUsableMassKg(50.0, 3000.0)
        assertEquals(0.0, usableAt50, 0.001)

        val usableAt40 = CngTelemetryCalculator.calculateUsableMassKg(40.0, 3000.0)
        assertEquals(0.0, usableAt40, 0.001)

        // At 230 bar, usable mass should be positive and substantial (~400 to 600 kg for 3000L)
        val usableAt230 = CngTelemetryCalculator.calculateUsableMassKg(230.0, 3000.0)
        assertTrue("Usable mass at 230 bar should be > 350 kg", usableAt230 > 350.0)
    }

    @Test
    fun testTopUpReplenishmentDemand() {
        // At 230 bar, replenishment demand should be 0 kg
        val topUpAt230 = CngTelemetryCalculator.calculateTopUpDemandKg(230.0, 3000.0)
        assertEquals(0.0, topUpAt230, 0.001)

        // At 50 bar, replenishment demand should equal the total usable capacity
        val topUpAt50 = CngTelemetryCalculator.calculateTopUpDemandKg(50.0, 3000.0)
        val usableAt230 = CngTelemetryCalculator.calculateUsableMassKg(230.0, 3000.0)
        assertEquals(usableAt230, topUpAt50, 1.0)
    }

    @Test
    fun testTimeToDryoutCalculation() {
        // 100 kg usable with 50 kg/h burn rate -> 2.0 hours TTD
        val ttd = CngTelemetryCalculator.calculateTimeToDryoutHours(100.0, 50.0)
        assertEquals(2.0, ttd, 0.01)

        // 0 kg usable -> 0.0 hours
        val ttdZero = CngTelemetryCalculator.calculateTimeToDryoutHours(0.0, 50.0)
        assertEquals(0.0, ttdZero, 0.01)

        // Very low or zero burn rate -> capped at 99 hours
        val ttdLull = CngTelemetryCalculator.calculateTimeToDryoutHours(100.0, 0.0)
        assertEquals(99.0, ttdLull, 0.01)
    }

    @Test
    fun testJouleThomsonCooling() {
        // Fast decanting with 100 bar pressure drop
        val ambient = 298.15
        val cooled = CngTelemetryCalculator.calculateJouleThomsonDecantingTemperature(ambient, 100.0)
        assertTrue("Decanting gas should experience cooling", cooled < ambient)
        assertEquals(ambient - (100.0 * 0.35), cooled, 0.01)
    }

    @Test
    fun testBayInterlock() {
        // 2 total bays, 2 occupied -> interlocked!
        assertTrue(CngTelemetryCalculator.isBayInterlocked(2, 2))

        // 2 total bays, 1 occupied -> open!
        assertFalse(CngTelemetryCalculator.isBayInterlocked(1, 2))
    }
}
