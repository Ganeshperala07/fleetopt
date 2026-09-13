package com.fleetopt.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hcv_fleet")
data class HcvEntity(
    @PrimaryKey
    val registration: String,
    val driverName: String,
    val driverPhone: String,
    val capacityKg: Double = 650.0,
    val status: String, // "AVAILABLE", "LOADING", "ON_TRIP", "DECANTING", "MAINTENANCE", "BREAKDOWN"
    val breakdownReason: String? = null,
    val isHydroTestValid: Boolean = true,
    val hydroTestExpiryDate: String = "2027-08-15",
    val isPrvCertified: Boolean = true,
    val tireConditionPercent: Int = 90,
    val heelPressureBar: Double = 25.0,
    val currentLocation: String = "CGS Yard",
    val lat: Double = 17.4526,
    val lng: Double = 78.3312,
    val assignedStationId: String? = null,
    val distanceToCgsKm: Double = 0.0,
    val tripsToday: Int = 0,
    val homeMotherStationId: String = "cgs-shamshabad",
    val homeMotherStationName: String = "CGS Shamshabad Mother Hub"
)
