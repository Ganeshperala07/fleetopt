package com.fleetopt.app.core.engine

import org.junit.Assert.*
import org.junit.Test

class AiRecommendationEngineTest {

    @Test
    fun testFleetMaintenanceAndBreakdownInterlock() {
        val stations = listOf(
            StationEvaluation(
                stationId = "tejas",
                stationName = "Tejas FS",
                currentPressureBar = 55.0, // Critical
                usableMassKg = 30.0,
                topUpDemandKg = 550.0,
                baselineDemandKg = 3000.0,
                projectedDemandKg = 3000.0,
                demandPendingKg = 2800.0,
                distanceKm = 20.0,
                traffic = "low",
                ttdHours = 0.5,
                openBays = 1,
                totalBays = 2,
                isBayInterlocked = false
            )
        )

        // Fleet with 1 broken down, 1 maintenance, 1 healthy available
        val fleet = listOf(
            HcvEvaluation(
                registration = "TS07UG8062",
                driverName = "Anil Yadav",
                driverPhone = "+91 98490 55678",
                capacityKg = 650.0,
                status = "maintenance",
                breakdownReason = "Scheduled Service",
                isHydroTestValid = true,
                isPrvCertified = true
            ),
            HcvEvaluation(
                registration = "GJ18BT7517",
                driverName = "Naresh Gupta",
                driverPhone = "+91 98490 99012",
                capacityKg = 650.0,
                status = "breakdown",
                breakdownReason = "Puncture on NH44",
                isHydroTestValid = true,
                isPrvCertified = true
            ),
            HcvEvaluation(
                registration = "TS07UG5579",
                driverName = "Ravi Kumar",
                driverPhone = "+91 98490 11234",
                capacityKg = 650.0,
                status = "available",
                isHydroTestValid = true,
                isPrvCertified = true
            )
        )

        val rec = AiRecommendationEngine.generateRecommendation(stations, fleet)
        assertNotNull("Should generate recommendation with eligible vehicle", rec)
        assertEquals("Should assign healthy available tanker TS07UG5579", "TS07UG5579", rec?.assignedHcv?.registration)
        assertEquals("Target station should be Tejas FS", "tejas", rec?.targetStation?.stationId)
    }

    @Test
    fun testSafetyTestExpirationInterlock() {
        val stations = listOf(
            StationEvaluation(
                stationId = "medak",
                stationName = "Medak FS",
                currentPressureBar = 60.0,
                usableMassKg = 50.0,
                topUpDemandKg = 600.0,
                baselineDemandKg = 2000.0,
                projectedDemandKg = 2000.0,
                demandPendingKg = 1800.0,
                distanceKm = 75.0,
                traffic = "moderate",
                ttdHours = 1.0,
                openBays = 1,
                totalBays = 1,
                isBayInterlocked = false
            )
        )

        // Only 1 vehicle available, but its PRV inspection is overdue (isPrvCertified = false)
        val fleet = listOf(
            HcvEvaluation(
                registration = "TS07UG8065",
                driverName = "Vinod Sharma",
                driverPhone = "+91 98490 77890",
                capacityKg = 650.0,
                status = "available",
                isHydroTestValid = true,
                isPrvCertified = false // SAFETY VIOLATION
            )
        )

        val rec = AiRecommendationEngine.generateRecommendation(stations, fleet)
        assertNull("Vehicle with overdue PRV certification must be excluded from dispatch", rec)
    }

    @Test
    fun testCostAndSavingsCalculation() {
        val cost = AiRecommendationEngine.calculateTripCost(distanceKm = 20.0, traffic = "low")
        // 20 km * 62 = 1240 + 210 base = 1450
        assertEquals(1450, cost)

        val savings = AiRecommendationEngine.calculateSavings(payloadKg = 650.0, distanceKm = 20.0, traffic = "low")
        // 650 * 2.8 = 1820 + 950 = 2770
        assertEquals(2770, savings)
    }

    @Test
    fun testExplicitFactorsAndPayloadClamping() {
        val stations = listOf(
            StationEvaluation(
                stationId = "medchal",
                stationName = "Medchal FS",
                currentPressureBar = 75.0,
                usableMassKg = 120.0,
                topUpDemandKg = 380.0, // Station only needs 380 kg to reach 230 bar
                baselineDemandKg = 2500.0,
                projectedDemandKg = 2500.0,
                demandPendingKg = 2100.0,
                distanceKm = 30.0,
                traffic = "moderate",
                ttdHours = 2.0,
                openBays = 2,
                totalBays = 2,
                isBayInterlocked = false
            )
        )

        val fleet = listOf(
            HcvEvaluation(
                registration = "TS07UG8064",
                driverName = "Suresh Reddy",
                driverPhone = "+91 98490 33456",
                capacityKg = 650.0,
                status = "available",
                isHydroTestValid = true,
                isPrvCertified = true
            )
        )

        val rec = AiRecommendationEngine.generateRecommendation(stations, fleet)
        assertNotNull("Should generate recommendation", rec)

        // Payload should be clamped to topUpDemandKg (380 kg) rather than overpressurizing with 650 kg
        assertEquals("Payload must be clamped to avoid cascade overpressure", 380.0, rec?.recommendedPayloadKg ?: 0.0, 0.01)

        // Explicit factors must contain 5 structured rationales
        assertEquals("Must provide 5 explicit decision factors", 5, rec?.explicitFactors?.size ?: 0)
        assertTrue("Explicit factors should mention urgency", rec?.explicitFactors?.any { it.contains("Urgency") } == true)
        assertTrue("Explicit factors should mention vehicle", rec?.explicitFactors?.any { it.contains("Vehicle") } == true)
        assertTrue("Explicit factors should mention safety", rec?.explicitFactors?.any { it.contains("Safety") } == true)
        assertTrue("Explicit factors should mention corridor", rec?.explicitFactors?.any { it.contains("Corridor") } == true)
        assertTrue("Explicit factors should mention decanting", rec?.explicitFactors?.any { it.contains("Decanting") } == true)
    }
}
