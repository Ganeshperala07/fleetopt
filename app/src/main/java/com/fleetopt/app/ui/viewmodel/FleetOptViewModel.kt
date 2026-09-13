package com.fleetopt.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fleetopt.app.core.engine.AiRecommendationEngine
import com.fleetopt.app.core.engine.DispatchRecommendation
import com.fleetopt.app.core.engine.HcvEvaluation
import com.fleetopt.app.core.engine.StationEvaluation
import com.fleetopt.app.core.forecasting.CalendarSurge
import com.fleetopt.app.core.forecasting.CngDemandForecastingEngine
import com.fleetopt.app.core.physics.CngTelemetryCalculator
import com.fleetopt.app.core.security.AdminAuthManager
import com.fleetopt.app.core.simulation.SimulationScenario
import com.fleetopt.app.core.simulation.TelemetrySimulationEngine
import com.fleetopt.app.data.local.entity.DispatchTripEntity
import com.fleetopt.app.data.local.entity.HcvEntity
import com.fleetopt.app.data.local.entity.StationEntity
import com.fleetopt.app.data.repository.FleetOptRepository
import com.fleetopt.app.ui.theme.ThemeMode
import com.fleetopt.app.ui.theme.ThemePreferenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class KpiState(
    val totalHcvs: Int = 9,
    val availableHcvs: Int = 0,
    val activeTrips: Int = 0,
    val fleetUtilizationPercent: Int = 0,
    val averageEtaMinutes: Int = 0,
    val dispatchesToday: Int = 0,
    val estDailyCostSavingsInr: Int = 0
)

data class FleetOptUiState(
    val stations: List<StationEvaluation> = emptyList(),
    val rawStations: List<StationEntity> = emptyList(),
    val motherStations: List<StationEntity> = emptyList(),
    val fleet: List<HcvEntity> = emptyList(),
    val trips: List<DispatchTripEntity> = emptyList(),
    val recommendation: DispatchRecommendation? = null,
    val kpi: KpiState = KpiState(),
    val currentSurge: CalendarSurge = CalendarSurge.WEEKDAY,
    val currentScenario: SimulationScenario = SimulationScenario.STANDARD,
    val isEvaluating: Boolean = false,
    val lastDispatchedTripId: String? = null,
    val isAdminMode: Boolean = false,
    val isSimulationActive: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK
)

private data class ControlsState(
    val surge: CalendarSurge,
    val scenario: SimulationScenario,
    val rec: DispatchRecommendation?,
    val isAdmin: Boolean,
    val isSimActive: Boolean,
    val theme: ThemeMode
)

class FleetOptViewModel(
    private val repository: FleetOptRepository,
    private val simulationEngine: TelemetrySimulationEngine,
    private val adminAuthManager: AdminAuthManager,
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {

    private val _currentSurge = MutableStateFlow(CalendarSurge.WEEKDAY)
    private val _currentScenario = MutableStateFlow(SimulationScenario.STANDARD)
    private val _activeRecommendation = MutableStateFlow<DispatchRecommendation?>(null)
    private val _isEvaluating = MutableStateFlow(false)
    private val _lastDispatchedTripId = MutableStateFlow<String?>(null)

    private val _appEnvironment = combine(
        adminAuthManager.isAdminActive,
        simulationEngine.isSimulationActive,
        themePreferenceManager.themeMode
    ) { isAdmin, isSim, theme ->
        Triple(isAdmin, isSim, theme)
    }

    private val _controls = combine(
        _currentSurge,
        _currentScenario,
        _activeRecommendation,
        _appEnvironment
    ) { surge, scenario, rec, env ->
        val (isAdmin, isSim, theme) = env
        ControlsState(surge, scenario, rec, isAdmin, isSim, theme)
    }

    val uiState: StateFlow<FleetOptUiState> = combine(
        repository.stationsFlow,
        repository.hcvListFlow,
        repository.tripsFlow,
        simulationEngine.simulatedPressures,
        _controls
    ) { stations, fleet, trips, simPressures, controls ->
        val surge = controls.surge
        val scenario = controls.scenario
        val recommendation = controls.rec
        val isAdmin = controls.isAdmin
        val isSimActive = controls.isSimActive
        val theme = controls.theme

        val motherStations = stations.filter { it.stationType.equals("MOTHER", ignoreCase = true) }
        val daughterStations = stations.filter { !it.stationType.equals("MOTHER", ignoreCase = true) }

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Evaluate physical inventory and demand for daughter booster stations
        val evaluatedStations = daughterStations.map { station ->
            val effectivePressure = if (isSimActive && simPressures.containsKey(station.id)) {
                simPressures[station.id] ?: station.currentPressureBar
            } else {
                station.currentPressureBar
            }

            val usableMass = CngTelemetryCalculator.calculateUsableMassKg(
                currentPressureBar = effectivePressure,
                geometricVolumeLiters = station.geometricVolumeLiters,
                temperatureKelvin = station.temperatureKelvin
            )
            val topUpDemand = CngTelemetryCalculator.calculateTopUpDemandKg(
                currentPressureBar = effectivePressure,
                geometricVolumeLiters = station.geometricVolumeLiters,
                temperatureKelvin = station.temperatureKelvin
            )
            val projectedDemand = CngDemandForecastingEngine.computeProjectedDailyDemand(
                baselineDailyDemandKg = station.baselineDailyDemandKg,
                surge = surge
            )
            val pendingDemand = (projectedDemand - station.demandDeliveredTodayKg).coerceAtLeast(0.0)

            val burnRate = CngDemandForecastingEngine.computeBlendedForecastBurnRate(
                telemetryBurnRateKgPerHour = 0.0,
                isTelemetryActive = false,
                baselineDailyDemandKg = station.baselineDailyDemandKg,
                hourOfDay = currentHour,
                surge = surge
            )

            val ttdHours = CngTelemetryCalculator.calculateTimeToDryoutHours(usableMass, burnRate)
            val isInterlocked = CngTelemetryCalculator.isBayInterlocked(station.occupiedBays, station.totalBays)

            StationEvaluation(
                stationId = station.id,
                stationName = station.name,
                currentPressureBar = effectivePressure,
                usableMassKg = usableMass,
                topUpDemandKg = topUpDemand,
                baselineDemandKg = station.baselineDailyDemandKg,
                projectedDemandKg = projectedDemand,
                demandPendingKg = pendingDemand,
                distanceKm = station.distanceKm,
                traffic = station.traffic,
                ttdHours = ttdHours,
                openBays = (station.totalBays - station.occupiedBays).coerceAtLeast(0),
                totalBays = station.totalBays,
                isBayInterlocked = isInterlocked
            )
        }.sortedBy { it.ttdHours } // Ranked by urgency (lowest TTD first)

        // Calculate live KPIs
        val availableCount = fleet.count { it.status.equals("available", ignoreCase = true) }
        val activeTripsCount = trips.count { it.status.equals("EN_ROUTE", ignoreCase = true) }
        val engagedCount = fleet.count { !it.status.equals("AVAILABLE", ignoreCase = true) && !it.status.equals("MAINTENANCE", ignoreCase = true) }
        val utilizationPercent = if (fleet.isNotEmpty()) (engagedCount * 100) / fleet.size else 0

        val avgEta = if (evaluatedStations.isNotEmpty()) {
            evaluatedStations.map { AiRecommendationEngine.calculateEtaMinutes(it.distanceKm, it.traffic) }.average().toInt()
        } else 35

        val dispatchesCount = trips.size
        val totalSavings = trips.sumOf { it.estimatedSavingsInr }

        FleetOptUiState(
            stations = evaluatedStations,
            rawStations = stations,
            motherStations = motherStations,
            fleet = fleet,
            trips = trips,
            recommendation = recommendation,
            kpi = KpiState(
                totalHcvs = fleet.size,
                availableHcvs = availableCount,
                activeTrips = activeTripsCount,
                fleetUtilizationPercent = utilizationPercent,
                averageEtaMinutes = avgEta,
                dispatchesToday = dispatchesCount,
                estDailyCostSavingsInr = totalSavings
            ),
            currentSurge = surge,
            currentScenario = scenario,
            isEvaluating = false,
            lastDispatchedTripId = _lastDispatchedTripId.value,
            isAdminMode = isAdmin,
            isSimulationActive = isSimActive,
            themeMode = theme
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FleetOptUiState()
    )

    init {
        // Automatically generate initial recommendation on load
        viewModelScope.launch {
            generateRecommendation()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        themePreferenceManager.setThemeMode(mode)
    }

    fun toggleSimulation(active: Boolean) {
        if (active) {
            simulationEngine.startSimulation()
        } else {
            simulationEngine.stopSimulation()
        }
        generateRecommendation()
    }

    fun resetSimulation() {
        simulationEngine.resetSimulation()
        _currentScenario.value = SimulationScenario.STANDARD
        _currentSurge.value = CalendarSurge.WEEKDAY
        generateRecommendation()
    }

    fun setCalendarSurge(surge: CalendarSurge) {
        _currentSurge.value = surge
        simulationEngine.currentSurge = surge
        generateRecommendation()
    }

    fun setScenario(scenario: SimulationScenario) {
        _currentScenario.value = scenario
        simulationEngine.applyScenario(scenario)
        generateRecommendation()
    }

    fun generateRecommendation() {
        viewModelScope.launch {
            val state = uiState.value
            val hcvEvaluations = state.fleet.map { hcv ->
                HcvEvaluation(
                    registration = hcv.registration,
                    driverName = hcv.driverName,
                    driverPhone = hcv.driverPhone,
                    capacityKg = hcv.capacityKg,
                    status = hcv.status.lowercase(),
                    breakdownReason = hcv.breakdownReason,
                    isHydroTestValid = hcv.isHydroTestValid,
                    isPrvCertified = hcv.isPrvCertified,
                    tireConditionPercent = hcv.tireConditionPercent,
                    distanceToCgsKm = hcv.distanceToCgsKm,
                    locationDescription = hcv.currentLocation
                )
            }

            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val rec = AiRecommendationEngine.generateRecommendation(
                stations = state.stations,
                fleet = hcvEvaluations,
                currentHour = currentHour,
                surge = _currentSurge.value
            )
            _activeRecommendation.value = rec
        }
    }

    fun acceptDispatch(onDispatched: (tripId: String) -> Unit = {}) {
        viewModelScope.launch {
            val rec = _activeRecommendation.value ?: return@launch
            val tripId = repository.executeDispatch(rec)
            _lastDispatchedTripId.value = tripId
            _activeRecommendation.value = null
            generateRecommendation() // Re-evaluate next best action
            onDispatched(tripId)
        }
    }

    fun completeTrip(tripId: String) {
        viewModelScope.launch {
            repository.completeTrip(tripId)
            adminAuthManager.recordAudit("COMPLETE_TRIP", "Trip $tripId marked completed")
            generateRecommendation()
        }
    }

    fun cancelTrip(tripId: String) {
        viewModelScope.launch {
            repository.cancelTrip(tripId)
            adminAuthManager.recordAudit("CANCEL_TRIP", "Trip $tripId cancelled")
            generateRecommendation()
        }
    }

    fun updateStationPressure(stationId: String, newPressureBar: Double) {
        viewModelScope.launch {
            repository.updateStationPressure(stationId, newPressureBar)
            adminAuthManager.recordAudit("UPDATE_PRESSURE", "Station $stationId set to $newPressureBar bar")
            generateRecommendation()
        }
    }

    fun reportHcvBreakdown(registration: String, reason: String) {
        viewModelScope.launch {
            repository.reportBreakdown(registration, reason)
            adminAuthManager.recordAudit("REPORT_BREAKDOWN", "Tanker $registration breakdown: $reason")
            generateRecommendation()
        }
    }

    fun clearHcvMaintenance(registration: String) {
        viewModelScope.launch {
            repository.clearMaintenance(registration)
            adminAuthManager.recordAudit("CLEAR_MAINTENANCE", "Tanker $registration cleared for service")
            generateRecommendation()
        }
    }

    fun addStation(
        name: String,
        code: String,
        lat: Double,
        lng: Double,
        volumeLiters: Double,
        bays: Int,
        baselineDailySalesKg: Double,
        distanceKm: Double,
        traffic: String
    ) {
        viewModelScope.launch {
            val newStation = StationEntity(
                id = code.lowercase().trim(),
                name = name.trim(),
                lat = lat,
                lng = lng,
                geometricVolumeLiters = volumeLiters,
                currentPressureBar = 150.0,
                totalBays = bays,
                occupiedBays = 0,
                baselineDailyDemandKg = baselineDailySalesKg,
                distanceKm = distanceKm,
                traffic = traffic.lowercase(),
                demandDeliveredTodayKg = 0.0
            )
            repository.addStation(newStation)
            adminAuthManager.recordAudit("ADD_STATION", "Added station $name ($code)")
            generateRecommendation()
        }
    }

    fun addHcv(
        registration: String,
        driverName: String,
        driverPhone: String,
        capacityKg: Double = 650.0,
        status: String = "AVAILABLE",
        isHydroTestValid: Boolean = true,
        hydroTestExpiryDate: String = "2027-12-31",
        isPrvCertified: Boolean = true,
        tireConditionPercent: Int = 95,
        heelPressureBar: Double = 25.0
    ) {
        viewModelScope.launch {
            val newHcv = HcvEntity(
                registration = registration.trim().uppercase(),
                driverName = driverName.trim(),
                driverPhone = driverPhone.trim(),
                capacityKg = capacityKg,
                status = status.uppercase(),
                breakdownReason = null,
                isHydroTestValid = isHydroTestValid,
                hydroTestExpiryDate = hydroTestExpiryDate,
                isPrvCertified = isPrvCertified,
                tireConditionPercent = tireConditionPercent,
                heelPressureBar = heelPressureBar,
                currentLocation = "CGS Yard",
                lat = 17.4526,
                lng = 78.3312,
                assignedStationId = null,
                distanceToCgsKm = 0.0,
                tripsToday = 0
            )
            repository.addHcv(newHcv)
            adminAuthManager.recordAudit("ADD_HCV", "Added tanker $registration")
            generateRecommendation()
        }
    }

    fun authenticateAdmin(pin: String): Boolean {
        return adminAuthManager.authenticate(pin)
    }

    fun lockAdmin() {
        adminAuthManager.lock()
    }

    fun changeAdminPin(oldPin: String, newPin: String): Boolean {
        return adminAuthManager.changePin(oldPin, newPin)
    }

    fun updateHcv(hcv: HcvEntity) {
        viewModelScope.launch {
            repository.updateHcv(hcv)
            adminAuthManager.recordAudit("UPDATE_HCV", "Updated tanker ${hcv.registration}")
            generateRecommendation()
        }
    }

    fun deleteHcv(registration: String) {
        viewModelScope.launch {
            repository.deleteHcv(registration)
            adminAuthManager.recordAudit("DELETE_HCV", "Deleted tanker $registration")
            generateRecommendation()
        }
    }

    fun updateStation(station: StationEntity) {
        viewModelScope.launch {
            repository.updateStation(station)
            adminAuthManager.recordAudit("UPDATE_STATION", "Updated station ${station.name}")
            generateRecommendation()
        }
    }

    fun deleteStation(stationId: String) {
        viewModelScope.launch {
            repository.deleteStation(stationId)
            adminAuthManager.recordAudit("DELETE_STATION", "Deleted station $stationId")
            generateRecommendation()
        }
    }

    fun addMotherStation(
        name: String,
        code: String,
        lat: Double,
        lng: Double,
        volumeLiters: Double = 10000.0,
        bays: Int = 4,
        compressorCapacityScmh: Double = 1500.0
    ) {
        viewModelScope.launch {
            val newMother = StationEntity(
                id = code.lowercase().trim(),
                name = name.trim(),
                lat = lat,
                lng = lng,
                geometricVolumeLiters = volumeLiters,
                currentPressureBar = 250.0,
                totalBays = bays,
                occupiedBays = 0,
                baselineDailyDemandKg = 0.0,
                distanceKm = 0.0,
                traffic = "low",
                demandDeliveredTodayKg = 0.0,
                stationType = "MOTHER",
                motherStationId = null,
                motherStationName = null,
                compressorCapacityScmh = compressorCapacityScmh
            )
            repository.addStation(newMother)
            adminAuthManager.recordAudit("ADD_MOTHER_STATION", "Added CGS Hub $name ($code)")
            generateRecommendation()
        }
    }
}

class FleetOptViewModelFactory(
    private val repository: FleetOptRepository,
    private val simulationEngine: TelemetrySimulationEngine,
    private val adminAuthManager: AdminAuthManager,
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FleetOptViewModel(repository, simulationEngine, adminAuthManager, themePreferenceManager) as T
    }
}
