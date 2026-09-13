package com.fleetopt.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val geometricVolumeLiters: Double,
    val currentPressureBar: Double,
    val totalBays: Int,
    val occupiedBays: Int,
    val baselineDailyDemandKg: Double,
    val distanceKm: Double,
    val traffic: String, // "low", "moderate", "heavy"
    val demandDeliveredTodayKg: Double = 0.0,
    val temperatureKelvin: Double = 298.15,
    val lastTelemetryTimestamp: Long = System.currentTimeMillis(),
    val stationType: String = "DAUGHTER", // "MOTHER" or "DAUGHTER"
    val motherStationId: String? = null,
    val motherStationName: String? = null,
    val compressorCapacityScmh: Double = 1200.0
)
