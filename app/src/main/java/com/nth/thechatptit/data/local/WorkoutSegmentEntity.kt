package com.nth.thechatptit.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "workout_segments",
    primaryKeys = ["id"],
    indices = [Index("workoutId"), Index(value = ["workoutId", "sequence"], unique = true)]
)
data class WorkoutSegmentEntity(
    val id: String,
    val workoutId: String,
    val sequence: Int,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long?,
    val durationMillis: Long,
    val distanceMeters: Double
)
