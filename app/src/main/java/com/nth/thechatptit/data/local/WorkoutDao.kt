package com.nth.thechatptit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE status IN ('RECORDING', 'PAUSED') LIMIT 1")
    suspend fun getActive(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkoutEntity?

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' AND ((:userId IS NULL AND ownerUserId IS NULL) OR ownerUserId = :userId) ORDER BY startedAtEpochMillis DESC")
    suspend fun getCompletedForUser(userId: String?): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' AND ((:userId IS NULL AND ownerUserId IS NULL) OR ownerUserId = :userId) AND startedAtEpochMillis >= :fromEpochMillis AND startedAtEpochMillis < :toEpochMillis ORDER BY startedAtEpochMillis DESC")
    suspend fun getCompletedBetweenForUser(userId: String?, fromEpochMillis: Long, toEpochMillis: Long): List<WorkoutEntity>

    @Query("UPDATE workouts SET syncStatus = :syncStatus, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE workouts SET ownerUserId = :userId WHERE id = :id AND ownerUserId IS NULL")
    suspend fun claimForUser(id: String, userId: String)

    @Query("UPDATE workouts SET ownerUserId = :userId WHERE status = 'COMPLETED' AND ownerUserId IS NULL")
    suspend fun claimUnownedCompletedForUser(userId: String)

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' AND syncStatus != 'SYNCED' AND (ownerUserId IS NULL OR ownerUserId = :userId) ORDER BY startedAtEpochMillis ASC")
    suspend fun getPendingSyncForUser(userId: String): List<WorkoutEntity>
}
