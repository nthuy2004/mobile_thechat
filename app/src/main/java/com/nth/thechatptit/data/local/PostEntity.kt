package com.nth.thechatptit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    indices = [Index("workoutId"), Index("createdAtEpochMillis")]
)
data class PostEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String?,
    val workoutId: String,
    val caption: String?,
    val imageUrl: String?,
    val visibility: String,
    val syncStatus: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
