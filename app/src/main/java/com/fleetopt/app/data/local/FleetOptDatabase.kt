package com.fleetopt.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fleetopt.app.core.forecasting.CngDemandForecastingEngine
import com.fleetopt.app.data.local.dao.DispatchTripDao
import com.fleetopt.app.data.local.dao.HcvDao
import com.fleetopt.app.data.local.dao.HourlySalesDao
import com.fleetopt.app.data.local.dao.StationDao
import com.fleetopt.app.data.local.entity.DispatchTripEntity
import com.fleetopt.app.data.local.entity.HcvEntity
import com.fleetopt.app.data.local.entity.HourlySalesEntity
import com.fleetopt.app.data.local.entity.StationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        StationEntity::class,
        HcvEntity::class,
        HourlySalesEntity::class,
        DispatchTripEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FleetOptDatabase : RoomDatabase() {

    abstract fun stationDao(): StationDao
    abstract fun hcvDao(): HcvDao
    abstract fun hourlySalesDao(): HourlySalesDao
    abstract fun dispatchTripDao(): DispatchTripDao

    companion object {
        @Volatile
        private var INSTANCE: FleetOptDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): FleetOptDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FleetOptDatabase::class.java,
                    "fleetopt_cng.db"
                )
                    .addCallback(FleetOptDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class FleetOptDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: FleetOptDatabase) {
            val stationDao = database.stationDao()
            val hcvDao = database.hcvDao()
            val hourlySalesDao = database.hourlySalesDao()
            val tripDao = database.dispatchTripDao()

            // 1. Initial Mother & Daughter Stations
            val initialStations = listOf(
                // Mother Stations (CGS Loading Hubs)
                StationEntity(
                    id = "cgs-shamshabad",
                    name = "CGS Shamshabad Mother Hub",
                    lat = 17.2403,
                    lng = 78.4294,
                    geometricVolumeLiters = 10000.0,
                    currentPressureBar = 250.0,
                    totalBays = 4,
                    occupiedBays = 1,
                    baselineDailyDemandKg = 0.0,
                    distanceKm = 0.0,
                    traffic = "low",
                    stationType = "MOTHER",
                    motherStationId = null,
                    motherStationName = null,
                    compressorCapacityScmh = 1800.0,
                    demandDeliveredTodayKg = 0.0
                ),
                StationEntity(
                    id = "cgs-medchal",
                    name = "CGS Medchal Mother Hub",
                    lat = 17.6297,
                    lng = 78.4814,
                    geometricVolumeLiters = 8000.0,
                    currentPressureBar = 250.0,
                    totalBays = 3,
                    occupiedBays = 0,
                    baselineDailyDemandKg = 0.0,
                    distanceKm = 0.0,
                    traffic = "low",
                    stationType = "MOTHER",
                    motherStationId = null,
                    motherStationName = null,
                    compressorCapacityScmh = 1200.0,
                    demandDeliveredTodayKg = 0.0
                ),
                // Daughter Stations (Matching fleetopt.lovable.app)
                StationEntity(
                    id = "tejas",
                    name = "Tejas FS",
                    lat = 17.503,
                    lng = 78.452,
                    geometricVolumeLiters = 3000.0,
                    currentPressureBar = 74.0,
                    totalBays = 2,
                    occupiedBays = 1,
                    baselineDailyDemandKg = 3000.0,
                    distanceKm = 20.0,
                    traffic = "low",
                    demandDeliveredTodayKg = 150.0,
                    stationType = "DAUGHTER",
                    motherStationId = "cgs-shamshabad",
                    motherStationName = "CGS Shamshabad Mother Hub"
                ),
                StationEntity(
                    id = "goel",
                    name = "Goel FS",
                    lat = 17.612,
                    lng = 78.238,
                    geometricVolumeLiters = 3500.0,
                    currentPressureBar = 142.0,
                    totalBays = 2,
                    occupiedBays = 0,
                    baselineDailyDemandKg = 1500.0,
                    distanceKm = 40.0,
                    traffic = "moderate",
                    demandDeliveredTodayKg = 180.0,
                    stationType = "DAUGHTER",
                    motherStationId = "cgs-shamshabad",
                    motherStationName = "CGS Shamshabad Mother Hub"
                ),
                StationEntity(
                    id = "medak",
                    name = "Medak FS",
                    lat = 17.900,
                    lng = 78.150,
                    geometricVolumeLiters = 4000.0,
                    currentPressureBar = 64.0,
                    totalBays = 1,
                    occupiedBays = 0,
                    baselineDailyDemandKg = 2000.0,
                    distanceKm = 75.0,
                    traffic = "moderate",
                    demandDeliveredTodayKg = 120.0,
                    stationType = "DAUGHTER",
                    motherStationId = "cgs-medchal",
                    motherStationName = "CGS Medchal Mother Hub"
                ),
                StationEntity(
                    id = "savitha",
                    name = "Savitha FS",
                    lat = 17.240,
                    lng = 78.830,
                    geometricVolumeLiters = 4500.0,
                    currentPressureBar = 58.0,
                    totalBays = 2,
                    occupiedBays = 0,
                    baselineDailyDemandKg = 3000.0,
                    distanceKm = 80.0,
                    traffic = "heavy",
                    demandDeliveredTodayKg = 260.0,
                    stationType = "DAUGHTER",
                    motherStationId = "cgs-shamshabad",
                    motherStationName = "CGS Shamshabad Mother Hub"
                ),
                StationEntity(
                    id = "balaji",
                    name = "Balaji FS",
                    lat = 17.090,
                    lng = 78.110,
                    geometricVolumeLiters = 3500.0,
                    currentPressureBar = 112.0,
                    totalBays = 1,
                    occupiedBays = 0,
                    baselineDailyDemandKg = 2000.0,
                    distanceKm = 83.0,
                    traffic = "moderate",
                    demandDeliveredTodayKg = 350.0,
                    stationType = "DAUGHTER",
                    motherStationId = "cgs-medchal",
                    motherStationName = "CGS Medchal Mother Hub"
                )
            )
            stationDao.insertStations(initialStations)

            // 2. Initial HCV Fleet (650 kg cascade tankers)
            val initialHcvs = listOf(
                HcvEntity(
                    registration = "TS07UG5579",
                    driverName = "Ravi Kumar",
                    driverPhone = "+91 98490 11234",
                    capacityKg = 650.0,
                    status = "AVAILABLE",
                    currentLocation = "CGS Yard - Bay 1",
                    heelPressureBar = 24.5,
                    isHydroTestValid = true,
                    hydroTestExpiryDate = "2027-04-10",
                    isPrvCertified = true,
                    tireConditionPercent = 92,
                    distanceToCgsKm = 0.5,
                    tripsToday = 2
                ),
                HcvEntity(
                    registration = "TS07UG8058",
                    driverName = "Suresh Naik",
                    driverPhone = "+91 98490 22345",
                    capacityKg = 650.0,
                    status = "AVAILABLE",
                    currentLocation = "CGS Yard - Bay 2",
                    heelPressureBar = 25.0,
                    isHydroTestValid = true,
                    hydroTestExpiryDate = "2027-06-22",
                    isPrvCertified = true,
                    tireConditionPercent = 88,
                    distanceToCgsKm = 1.0,
                    tripsToday = 1
                ),
                HcvEntity(
                    registration = "TS07UG8059",
                    driverName = "Imran Shaikh",
                    driverPhone = "+91 98490 33456",
                    capacityKg = 650.0,
                    status = "ON_TRIP",
                    assignedStationId = "tejas",
                    currentLocation = "NH44 Corridor (Km 12)",
                    lat = 17.485,
                    lng = 78.400,
                    heelPressureBar = 22.0,
                    isHydroTestValid = true,
                    hydroTestExpiryDate = "2027-01-18",
                    isPrvCertified = true,
                    tireConditionPercent = 94,
                    distanceToCgsKm = 8.0,
                    tripsToday = 1
                ),
                HcvEntity(
                    registration = "TS07UG8060",
                    driverName = "Mahesh Reddy",
                    driverPhone = "+91 98490 44567",
                    capacityKg = 650.0,
                    status = "AVAILABLE",
                    currentLocation = "CGS Yard - Bay 3",
                    heelPressureBar = 26.0,
                    isHydroTestValid = true,
                    hydroTestExpiryDate = "2027-09-05",
                    isPrvCertified = true,
                    tireConditionPercent = 90,
                    distanceToCgsKm = 1.2,
                    tripsToday = 3
                ),
                HcvEntity(
                    registration = "TS07UG8062",
                    driverName = "Anil Yadav",
                    driverPhone = "+91 98490 55678",
                    capacityKg = 650.0,
                    status = "MAINTENANCE",
                    breakdownReason = "Scheduled Hydrostatic Recertification",
                    currentLocation = "CGS Maintenance Workshop",
                    heelPressureBar = 5.0,
                    isHydroTestValid = false,
                    hydroTestExpiryDate = "2026-09-01",
                    isPrvCertified = false,
                    tireConditionPercent = 70,
                    distanceToCgsKm = 0.2,
                    tripsToday = 0
                ),
                HcvEntity(
                    registration = "TS07UG8063",
                    driverName = "Prakash Rao",
                    driverPhone = "+91 98490 66789",
                    capacityKg = 650.0,
                    status = "AVAILABLE",
                    currentLocation = "CGS Yard - Bay 4",
                    heelPressureBar = 23.5,
                    isHydroTestValid = true,
                    hydroTestExpiryDate = "2027-11-30",
                    isPrvCertified = true,
                    tireConditionPercent = 86,
                    distanceToCgsKm = 1.5,
                    tripsToday = 2
                ),
                HcvEntity(
                    registration = "TS07UG8065",
                    driverName = "Vinod Sharma",
                    driverPhone = "+91 98490 77890",
                    capacityKg = 650.0,
                    status = "ON_TRIP",
                    assignedStationId = "savitha",
                    currentLocation = "Outer Ring Road (Km 35)",
                    lat = 17.320,
                    lng = 78.650,
                    heelPressureBar = 25.0,
                    isHydroTestValid = true,
                    hydroTestExpiryDate = "2027-03-14",
                    isPrvCertified = true,
                    tireConditionPercent = 89,
                    distanceToCgsKm = 35.0,
                    tripsToday = 1
                ),
                HcvEntity(
                    registration = "GJ18BT3470",
                    driverName = "Jagdish Patel",
                    driverPhone = "+91 98490 88901",
                    capacityKg = 650.0,
                    status = "AVAILABLE",
                    currentLocation = "CGS Holding Area",
                    heelPressureBar = 27.0,
                    isHydroTestValid = true,
                    hydroTestExpiryDate = "2027-12-15",
                    isPrvCertified = true,
                    tireConditionPercent = 95,
                    distanceToCgsKm = 2.0,
                    tripsToday = 1
                ),
                HcvEntity(
                    registration = "GJ18BT7517",
                    driverName = "Naresh Gupta",
                    driverPhone = "+91 98490 99012",
                    capacityKg = 650.0,
                    status = "BREAKDOWN",
                    breakdownReason = "Tire Puncture & Manifold Coupling Check",
                    currentLocation = "Medak Highway Km 42 Lay-by",
                    lat = 17.750,
                    lng = 78.220,
                    heelPressureBar = 30.0,
                    isHydroTestValid = true,
                    hydroTestExpiryDate = "2027-05-19",
                    isPrvCertified = true,
                    tireConditionPercent = 40,
                    distanceToCgsKm = 42.0,
                    tripsToday = 0
                )
            )
            hcvDao.insertHcvList(initialHcvs)

            // 3. 7-Day Hourly Dispensing History per station
            val salesRecords = mutableListOf<HourlySalesEntity>()
            initialStations.forEach { station ->
                val baseAvgHourly = station.baselineDailyDemandKg / 24.0
                for (day in 1..7) {
                    val dayVariance = when (day) {
                        7 -> 1.05 // Same day last week correlation
                        6 -> 1.02
                        5 -> 0.98
                        else -> 1.00 + ((day % 3 - 1) * 0.04)
                    }
                    for (hour in 0..23) {
                        val diurnal = CngDemandForecastingEngine.getDiurnalMultiplier(hour)
                        val sales = baseAvgHourly * diurnal * dayVariance
                        salesRecords.add(
                            HourlySalesEntity(
                                stationId = station.id,
                                dayOffset = day,
                                hourOfDay = hour,
                                salesKg = (sales * 10.0).toInt() / 10.0
                            )
                        )
                    }
                }
            }
            hourlySalesDao.insertSales(salesRecords)

            // 4. Initial completed dispatch trips for audit log
            val sampleTrips = listOf(
                DispatchTripEntity(
                    tripId = "TRIP-101",
                    hcvRegistration = "TS07UG5579",
                    driverName = "Ravi Kumar",
                    driverPhone = "+91 98490 11234",
                    stationId = "tejas",
                    stationName = "Tejas FS",
                    dispatchedMassKg = 650.0,
                    decantedMassKg = 625.0,
                    departureTime = System.currentTimeMillis() - 14_400_000,
                    etaMinutes = 25,
                    status = "COMPLETED",
                    routeCostInr = 1450,
                    estimatedSavingsInr = 2770
                ),
                DispatchTripEntity(
                    tripId = "TRIP-102",
                    hcvRegistration = "TS07UG8060",
                    driverName = "Mahesh Reddy",
                    driverPhone = "+91 98490 44567",
                    stationId = "goel",
                    stationName = "Goel FS",
                    dispatchedMassKg = 650.0,
                    decantedMassKg = 630.0,
                    departureTime = System.currentTimeMillis() - 28_800_000,
                    etaMinutes = 55,
                    status = "COMPLETED",
                    routeCostInr = 2780,
                    estimatedSavingsInr = 2470
                ),
                DispatchTripEntity(
                    tripId = "TRIP-103",
                    hcvRegistration = "TS07UG8059",
                    driverName = "Imran Shaikh",
                    driverPhone = "+91 98490 33456",
                    stationId = "tejas",
                    stationName = "Tejas FS",
                    dispatchedMassKg = 650.0,
                    decantedMassKg = 0.0,
                    departureTime = System.currentTimeMillis() - 900_000,
                    etaMinutes = 20,
                    status = "EN_ROUTE",
                    routeCostInr = 1450,
                    estimatedSavingsInr = 2770
                ),
                DispatchTripEntity(
                    tripId = "TRIP-104",
                    hcvRegistration = "TS07UG8065",
                    driverName = "Vinod Sharma",
                    driverPhone = "+91 98490 77890",
                    stationId = "savitha",
                    stationName = "Savitha FS",
                    dispatchedMassKg = 650.0,
                    decantedMassKg = 0.0,
                    departureTime = System.currentTimeMillis() - 1_800_000,
                    etaMinutes = 110,
                    status = "EN_ROUTE",
                    routeCostInr = 5360,
                    estimatedSavingsInr = 2240
                )
            )
            sampleTrips.forEach { tripDao.insertTrip(it) }
        }
    }
}
