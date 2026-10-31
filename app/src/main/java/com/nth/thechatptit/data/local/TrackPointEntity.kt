package com.nth.thechatptit.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "track_points",
    primaryKeys = ["workoutId", "sequence"],
    indices = [Index("workoutId"), Index("segmentId")]
)
data class TrackPointEntity(
    val workoutId: String,
    val segmentId: String,
    val sequence: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float?,
    val capturedAtEpochMillis: Long
)
