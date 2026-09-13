package com.fleetopt.app.core.engine

import com.fleetopt.app.core.forecasting.CalendarSurge
import com.fleetopt.app.core.forecasting.CngDemandForecastingEngine
import com.fleetopt.app.core.physics.CngTelemetryCalculator
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ScoringWeights(
    val demand: Double = 0.40,
    val distance: Double = 0.20,
    val traffic: Double = 0.15,
    val availability: Double = 0.15,
    val capacity: Double = 0.10
)

data class CostParameters(
    val perKm: Double = 62.0,
    val baseHandling: Double = 210.0,
    val cngPricePerKg: Double = 94.0,
    val deliveryChargePerKg: Double = 6.5,
    val trafficSurcharges: Map<String, Double> = mapOf(
        "low" to 0.0,
        "moderate" to 90.0,
        "heavy" to 190.0
    )
)

data class StationEvaluation(
    val stationId: String,
    val stationName: String,
    val currentPressureBar: Double,
    val usableMassKg: Double,
    val topUpDemandKg: Double,
    val baselineDemandKg: Double,
    val projectedDemandKg: Double,
    val demandPendingKg: Double,
    val distanceKm: Double,
    val traffic: String, // "low", "moderate", "heavy"
    val ttdHours: Double,
    val openBays: Int,
    val totalBays: Int,
    val isBayInterlocked: Boolean
)

data class HcvEvaluation(
    val registration: String,
    val driverName: String,
    val driverPhone: String,
    val capacityKg: Double = 650.0,
    val status: String, // "available", "loading", "on-trip", "decanting", "maintenance", "breakdown"
    val breakdownReason: String? = null,
    val isHydroTestValid: Boolean = true,
    val isPrvCertified: Boolean = true,
    val tireConditionPercent: Int = 90,
    val distanceToCgsKm: Double = 0.0,
    val locationDescription: String = "CGS Yard"
)

data class FactorScoreBreakdown(
    val demandScore: Double,      // 0.0 - 1.0
    val distanceScore: Double,    // 0.0 - 1.0
    val trafficScore: Double,     // 0.0 - 1.0
    val availabilityScore: Double,// 0.0 - 1.0
    val capacityScore: Double     // 0.0 - 1.0
)

data class DispatchRecommendation(
    val targetStation: StationEvaluation,
    val assignedHcv: HcvEvaluation,
    val recommendedPayloadKg: Double,
    val etaMinutes: Int,
    val tripCostInr: Int,
    val estimatedSavingsInr: Int,
    val totalScore: Double,
    val factorBreakdown: FactorScoreBreakdown,
    val rationale: String,
    val explicitFactors: List<String> = emptyList(),
    val isSplitMilkRun: Boolean = false,
    val secondaryStation: StationEvaluation? = null,
    val secondaryPayloadKg: Double = 0.0
)

object AiRecommendationEngine {

    val weights = ScoringWeights()
    val costParams = CostParameters()

    private val trafficSpeeds = mapOf(
        "low" to 46.0,      // km/h
        "moderate" to 36.0,
        "heavy" to 26.0
    )

    private val trafficWeights = mapOf(
        "low" to 1.0,
        "moderate" to 0.60,
        "heavy" to 0.25
    )

    /**
     * Estimates transit time in minutes given corridor distance and traffic conditions.
     */
    fun calculateEtaMinutes(distanceKm: Double, traffic: String, deadheadKm: Double = 0.0): Int {
        val speed = trafficSpeeds[traffic.lowercase()] ?: 35.0
        val totalDistance = distanceKm + (deadheadKm * 0.35)
        val hours = totalDistance / speed
        return max(15, (hours * 60.0).roundToInt())
    }

    /**
     * Calculates transit cost (INR) based on distance, deadhead, base handling, and traffic surcharge.
     */
    fun calculateTripCost(distanceKm: Double, traffic: String, deadheadKm: Double = 0.0): Int {
        val totalKm = distanceKm + (deadheadKm * 0.35)
        val surcharge = costParams.trafficSurcharges[traffic.lowercase()] ?: 90.0
        val rawCost = (totalKm * costParams.perKm) + costParams.baseHandling + surcharge
        return (rawCost / 10.0).roundToInt() * 10
    }

    /**
     * Calculates estimated cost savings (INR) vs manual planning.
     * Combines route fuel optimization with stockout avoidance penalty protection.
     */
    fun calculateSavings(payloadKg: Double, distanceKm: Double, traffic: String): Int {
        val stockoutAvoidanceSavings = payloadKg * 2.8 // Value of prevented compressor dryout
        val routeEfficiencySavings = when (traffic.lowercase()) {
            "low" -> 950.0
            "moderate" -> 650.0
            else -> 420.0
        }
        return (stockoutAvoidanceSavings + routeEfficiencySavings).roundToInt()
    }

    /**
     * Evaluates and scores every available HCV against every daughter station to produce
     * the optimal dispatch recommendation.
     *
     * Automatically filters out vehicles in MAINTENANCE or BREAKDOWN or with expired safety tests.
     */
    fun generateRecommendation(
        stations: List<StationEvaluation>,
        fleet: List<HcvEvaluation>,
        currentHour: Int = 10,
        surge: CalendarSurge = CalendarSurge.WEEKDAY
    ): DispatchRecommendation? {
        // Filter fleet: ONLY healthy, available vehicles at CGS
        val eligibleFleet = fleet.filter { hcv ->
            hcv.status.equals("available", ignoreCase = true) &&
                    hcv.isHydroTestValid &&
                    hcv.isPrvCertified &&
                    hcv.tireConditionPercent >= 50
        }

        if (eligibleFleet.isEmpty() || stations.isEmpty()) {
            return null
        }

        val maxDailySale = stations.maxOfOrNull { it.baselineDemandKg }?.coerceAtLeast(1.0) ?: 3000.0
        val maxDistance = stations.maxOfOrNull { it.distanceKm }?.coerceAtLeast(1.0) ?: 100.0
        val maxHcvCapacity = eligibleFleet.maxOfOrNull { it.capacityKg }?.coerceAtLeast(1.0) ?: 650.0

        var bestScore = -1.0
        var bestRecommendation: DispatchRecommendation? = null

        for (station in stations) {
            // Check bay interlock: If no bays open, heavily penalize or skip
            val bayPenalty = if (station.isBayInterlocked) 0.50 else 0.0

            for (hcv in eligibleFleet) {
                // 1. Demand Urgency factor (based on pending demand and low TTD)
                val pendingRatio = min(1.0, station.demandPendingKg / max(1.0, station.projectedDemandKg))
                val ttdScore = if (station.ttdHours <= 1.5) 1.0 else if (station.ttdHours <= 3.0) 0.75 else 0.40
                val demandScore = (pendingRatio * 0.6) + (ttdScore * 0.4)

                // 2. Distance score (shorter distance = higher score)
                val distanceScore = max(0.0, 1.0 - (station.distanceKm / maxDistance))

                // 3. Traffic score
                val trafficScore = trafficWeights[station.traffic.lowercase()] ?: 0.5

                // 4. Availability & Readiness score
                val availabilityScore = if (hcv.distanceToCgsKm <= 2.0) 1.0 else 0.8

                // 5. Capacity matching score
                val capacityScore = min(1.0, station.topUpDemandKg / maxHcvCapacity)

                // Composite weighted score
                val rawScore = (demandScore * weights.demand) +
                        (distanceScore * weights.distance) +
                        (trafficScore * weights.traffic) +
                        (availabilityScore * weights.availability) +
                        (capacityScore * weights.capacity) - bayPenalty

                val finalScore = max(0.0, min(1.0, rawScore))

                if (finalScore > bestScore) {
                    bestScore = finalScore

                    val safeTopUp = max(50.0, station.topUpDemandKg)
                    val payload = min(hcv.capacityKg, safeTopUp)
                    val eta = calculateEtaMinutes(station.distanceKm, station.traffic, hcv.distanceToCgsKm)
                    val cost = calculateTripCost(station.distanceKm, station.traffic, hcv.distanceToCgsKm)
                    val savings = calculateSavings(payload, station.distanceKm, station.traffic)

                    // Check for potential Milk-Run chaining if payload is low
                    var isMilkRun = false
                    var secondaryStation: StationEvaluation? = null
                    var secondaryPayload = 0.0

                    if (station.topUpDemandKg < 400.0) {
                        // Find neighboring station on same corridor with open bays
                        val candidate = stations.firstOrNull { other ->
                            other.stationId != station.stationId &&
                                    !other.isBayInterlocked &&
                                    other.topUpDemandKg > 200.0
                        }
                        if (candidate != null) {
                            isMilkRun = true
                            secondaryStation = candidate
                            secondaryPayload = min(hcv.capacityKg - payload, candidate.topUpDemandKg)
                        }
                    }

                    val rationale = buildString {
                        append("${station.stationName} has ~${"%.1f".format(station.ttdHours)}h of cover left ")
                        append("with ${station.demandPendingKg.roundToInt()} kg unserved demand today. ")
                        append("Dispatched via ${station.traffic.replaceFirstChar { it.uppercase() }} traffic corridor. ")
                        if (station.isBayInterlocked) {
                            append("⚠️ Note: Bay interlock active, tanker will wait in holding area.")
                        } else {
                            append("Bay ${station.totalBays - station.openBays + 1} ready for decanting.")
                        }
                        if (isMilkRun && secondaryStation != null) {
                            append(" [Split Milk-Run]: Continuing with remaining ${secondaryPayload.roundToInt()} kg to ${secondaryStation.stationName}.")
                        }
                    }

                    val factorList = listOf(
                        "Urgency: ${station.stationName} has ~${"%.1f".format(station.ttdHours)}h cover left (${station.currentPressureBar.roundToInt()} bar)",
                        "Vehicle: Tanker ${hcv.registration} available at ${hcv.locationDescription} with ${hcv.capacityKg.roundToInt()} kg capacity",
                        "Safety: Hydro-test valid, PRV certified, ${hcv.tireConditionPercent}% tire health",
                        "Corridor: ${station.distanceKm.roundToInt()} km via ${station.traffic} traffic (estimated ETA ~${eta} min)",
                        "Decanting: ${if (station.isBayInterlocked) "Bay interlock active (Holding standby)" else "Bay ${station.totalBays - station.openBays + 1} open & ready"}"
                    )

                    bestRecommendation = DispatchRecommendation(
                        targetStation = station,
                        assignedHcv = hcv,
                        recommendedPayloadKg = payload,
                        etaMinutes = eta,
                        tripCostInr = cost,
                        estimatedSavingsInr = savings,
                        totalScore = finalScore,
                        factorBreakdown = FactorScoreBreakdown(
                            demandScore = demandScore,
                            distanceScore = distanceScore,
                            trafficScore = trafficScore,
                            availabilityScore = availabilityScore,
                            capacityScore = capacityScore
                        ),
                        rationale = rationale,
                        explicitFactors = factorList,
                        isSplitMilkRun = isMilkRun,
                        secondaryStation = secondaryStation,
                        secondaryPayloadKg = secondaryPayload
                    )
                }
            }
        }

        return bestRecommendation
    }
}
