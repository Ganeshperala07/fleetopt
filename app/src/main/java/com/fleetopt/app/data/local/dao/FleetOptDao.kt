package com.fleetopt.app.data.local.dao

import androidx.room.*
import com.fleetopt.app.data.local.entity.DispatchTripEntity
import com.fleetopt.app.data.local.entity.HcvEntity
import com.fleetopt.app.data.local.entity.HourlySalesEntity
import com.fleetopt.app.data.local.entity.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY name ASC")
    fun getAllStationsFlow(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE id = :stationId LIMIT 1")
    suspend fun getStationById(stationId: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<StationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: StationEntity)

    @Update
    suspend fun updateStation(station: StationEntity)

    @Delete
    suspend fun deleteStation(station: StationEntity)

    @Query("DELETE FROM stations WHERE id = :stationId")
    suspend fun deleteStationById(stationId: String)

    @Query("SELECT * FROM stations WHERE stationType = 'MOTHER' ORDER BY name ASC")
    fun getMotherStationsFlow(): Flow<List<StationEntity>>

    @Query("UPDATE stations SET currentPressureBar = :pressureBar, lastTelemetryTimestamp = :timestamp WHERE id = :stationId")
    suspend fun updatePressure(stationId: String, pressureBar: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE stations SET currentPressureBar = :pressureBar, demandDeliveredTodayKg = :deliveredDemand, lastTelemetryTimestamp = :timestamp WHERE id = :stationId")
    suspend fun updatePressureAndDeliveredDemand(stationId: String, pressureBar: Double, deliveredDemand: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE stations SET occupiedBays = :occupiedBays WHERE id = :stationId")
    suspend fun updateOccupiedBays(stationId: String, occupiedBays: Int)
}

@Dao
interface HcvDao {
    @Query("SELECT * FROM hcv_fleet ORDER BY registration ASC")
    fun getAllHcvFlow(): Flow<List<HcvEntity>>

    @Query("SELECT * FROM hcv_fleet WHERE registration = :registration LIMIT 1")
    suspend fun getHcvByRegistration(registration: String): HcvEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHcvList(hcvList: List<HcvEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHcv(hcv: HcvEntity)

    @Update
    suspend fun updateHcv(hcv: HcvEntity)

    @Delete
    suspend fun deleteHcv(hcv: HcvEntity)

    @Query("DELETE FROM hcv_fleet WHERE registration = :registration")
    suspend fun deleteHcvByRegistration(registration: String)

    @Query("UPDATE hcv_fleet SET status = :status, assignedStationId = :stationId, currentLocation = :location WHERE registration = :registration")
    suspend fun updateStatus(registration: String, status: String, stationId: String?, location: String)

    @Query("UPDATE hcv_fleet SET status = 'BREAKDOWN', breakdownReason = :reason WHERE registration = :registration")
    suspend fun reportBreakdown(registration: String, reason: String)

    @Query("UPDATE hcv_fleet SET status = 'AVAILABLE', breakdownReason = null, heelPressureBar = :residualBar WHERE registration = :registration")
    suspend fun clearMaintenance(registration: String, residualBar: Double = 25.0)

    @Query("UPDATE hcv_fleet SET lat = :lat, lng = :lng, currentLocation = :location WHERE registration = :registration")
    suspend fun updateLocation(registration: String, lat: Double, lng: Double, location: String)
}

@Dao
interface HourlySalesDao {
    @Query("SELECT * FROM hourly_sales WHERE stationId = :stationId ORDER BY dayOffset ASC, hourOfDay ASC")
    suspend fun getSalesForStation(stationId: String): List<HourlySalesEntity>

    @Query("SELECT SUM(salesKg) FROM hourly_sales WHERE stationId = :stationId AND dayOffset = :dayOffset")
    suspend fun getDailySaleForDay(stationId: String, dayOffset: Int): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<HourlySalesEntity>)
}

@Dao
interface DispatchTripDao {
    @Query("SELECT * FROM dispatch_trips ORDER BY departureTime DESC")
    fun getAllTripsFlow(): Flow<List<DispatchTripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: DispatchTripEntity)

    @Update
    suspend fun updateTrip(trip: DispatchTripEntity)

    @Query("SELECT * FROM dispatch_trips WHERE tripId = :tripId LIMIT 1")
    suspend fun getTripById(tripId: String): DispatchTripEntity?

    @Query("UPDATE dispatch_trips SET status = :status, decantedMassKg = :decantedMass WHERE tripId = :tripId")
    suspend fun updateTripStatus(tripId: String, status: String, decantedMass: Double)
}
