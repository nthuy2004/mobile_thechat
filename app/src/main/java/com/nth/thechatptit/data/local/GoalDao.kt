package com.nth.thechatptit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?

    @Query(
        "SELECT * FROM goals WHERE isActive = 1 AND " +
            "((:userId IS NULL AND ownerUserId IS NULL) OR ownerUserId = :userId) " +
            "ORDER BY createdAtEpochMillis DESC"
    )
    suspend fun getActiveForUser(userId: String?): List<GoalEntity>

    @Query("UPDATE goals SET ownerUserId = :userId WHERE ownerUserId IS NULL")
    suspend fun claimUnownedForUser(userId: String)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)
}
