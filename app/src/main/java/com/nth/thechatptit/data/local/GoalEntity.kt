package com.nth.thechatptit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    indices = [Index("ownerUserId"), Index("isActive")]
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String?,
    val title: String,
    val metric: String,
    val workoutType: String?,
    val period: String,
    val targetValue: Double,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long?,
    val reminderMinutesOfDay: Int?,
    val reminderTimeZoneId: String?,
    val reminderDaysOfWeekMask: Int?,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
