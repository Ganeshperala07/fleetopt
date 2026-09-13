package com.fleetopt.app.data.repository

import com.fleetopt.app.core.engine.DispatchRecommendation
import com.fleetopt.app.data.local.FleetOptDatabase
import com.fleetopt.app.data.local.entity.DispatchTripEntity
import com.fleetopt.app.data.local.entity.HcvEntity
import com.fleetopt.app.data.local.entity.HourlySalesEntity
import com.fleetopt.app.data.local.entity.StationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FleetOptRepository(private val database: FleetOptDatabase) {

    private val stationDao = database.stationDao()
    private val hcvDao = database.hcvDao()
    private val hourlySalesDao = database.hourlySalesDao()
    private val tripDao = database.dispatchTripDao()

    val stationsFlow: Flow<List<StationEntity>> = stationDao.getAllStationsFlow()
    val motherStationsFlow: Flow<List<StationEntity>> = stationDao.getMotherStationsFlow()
    val hcvListFlow: Flow<List<HcvEntity>> = hcvDao.getAllHcvFlow()
    val tripsFlow: Flow<List<DispatchTripEntity>> = tripDao.getAllTripsFlow()

    suspend fun updateStationPressure(stationId: String, pressureBar: Double) = withContext(Dispatchers.IO) {
        stationDao.updatePressure(stationId, pressureBar)
    }

    suspend fun addStation(station: StationEntity) = withContext(Dispatchers.IO) {
        stationDao.insertStation(station)
    }

    suspend fun updateStation(station: StationEntity) = withContext(Dispatchers.IO) {
        stationDao.updateStation(station)
    }

    suspend fun deleteStation(stationId: String) = withContext(Dispatchers.IO) {
        stationDao.deleteStationById(stationId)
    }

    suspend fun getStationById(stationId: String): StationEntity? = withContext(Dispatchers.IO) {
        stationDao.getStationById(stationId)
    }

    suspend fun getHourlySales(stationId: String): List<HourlySalesEntity> = withContext(Dispatchers.IO) {
        hourlySalesDao.getSalesForStation(stationId)
    }

    suspend fun reportBreakdown(registration: String, reason: String) = withContext(Dispatchers.IO) {
        hcvDao.reportBreakdown(registration, reason)
    }

    suspend fun clearMaintenance(registration: String) = withContext(Dispatchers.IO) {
        hcvDao.clearMaintenance(registration, residualBar = 25.0)
    }

    suspend fun addHcv(hcv: HcvEntity) = withContext(Dispatchers.IO) {
        hcvDao.insertHcv(hcv)
    }

    suspend fun updateHcv(hcv: HcvEntity) = withContext(Dispatchers.IO) {
        hcvDao.updateHcv(hcv)
    }

    suspend fun deleteHcv(registration: String) = withContext(Dispatchers.IO) {
        hcvDao.deleteHcvByRegistration(registration)
    }

    suspend fun updateHcvLocation(registration: String, lat: Double, lng: Double, location: String) = withContext(Dispatchers.IO) {
        hcvDao.updateLocation(registration, lat, lng, location)
    }

    suspend fun executeDispatch(recommendation: DispatchRecommendation): String = withContext(Dispatchers.IO) {
        val tripId = "TRIP-${System.currentTimeMillis()}"
        val newTrip = DispatchTripEntity(
            tripId = tripId,
            hcvRegistration = recommendation.assignedHcv.registration,
            driverName = recommendation.assignedHcv.driverName,
            driverPhone = recommendation.assignedHcv.driverPhone,
            stationId = recommendation.targetStation.stationId,
            stationName = recommendation.targetStation.stationName,
            dispatchedMassKg = recommendation.recommendedPayloadKg,
            decantedMassKg = 0.0,
            departureTime = System.currentTimeMillis(),
            etaMinutes = recommendation.etaMinutes,
            status = "EN_ROUTE",
            routeCostInr = recommendation.tripCostInr,
            estimatedSavingsInr = recommendation.estimatedSavingsInr,
            isSplitMilkRun = recommendation.isSplitMilkRun,
            secondaryStationId = recommendation.secondaryStation?.stationId,
            secondaryStationName = recommendation.secondaryStation?.stationName,
            secondaryMassKg = recommendation.secondaryPayloadKg
        )

        tripDao.insertTrip(newTrip)

        // Update HCV status to ON_TRIP
        hcvDao.updateStatus(
            registration = recommendation.assignedHcv.registration,
            status = "ON_TRIP",
            stationId = recommendation.targetStation.stationId,
            location = "En Route to ${recommendation.targetStation.stationName}"
        )

        // Increment station occupied bays
        val station = stationDao.getStationById(recommendation.targetStation.stationId)
        if (station != null) {
            val newOccupied = (station.occupiedBays + 1).coerceAtMost(station.totalBays)
            stationDao.updateOccupiedBays(station.id, newOccupied)
        }

        tripId
    }

    suspend fun completeTrip(tripId: String) = withContext(Dispatchers.IO) {
        val trip = tripDao.getTripById(tripId) ?: return@withContext
        if (trip.status == "COMPLETED") return@withContext

        // Mark trip as COMPLETED with full delivered payload
        tripDao.updateTripStatus(tripId, "COMPLETED", trip.dispatchedMassKg)

        // Return HCV to AVAILABLE status at CGS Yard
        hcvDao.updateStatus(
            registration = trip.hcvRegistration,
            status = "AVAILABLE",
            stationId = null,
            location = "CGS Yard"
        )

        // Release station bay & replenish station pressure and delivered demand
        val station = stationDao.getStationById(trip.stationId)
        if (station != null) {
            val newOccupied = (station.occupiedBays - 1).coerceAtLeast(0)
            stationDao.updateOccupiedBays(station.id, newOccupied)

            val newDelivered = station.demandDeliveredTodayKg + trip.dispatchedMassKg
            val barBoost = trip.dispatchedMassKg / 15.0
            val newPressure = (station.currentPressureBar + barBoost).coerceAtMost(230.0)
            stationDao.updatePressureAndDeliveredDemand(station.id, newPressure, newDelivered)
        }
    }

    suspend fun cancelTrip(tripId: String) = withContext(Dispatchers.IO) {
        val trip = tripDao.getTripById(tripId) ?: return@withContext
        if (trip.status == "CANCELLED" || trip.status == "COMPLETED") return@withContext

        tripDao.updateTripStatus(tripId, "CANCELLED", 0.0)

        // Return HCV to AVAILABLE status
        hcvDao.updateStatus(
            registration = trip.hcvRegistration,
            status = "AVAILABLE",
            stationId = null,
            location = "CGS Yard"
        )

        // Release station bay
        val station = stationDao.getStationById(trip.stationId)
        if (station != null) {
            val newOccupied = (station.occupiedBays - 1).coerceAtLeast(0)
            stationDao.updateOccupiedBays(station.id, newOccupied)
        }
    }
}
