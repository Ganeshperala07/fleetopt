package com.fleetopt.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dispatch_trips")
data class DispatchTripEntity(
    @PrimaryKey
    val tripId: String,
    val hcvRegistration: String,
    val driverName: String,
    val driverPhone: String,
    val stationId: String,
    val stationName: String,
    val dispatchedMassKg: Double,
    val decantedMassKg: Double = 0.0,
    val departureTime: Long = System.currentTimeMillis(),
    val etaMinutes: Int,
    val status: String, // "EN_ROUTE", "DECANTING", "COMPLETED", "CANCELLED"
    val routeCostInr: Int,
    val estimatedSavingsInr: Int,
    val isSplitMilkRun: Boolean = false,
    val secondaryStationId: String? = null,
    val secondaryStationName: String? = null,
    val secondaryMassKg: Double = 0.0
)
