package com.nth.thechatptit.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "workouts",
    indices = [Index("status"), Index("startedAtEpochMillis")]
)
data class WorkoutEntity(
    @androidx.room.PrimaryKey val id: String,
    val ownerUserId: String?,
    val type: String,
    val status: String,
    val title: String?,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long?,
    val activeDurationMillis: Long,
    val pausedDurationMillis: Long,
    val activeStartedAtEpochMillis: Long?,
    val pauseStartedAtEpochMillis: Long?,
    val distanceMeters: Double,
    val visibility: String,
    val syncStatus: String,
    val updatedAtEpochMillis: Long
)
