package com.nth.thechatptit.data.repo

import com.nth.thechatptit.data.local.AppDatabase
import androidx.room.withTransaction
import com.nth.thechatptit.data.model.TrackPoint
import com.nth.thechatptit.data.model.Workout
import com.nth.thechatptit.data.model.WorkoutSegment
import com.nth.thechatptit.data.model.WorkoutStatus
import com.nth.thechatptit.data.model.WorkoutVisibility
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.data.model.WorkoutStatistics
import com.nth.thechatptit.data.model.DailyWorkoutStatistics
import com.nth.thechatptit.data.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val database: AppDatabase
) {
    suspend fun getWorkout(id: String): Workout? = withContext(Dispatchers.IO) {
        database.workoutDao().getById(id)?.toDomain()
    }

    suspend fun getCompletedWorkouts(userId: String?): List<Workout> = withContext(Dispatchers.IO) {
        database.workoutDao().getCompletedForUser(userId).map { it.toDomain() }
    }

    suspend fun getStatistics(
        fromEpochMillis: Long,
        toEpochMillis: Long,
        workoutType: WorkoutType? = null,
        userId: String? = null
    ): WorkoutStatistics = withContext(Dispatchers.IO) {
        val workouts = database.workoutDao().getCompletedBetweenForUser(userId, fromEpochMillis, toEpochMillis)
            .map { it.toDomain() }
            .filter { workoutType == null || it.type == workoutType }
        WorkoutStatistics(
            fromEpochMillis = fromEpochMillis,
            toEpochMillis = toEpochMillis,
            workoutType = workoutType,
            workoutCount = workouts.size,
            totalDistanceMeters = workouts.sumOf { it.distanceMeters },
            totalActiveDurationMillis = workouts.sumOf { it.activeDurationMillis }
        )
    }

    suspend fun getDailyStatistics(
        fromEpochMillis: Long,
        toEpochMillis: Long,
        userId: String? = null
    ): List<DailyWorkoutStatistics> = withContext(Dispatchers.IO) {
        val workouts = database.workoutDao()
            .getCompletedBetweenForUser(userId, fromEpochMillis, toEpochMillis)
            .map { it.toDomain() }
        val grouped = workouts.groupBy { workout ->
            Calendar.getInstance().apply { timeInMillis = workout.startedAtEpochMillis }
                .get(Calendar.DAY_OF_WEEK)
        }
        val firstDay = Calendar.getInstance().apply {
            timeInMillis = fromEpochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        (0..6).map { offset ->
            val day = (firstDay.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
                .get(Calendar.DAY_OF_WEEK)
            val dailyWorkouts = grouped[day].orEmpty()
            DailyWorkoutStatistics(
                dayOfWeek = day,
                distanceMeters = dailyWorkouts.sumOf { it.distanceMeters },
                activeDurationMillis = dailyWorkouts.sumOf { it.activeDurationMillis },
                workoutCount = dailyWorkouts.size
            )
        }
    }

    suspend fun getRoute(workoutId: String): List<TrackPoint> = withContext(Dispatchers.IO) {
        database.trackPointDao().getForWorkout(workoutId).map { it.toDomain() }
    }

    suspend fun getSegments(workoutId: String): List<WorkoutSegment> = withContext(Dispatchers.IO) {
        database.workoutSegmentDao().getForWorkout(workoutId).map { it.toDomain() }
    }

    suspend fun updateSyncStatus(workoutId: String, status: SyncStatus) = withContext(Dispatchers.IO) {
        database.workoutDao().updateSyncStatus(workoutId, status.name, System.currentTimeMillis())
    }

    suspend fun getPendingSyncForUser(userId: String): List<Workout> = withContext(Dispatchers.IO) {
        database.workoutDao().getPendingSyncForUser(userId).map { it.toDomain() }
    }

    suspend fun claimForUser(workoutId: String, userId: String) = withContext(Dispatchers.IO) {
        database.workoutDao().claimForUser(workoutId, userId)
    }

    suspend fun claimUnownedCompletedForUser(userId: String) = withContext(Dispatchers.IO) {
        database.workoutDao().claimUnownedCompletedForUser(userId)
    }

    suspend fun importRemoteWorkout(workout: Workout, route: List<TrackPoint>, userId: String) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val existing = database.workoutDao().getById(workout.id)
                if (existing?.ownerUserId != null && existing.ownerUserId != userId) return@withTransaction
                if (existing != null && existing.syncStatus != SyncStatus.SYNCED.name) return@withTransaction

                database.workoutDao().upsert(
                    workout.copy(ownerUserId = userId, syncStatus = SyncStatus.SYNCED).toEntity()
                )
                database.trackPointDao().deleteForWorkout(workout.id)
                database.trackPointDao().insertAll(route.map { it.toEntity() })
            }
        }

    suspend fun updateShareInfo(
        workoutId: String,
        title: String?,
        visibility: WorkoutVisibility
    ) = withContext(Dispatchers.IO) {
        val workout = database.workoutDao().getById(workoutId) ?: return@withContext
        val now = System.currentTimeMillis()
        database.workoutDao().upsert(
            workout.copy(
                title = title?.trim()?.takeIf { it.isNotEmpty() },
                visibility = visibility.name,
                updatedAtEpochMillis = now
            )
        )
    }

    suspend fun discardWorkout(workoutId: String): String? = withContext(Dispatchers.IO) {
        val post = database.postDao().getForWorkout(workoutId)
        database.withTransaction {
            database.postDao().deleteForWorkout(workoutId)
            database.trackPointDao().deleteForWorkout(workoutId)
            database.workoutSegmentDao().deleteForWorkout(workoutId)
            database.workoutDao().deleteById(workoutId)
        }
        post?.imageUrl
    }

    private fun com.nth.thechatptit.data.local.WorkoutEntity.toDomain() = Workout(
        id = id,
        ownerUserId = ownerUserId,
        type = runCatching { com.nth.thechatptit.data.model.WorkoutType.valueOf(type) }
            .getOrDefault(com.nth.thechatptit.data.model.WorkoutType.RUNNING),
        status = runCatching { com.nth.thechatptit.data.model.WorkoutStatus.valueOf(status) }
            .getOrDefault(com.nth.thechatptit.data.model.WorkoutStatus.COMPLETED),
        title = title,
        startedAtEpochMillis = startedAtEpochMillis,
        finishedAtEpochMillis = finishedAtEpochMillis,
        activeDurationMillis = activeDurationMillis,
        pausedDurationMillis = pausedDurationMillis,
        activeStartedAtEpochMillis = activeStartedAtEpochMillis,
        pauseStartedAtEpochMillis = pauseStartedAtEpochMillis,
        distanceMeters = distanceMeters,
        visibility = runCatching { WorkoutVisibility.valueOf(visibility) }
            .getOrDefault(WorkoutVisibility.PRIVATE),
        syncStatus = runCatching { com.nth.thechatptit.data.model.SyncStatus.valueOf(syncStatus) }
            .getOrDefault(com.nth.thechatptit.data.model.SyncStatus.LOCAL_ONLY),
        updatedAtEpochMillis = updatedAtEpochMillis
    )

    private fun com.nth.thechatptit.data.local.TrackPointEntity.toDomain() = TrackPoint(
        workoutId = workoutId,
        segmentId = segmentId,
        sequence = sequence,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
        accuracyMeters = accuracyMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        capturedAtEpochMillis = capturedAtEpochMillis
    )

    private fun Workout.toEntity() = com.nth.thechatptit.data.local.WorkoutEntity(
        id = id,
        ownerUserId = ownerUserId,
        type = type.name,
        status = status.name,
        title = title,
        startedAtEpochMillis = startedAtEpochMillis,
        finishedAtEpochMillis = finishedAtEpochMillis,
        activeDurationMillis = activeDurationMillis,
        pausedDurationMillis = pausedDurationMillis,
        activeStartedAtEpochMillis = null,
        pauseStartedAtEpochMillis = null,
        distanceMeters = distanceMeters,
        visibility = visibility.name,
        syncStatus = syncStatus.name,
        updatedAtEpochMillis = updatedAtEpochMillis
    )

    private fun TrackPoint.toEntity() = com.nth.thechatptit.data.local.TrackPointEntity(
        workoutId = workoutId,
        segmentId = segmentId,
        sequence = sequence,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
        accuracyMeters = accuracyMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        capturedAtEpochMillis = capturedAtEpochMillis
    )

    private fun com.nth.thechatptit.data.local.WorkoutSegmentEntity.toDomain() = WorkoutSegment(
        id = id,
        workoutId = workoutId,
        sequence = sequence,
        startedAtEpochMillis = startedAtEpochMillis,
        finishedAtEpochMillis = finishedAtEpochMillis,
        durationMillis = durationMillis,
        distanceMeters = distanceMeters
    )
}
