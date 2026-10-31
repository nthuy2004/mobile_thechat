package com.nth.thechatptit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutSegmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(segment: WorkoutSegmentEntity)

    @Query("SELECT * FROM workout_segments WHERE workoutId = :workoutId ORDER BY sequence ASC")
    suspend fun getForWorkout(workoutId: String): List<WorkoutSegmentEntity>

    @Query("SELECT * FROM workout_segments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkoutSegmentEntity?

    @Query("DELETE FROM workout_segments WHERE workoutId = :workoutId")
    suspend fun deleteForWorkout(workoutId: String)
}
