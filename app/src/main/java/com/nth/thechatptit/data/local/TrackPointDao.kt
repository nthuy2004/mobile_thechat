package com.nth.thechatptit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: TrackPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<TrackPointEntity>)

    @Query("DELETE FROM track_points WHERE workoutId = :workoutId")
    suspend fun deleteForWorkout(workoutId: String)

    @Query("SELECT * FROM track_points WHERE workoutId = :workoutId ORDER BY sequence ASC")
    suspend fun getForWorkout(workoutId: String): List<TrackPointEntity>

    @Query("SELECT * FROM track_points WHERE workoutId = :workoutId ORDER BY sequence DESC LIMIT 1")
    suspend fun getLast(workoutId: String): TrackPointEntity?
}
