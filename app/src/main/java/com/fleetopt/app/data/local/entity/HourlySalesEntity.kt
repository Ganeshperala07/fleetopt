package com.fleetopt.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hourly_sales",
    indices = [Index(value = ["stationId", "dayOffset", "hourOfDay"], unique = true)]
)
data class HourlySalesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stationId: String,
    val dayOffset: Int, // 0 = today, 1 = yesterday, ..., 7 = 7 days ago
    val hourOfDay: Int, // 0 to 23
    val salesKg: Double
)
