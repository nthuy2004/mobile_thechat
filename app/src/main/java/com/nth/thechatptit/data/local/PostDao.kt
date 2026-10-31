package com.nth.thechatptit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(post: PostEntity)

    @Query("SELECT * FROM posts WHERE ((:userId IS NULL AND ownerUserId IS NULL) OR ownerUserId = :userId) ORDER BY createdAtEpochMillis DESC")
    suspend fun getRecentForUser(userId: String?): List<PostEntity>

    @Query("SELECT * FROM posts WHERE workoutId = :workoutId LIMIT 1")
    suspend fun getForWorkout(workoutId: String): PostEntity?

    @Query("DELETE FROM posts WHERE workoutId = :workoutId")
    suspend fun deleteForWorkout(workoutId: String)

    @Query("UPDATE posts SET syncStatus = :syncStatus, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE posts SET ownerUserId = :userId WHERE id = :id AND ownerUserId IS NULL")
    suspend fun claimForUser(id: String, userId: String)

    @Query("UPDATE posts SET ownerUserId = :userId WHERE ownerUserId IS NULL AND workoutId IN (SELECT id FROM workouts WHERE ownerUserId = :userId)")
    suspend fun claimUnownedForUser(userId: String)

    @Query("SELECT * FROM posts WHERE syncStatus != 'SYNCED' AND (ownerUserId IS NULL OR ownerUserId = :userId) ORDER BY createdAtEpochMillis ASC")
    suspend fun getPendingSyncForUser(userId: String): List<PostEntity>
}
