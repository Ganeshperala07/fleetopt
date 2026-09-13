package com.fleetopt.app.core.simulation

import com.fleetopt.app.core.forecasting.CalendarSurge
import com.fleetopt.app.core.forecasting.CngDemandForecastingEngine
import com.fleetopt.app.core.notification.StockoutNotificationManager
import com.fleetopt.app.core.physics.CngTelemetryCalculator
import com.fleetopt.app.data.repository.FleetOptRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlin.math.max
import kotlin.math.min

enum class SimulationScenario(val displayName: String, val description: String) {
    STANDARD("Standard Day", "Nominal demand, steady diurnal sales curve"),
    MORNING_RUSH("Morning Commute Rush", "Peak 07:00-10:00 morning rush, rapid cascade draw"),
    CORRIDOR_JAM("Corridor Bottleneck", "Heavy arterial congestion, extended tanker ETAs"),
    FESTIVAL_SURGE("Holiday Surge (+50%)", "Festival travel spike across all highway corridors")
}

class TelemetrySimulationEngine(
    private val repository: FleetOptRepository,
    private val notificationManager: StockoutNotificationManager,
    private val scope: CoroutineScope
) {
    private val _isSimulationActive = MutableStateFlow(false)
    val isSimulationActive: StateFlow<Boolean> = _isSimulationActive.asStateFlow()

    private val _simulatedPressures = MutableStateFlow<Map<String, Double>>(emptyMap())
    val simulatedPressures: StateFlow<Map<String, Double>> = _simulatedPressures.asStateFlow()

    var currentScenario: SimulationScenario = SimulationScenario.STANDARD
    var currentSurge: CalendarSurge = CalendarSurge.WEEKDAY

    private var simulationJob: Job? = null
    private val alertedStations = mutableSetOf<String>()

    fun startSimulation() {
        if (_isSimulationActive.value && simulationJob != null) return
        _isSimulationActive.value = true

        simulationJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isSimulationActive.value) {
                delay(4000) // Tick every 4 seconds
                try {
                    tickStationPressures()
                } catch (_: Exception) {
                    // Prevent simulation crash
                }
            }
        }
    }

    fun stopSimulation() {
        _isSimulationActive.value = false
        simulationJob?.cancel()
        simulationJob = null
    }

    fun resetSimulation() {
        stopSimulation()
        _simulatedPressures.value = emptyMap()
        alertedStations.clear()
        currentScenario = SimulationScenario.STANDARD
        currentSurge = CalendarSurge.WEEKDAY
    }

    fun applyScenario(scenario: SimulationScenario) {
        currentScenario = scenario
        currentSurge = when (scenario) {
            SimulationScenario.FESTIVAL_SURGE -> CalendarSurge.FESTIVAL
            SimulationScenario.MORNING_RUSH -> CalendarSurge.WEEKDAY
            SimulationScenario.CORRIDOR_JAM -> CalendarSurge.WEEKDAY
            SimulationScenario.STANDARD -> CalendarSurge.WEEKDAY
        }
        if (!_isSimulationActive.value) {
            startSimulation()
        }
    }

    private suspend fun tickStationPressures() {
        val stations = repository.stationsFlow.first()
        val currentSimMap = _simulatedPressures.value.toMutableMap()

        for (station in stations) {
            val basePressure = currentSimMap[station.id] ?: station.currentPressureBar

            val hour = when (currentScenario) {
                SimulationScenario.MORNING_RUSH -> 8
                else -> 11
            }

            val burnRate = CngDemandForecastingEngine.computeBlendedForecastBurnRate(
                telemetryBurnRateKgPerHour = 0.0,
                isTelemetryActive = false,
                baselineDailyDemandKg = station.baselineDailyDemandKg,
                hourOfDay = hour,
                surge = currentSurge
            )

            val kgPerTick = (burnRate / 3600.0) * 4.0
            val barDrop = (kgPerTick / 15.0).coerceIn(0.04, 0.25)
            val newPressure = max(CngTelemetryCalculator.P_DRYOUT_BAR, basePressure - barDrop)

            currentSimMap[station.id] = (newPressure * 10.0).toInt() / 10.0

            // Check for critical stockout notification threshold
            val usableMass = CngTelemetryCalculator.calculateUsableMassKg(newPressure, station.geometricVolumeLiters)
            val ttd = CngTelemetryCalculator.calculateTimeToDryoutHours(usableMass, burnRate)

            if (newPressure <= 60.0 && !alertedStations.contains(station.id)) {
                alertedStations.add(station.id)
                notificationManager.postStockoutAlert(station.id, station.name, newPressure, ttd)
            } else if (newPressure > 65.0) {
                alertedStations.remove(station.id)
            }
        }

        _simulatedPressures.value = currentSimMap
    }
}
