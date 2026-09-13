package com.fleetopt.app.core.physics

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Real Gas Physics Calculator for CNG Distribution Networks.
 *
 * Implements real gas state calculations, compressibility factor Z(P, T),
 * usable gas inventory (50–230 bar), replenishment mass, and Time-To-Dryout (TTD).
 *
 * Reference constants:
 * - R_specific (Methane) = 518.3 J/(kg·K)
 * - T_ref = 298.15 K (25°C)
 * - P_max = 230 bar (230e5 Pa), Z_230 ≈ 0.83
 * - P_dryout = 50 bar (50e5 Pa), Z_50 ≈ 0.93
 */
object CngTelemetryCalculator {

    const val R_SPECIFIC_METHANE: Double = 518.3 // J / (kg * K)
    const val DEFAULT_TEMPERATURE_KELVIN: Double = 298.15 // 25°C
    const val P_MAX_BAR: Double = 230.0
    const val P_DRYOUT_BAR: Double = 50.0
    const val Z_AT_50_BAR: Double = 0.93
    const val Z_AT_230_BAR: Double = 0.83

    // Joule-Thomson coefficient for CNG expansion (approx 0.35 K / bar)
    const val JOULE_THOMSON_COEFF: Double = 0.35

    /**
     * Computes the compressibility factor Z as a function of pressure (bar) and temperature (K).
     * Linear interpolation between 50 bar (0.93) and 230 bar (0.83) with temperature scaling.
     */
    fun calculateCompressibilityZ(
        pressureBar: Double,
        temperatureKelvin: Double = DEFAULT_TEMPERATURE_KELVIN
    ): Double {
        val clampedP = max(0.0, min(300.0, pressureBar))
        val baseZ = if (clampedP <= P_DRYOUT_BAR) {
            // Near atmospheric to 50 bar: transitions smoothly toward 1.0 at 1 bar
            1.0 - (1.0 - Z_AT_50_BAR) * (clampedP / P_DRYOUT_BAR)
        } else {
            // 50 to 230 bar interpolation
            val fraction = (clampedP - P_DRYOUT_BAR) / (P_MAX_BAR - P_DRYOUT_BAR)
            Z_AT_50_BAR + fraction * (Z_AT_230_BAR - Z_AT_50_BAR)
        }
        val tempCorrection = (DEFAULT_TEMPERATURE_KELVIN / max(200.0, temperatureKelvin)).pow(0.3)
        return max(0.70, min(1.05, baseZ * tempCorrection))
    }

    /**
     * Calculates total CNG mass in kilograms for a given geometric water volume (Liters)
     * at specified pressure (bar) and temperature (K).
     *
     * m = (P * V) / (Z * R_specific * T)
     * Pressure: bar -> Pa (* 1e5)
     * Volume: Liters -> m^3 (* 1e-3)
     */
    fun calculateTotalGasMassKg(
        pressureBar: Double,
        geometricVolumeLiters: Double,
        temperatureKelvin: Double = DEFAULT_TEMPERATURE_KELVIN
    ): Double {
        if (pressureBar <= 0.0 || geometricVolumeLiters <= 0.0) return 0.0
        val pressurePa = pressureBar * 100_000.0
        val volumeCubicMeters = geometricVolumeLiters * 0.001
        val z = calculateCompressibilityZ(pressureBar, temperatureKelvin)
        return (pressurePa * volumeCubicMeters) / (z * R_SPECIFIC_METHANE * temperatureKelvin)
    }

    /**
     * Usable Mass Remaining (kg):
     * Gas available strictly between current pressure and the 50 bar dryout cutoff.
     * If current pressure is <= 50 bar, usable mass is 0 kg.
     */
    fun calculateUsableMassKg(
        currentPressureBar: Double,
        geometricVolumeLiters: Double,
        temperatureKelvin: Double = DEFAULT_TEMPERATURE_KELVIN
    ): Double {
        if (currentPressureBar <= P_DRYOUT_BAR) return 0.0
        val totalCurrentMass = calculateTotalGasMassKg(currentPressureBar, geometricVolumeLiters, temperatureKelvin)
        val dryoutResidualMass = calculateTotalGasMassKg(P_DRYOUT_BAR, geometricVolumeLiters, temperatureKelvin)
        return max(0.0, totalCurrentMass - dryoutResidualMass)
    }

    /**
     * Top-Up Replenishment Demand (D_topup in kg):
     * The gas mass required to refill the station cascade back to 230 bar.
     */
    fun calculateTopUpDemandKg(
        currentPressureBar: Double,
        geometricVolumeLiters: Double,
        temperatureKelvin: Double = DEFAULT_TEMPERATURE_KELVIN
    ): Double {
        if (currentPressureBar >= P_MAX_BAR) return 0.0
        val fullMassAt230 = calculateTotalGasMassKg(P_MAX_BAR, geometricVolumeLiters, temperatureKelvin)
        val currentMass = calculateTotalGasMassKg(max(0.0, currentPressureBar), geometricVolumeLiters, temperatureKelvin)
        return max(0.0, fullMassAt230 - currentMass)
    }

    /**
     * True Time-To-Dryout (TTD) in hours:
     * TTD = m_usable / m_dot_forecast
     */
    fun calculateTimeToDryoutHours(
        usableMassKg: Double,
        burnRateKgPerHour: Double
    ): Double {
        if (usableMassKg <= 0.0) return 0.0
        if (burnRateKgPerHour <= 0.01) return 99.0 // Infinite/quiescent cover
        val ttd = usableMassKg / burnRateKgPerHour
        return min(99.0, max(0.0, ttd))
    }

    /**
     * Corrects temperature for Joule-Thomson adiabatic cooling during high-speed decanting.
     * Delta T ≈ mu_JT * (P_source - P_dest)
     */
    fun calculateJouleThomsonDecantingTemperature(
        ambientTempKelvin: Double,
        pressureDropBar: Double
    ): Double {
        val deltaT = JOULE_THOMSON_COEFF * max(0.0, pressureDropBar)
        return max(260.0, ambientTempKelvin - deltaT)
    }

    /**
     * Evaluates whether a station's decanting bays are fully occupied (Bay Interlock).
     */
    fun isBayInterlocked(
        activeOccupiedBays: Int,
        totalStationBays: Int
    ): Boolean {
        return activeOccupiedBays >= totalStationBays
    }
}
