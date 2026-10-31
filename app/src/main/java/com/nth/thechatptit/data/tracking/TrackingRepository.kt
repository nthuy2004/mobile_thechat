package com.nth.thechatptit.data.tracking

import android.location.Location
import androidx.room.withTransaction
import com.nth.thechatptit.data.local.AppDatabase
import com.nth.thechatptit.data.local.TrackPointEntity
import com.nth.thechatptit.data.local.WorkoutEntity
import com.nth.thechatptit.data.local.WorkoutSegmentEntity
import com.nth.thechatptit.data.auth.AuthRepository
import com.nth.thechatptit.data.model.GpsStatus
import com.nth.thechatptit.data.model.TrackPoint
import com.nth.thechatptit.data.model.TrackingState
import com.nth.thechatptit.data.model.TrackingStatus
import com.nth.thechatptit.data.model.WorkoutStatus
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.data.model.SyncStatus
import com.nth.thechatptit.data.model.Workout
import com.nth.thechatptit.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingRepository @Inject constructor(
    private val database: AppDatabase,
    private val authRepository: AuthRepository,
    settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(TrackingState())
    private val _route = MutableStateFlow<List<TrackPoint>>(emptyList())
    private var lastLocation: Location? = null
    private var nextSequence = 0L
    @Volatile private var autoPauseEnabled = false
    private var lastMovementAtEpochMillis: Long? = null
    private var hasMovementSample = false

    val state: StateFlow<TrackingState> = _state.asStateFlow()
    val route: StateFlow<List<TrackPoint>> = _route.asStateFlow()

    init {
        scope.launch {
            settingsRepository.settings.collect { autoPauseEnabled = it.autoPauseEnabled }
        }
        scope.launch { restore() }
    }

    suspend fun restore() = mutex.withLock {
        restoreLocked()
    }

    suspend fun start(type: WorkoutType) = mutex.withLock {
        if (database.workoutDao().getActive() != null) {
            restoreLocked()
            return@withLock
        }
        val now = System.currentTimeMillis()
        val workoutId = UUID.randomUUID().toString()
        val segmentId = UUID.randomUUID().toString()
        val workout = WorkoutEntity(
            id = workoutId,
            ownerUserId = (authRepository.state.value as? com.nth.thechatptit.data.model.AuthState.LoggedIn)
                ?.user?.id,
            type = type.name,
            status = WorkoutStatus.RECORDING.name,
            title = null,
            startedAtEpochMillis = now,
            finishedAtEpochMillis = null,
            activeDurationMillis = 0L,
            pausedDurationMillis = 0L,
            activeStartedAtEpochMillis = now,
            pauseStartedAtEpochMillis = null,
            distanceMeters = 0.0,
            visibility = "PRIVATE",
            syncStatus = "LOCAL_ONLY",
            updatedAtEpochMillis = now
        )
        val segment = WorkoutSegmentEntity(
            id = segmentId,
            workoutId = workoutId,
            sequence = 0,
            startedAtEpochMillis = now,
            finishedAtEpochMillis = null,
            durationMillis = 0L,
            distanceMeters = 0.0
        )
        database.withTransaction {
            database.workoutDao().upsert(workout)
            database.workoutSegmentDao().upsert(segment)
        }
        lastLocation = null
        nextSequence = 0L
        lastMovementAtEpochMillis = now
        hasMovementSample = false
        _route.value = emptyList()
        _state.value = TrackingState(
            status = TrackingStatus.RECORDING,
            workoutId = workoutId,
            activeSegmentId = segmentId,
            workoutType = type,
            gpsStatus = GpsStatus.SEARCHING
        )
    }

    suspend fun pause() = mutex.withLock {
        pauseLocked()
    }

    private suspend fun pauseLocked() {
        val current = _state.value
        if (current.status != TrackingStatus.RECORDING || current.workoutId == null) return
        val workout = database.workoutDao().getById(current.workoutId) ?: return
        val now = System.currentTimeMillis()
        val activeDuration = calculateActiveDuration(workout, now)
        val updatedWorkout = workout.copy(
            status = WorkoutStatus.PAUSED.name,
            activeDurationMillis = activeDuration,
            activeStartedAtEpochMillis = null,
            pauseStartedAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        val segment = current.activeSegmentId?.let { database.workoutSegmentDao().getById(it) }
        database.withTransaction {
            database.workoutDao().upsert(updatedWorkout)
            if (segment != null) {
                database.workoutSegmentDao().upsert(
                    segment.copy(
                        finishedAtEpochMillis = now,
                        durationMillis = (now - segment.startedAtEpochMillis).coerceAtLeast(0L)
                    )
                )
            }
        }
        _state.update {
            it.copy(
                status = TrackingStatus.PAUSED,
                activeDurationMillis = activeDuration,
                currentSpeedMetersPerSecond = null
            )
        }
    }

    suspend fun resume() = mutex.withLock {
        val current = _state.value
        if (current.status != TrackingStatus.PAUSED || current.workoutId == null || current.workoutType == null) return@withLock
        val workout = database.workoutDao().getById(current.workoutId) ?: return@withLock
        val now = System.currentTimeMillis()
        val pauseDuration = workout.pauseStartedAtEpochMillis?.let { (now - it).coerceAtLeast(0L) } ?: 0L
        val segmentId = UUID.randomUUID().toString()
        val sequence = database.workoutSegmentDao().getForWorkout(workout.id).size
        val segment = WorkoutSegmentEntity(
            id = segmentId,
            workoutId = workout.id,
            sequence = sequence,
            startedAtEpochMillis = now,
            finishedAtEpochMillis = null,
            durationMillis = 0L,
            distanceMeters = 0.0
        )
        val updatedWorkout = workout.copy(
            status = WorkoutStatus.RECORDING.name,
            pausedDurationMillis = workout.pausedDurationMillis + pauseDuration,
            activeStartedAtEpochMillis = now,
            pauseStartedAtEpochMillis = null,
            updatedAtEpochMillis = now
        )
        database.withTransaction {
            database.workoutDao().upsert(updatedWorkout)
            database.workoutSegmentDao().upsert(segment)
        }
        lastLocation = null
        lastMovementAtEpochMillis = now
        hasMovementSample = false
        _state.update {
            it.copy(
                status = TrackingStatus.RECORDING,
                activeSegmentId = segmentId,
                currentSpeedMetersPerSecond = null,
                gpsStatus = GpsStatus.SEARCHING
            )
        }
    }

    suspend fun finish() = mutex.withLock {
        val current = _state.value
        val workoutId = current.workoutId ?: return@withLock
        val workout = database.workoutDao().getById(workoutId) ?: return@withLock
        val now = System.currentTimeMillis()
        val activeDuration = if (workout.status == WorkoutStatus.RECORDING.name) calculateActiveDuration(workout, now) else workout.activeDurationMillis
        val pausedDuration = if (workout.status == WorkoutStatus.PAUSED.name) {
            workout.pausedDurationMillis + (workout.pauseStartedAtEpochMillis?.let { now - it } ?: 0L).coerceAtLeast(0L)
        } else workout.pausedDurationMillis
        val updatedWorkout = workout.copy(
            status = WorkoutStatus.COMPLETED.name,
            finishedAtEpochMillis = now,
            activeDurationMillis = activeDuration,
            pausedDurationMillis = pausedDuration,
            activeStartedAtEpochMillis = null,
            pauseStartedAtEpochMillis = null,
            updatedAtEpochMillis = now
        )
        val segment = current.activeSegmentId?.let { database.workoutSegmentDao().getById(it) }
        database.withTransaction {
            database.workoutDao().upsert(updatedWorkout)
            if (segment != null && segment.finishedAtEpochMillis == null) {
                database.workoutSegmentDao().upsert(
                    segment.copy(
                        finishedAtEpochMillis = now,
                        durationMillis = (now - segment.startedAtEpochMillis).coerceAtLeast(0L)
                    )
                )
            }
        }
        _state.value = TrackingState()
        _route.value = emptyList()
        lastLocation = null
    }

    suspend fun stopAndWait(timeoutMillis: Long = 15_000L) {
        withTimeout(timeoutMillis) {
            state.first { it.status == TrackingStatus.IDLE }
        }
    }

    suspend fun updateDuration() = mutex.withLock {
        val current = _state.value
        if (current.status != TrackingStatus.RECORDING || current.workoutId == null) return@withLock
        val now = System.currentTimeMillis()
        if (
            autoPauseEnabled && hasMovementSample &&
            lastMovementAtEpochMillis?.let { now - it >= AUTO_PAUSE_AFTER_MILLIS } == true
        ) {
            pauseLocked()
            return@withLock
        }
        val workout = database.workoutDao().getById(current.workoutId) ?: return@withLock
        val duration = calculateActiveDuration(workout, now)
        database.workoutDao().upsert(
            workout.copy(
                activeDurationMillis = duration,
                activeStartedAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
        )
        _state.update { it.copy(activeDurationMillis = duration) }
    }

    suspend fun acceptLocation(location: Location) = mutex.withLock {
        val current = _state.value
        if (current.status != TrackingStatus.RECORDING || current.workoutId == null || current.activeSegmentId == null) return@withLock
        if (!location.hasAccuracy() || location.accuracy > 50f) {
            _state.update { it.copy(gpsStatus = GpsStatus.WEAK) }
            return@withLock
        }
        val previous = lastLocation
        val capturedAt = if (location.time > 0L) location.time else System.currentTimeMillis()
        val distance = previous?.distanceTo(location)?.toDouble() ?: 0.0
        val delta = if (previous == null) 0L else (capturedAt - previous.time).coerceAtLeast(0L)
        val calculatedSpeed = if (delta > 0L) distance / (delta / 1_000.0) else 0.0
        val reportedSpeed = if (location.hasSpeed()) location.speed.toDouble() else null
        val speed = reportedSpeed ?: calculatedSpeed
        val maxSpeed = if (current.workoutType == WorkoutType.CYCLING) 40.0 else 15.0
        val minimumDistance = maxOf(2.0, location.accuracy.toDouble() * 0.5)
        val speedMismatch = previous != null &&
            reportedSpeed != null &&
            calculatedSpeed > reportedSpeed * 2.0 + 1.0
        val acceptsLocation = previous == null || (
            distance >= minimumDistance &&
                !speedMismatch &&
                speed <= maxSpeed &&
                (reportedSpeed == null || reportedSpeed <= maxSpeed)
            )
        if (!acceptsLocation) {
            _state.update {
                it.copy(
                    currentPoint = pointFor(location, current.workoutId, current.activeSegmentId),
                    currentSpeedMetersPerSecond = reportedSpeed?.coerceAtLeast(0.0),
                    gpsStatus = GpsStatus.GOOD
                )
            }
            return@withLock
        }
        val point = TrackPoint(
            workoutId = current.workoutId,
            segmentId = current.activeSegmentId,
            sequence = nextSequence++,
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
            accuracyMeters = location.accuracy,
            speedMetersPerSecond = if (location.hasSpeed()) location.speed else null,
            capturedAtEpochMillis = capturedAt
        )
        val totalDistance = current.distanceMeters + distance
        val workout = database.workoutDao().getById(current.workoutId) ?: return@withLock
        val segment = database.workoutSegmentDao().getById(current.activeSegmentId)
        database.withTransaction {
            database.trackPointDao().insert(point.toEntity())
            database.workoutDao().upsert(workout.copy(distanceMeters = totalDistance, updatedAtEpochMillis = System.currentTimeMillis()))
            if (segment != null) database.workoutSegmentDao().upsert(segment.copy(distanceMeters = segment.distanceMeters + distance))
        }
        lastLocation = location.apply { time = capturedAt }
        if (previous == null || (distance >= minimumDistance && speed >= AUTO_PAUSE_MIN_SPEED_METERS_PER_SECOND)) {
            lastMovementAtEpochMillis = capturedAt
            hasMovementSample = true
        }
        _route.update { it + point }
        _state.update {
            it.copy(
                distanceMeters = totalDistance,
                currentPoint = point,
                currentSpeedMetersPerSecond = reportedSpeed?.coerceAtLeast(0.0),
                gpsStatus = GpsStatus.GOOD
            )
        }
    }

    fun setGpsStatus(status: GpsStatus) {
        _state.update { it.copy(gpsStatus = status) }
    }

    suspend fun getCompletedWorkouts(userId: String? = null): List<Workout> = mutex.withLock {
        database.workoutDao().getCompletedForUser(userId).map { it.toDomain() }
    }

    suspend fun getWorkout(id: String): Workout? = mutex.withLock {
        database.workoutDao().getById(id)?.toDomain()
    }

    suspend fun getRoute(workoutId: String): List<TrackPoint> = mutex.withLock {
        database.trackPointDao().getForWorkout(workoutId).map { it.toDomain() }
    }

    private suspend fun restoreLocked() {
        val workout = database.workoutDao().getActive() ?: return
        val points = database.trackPointDao().getForWorkout(workout.id)
        val currentSegment = database.workoutSegmentDao().getForWorkout(workout.id).lastOrNull { it.finishedAtEpochMillis == null }
        val type = runCatching { WorkoutType.valueOf(workout.type) }.getOrDefault(WorkoutType.RUNNING)
        val status = if (workout.status == WorkoutStatus.PAUSED.name) TrackingStatus.PAUSED else TrackingStatus.RECORDING
        val duration = calculateActiveDuration(workout, System.currentTimeMillis())
        val lastPoint = points.lastOrNull()
        nextSequence = (lastPoint?.sequence ?: -1L) + 1L
        lastLocation = lastPoint?.toLocation()
        lastMovementAtEpochMillis = System.currentTimeMillis()
        hasMovementSample = lastPoint != null
        _route.value = points.map { it.toDomain() }
        _state.value = TrackingState(
            status = status,
            workoutId = workout.id,
            activeSegmentId = currentSegment?.id,
            workoutType = type,
            activeDurationMillis = duration,
            distanceMeters = workout.distanceMeters,
            currentPoint = lastPoint?.toDomain(),
            currentSpeedMetersPerSecond = lastPoint?.speedMetersPerSecond?.toDouble(),
            gpsStatus = GpsStatus.SEARCHING
        )
    }

    private fun pointFor(
        location: Location,
        workoutId: String,
        segmentId: String
    ): TrackPoint = TrackPoint(
        workoutId = workoutId,
        segmentId = segmentId,
        sequence = nextSequence,
        latitude = location.latitude,
        longitude = location.longitude,
        altitudeMeters = if (location.hasAltitude()) location.altitude else null,
        accuracyMeters = location.accuracy,
        speedMetersPerSecond = if (location.hasSpeed()) location.speed else null,
        capturedAtEpochMillis = if (location.time > 0L) location.time else System.currentTimeMillis()
    )

    private fun calculateActiveDuration(workout: WorkoutEntity, now: Long): Long {
        val running = if (workout.status == WorkoutStatus.RECORDING.name) {
            (now - (workout.activeStartedAtEpochMillis ?: now)).coerceAtLeast(0L)
        } else 0L
        return workout.activeDurationMillis + running
    }

    companion object {
        private const val AUTO_PAUSE_AFTER_MILLIS = 30_000L
        private const val AUTO_PAUSE_MIN_SPEED_METERS_PER_SECOND = 0.7
    }

    private fun TrackPoint.toEntity() = TrackPointEntity(
        workoutId, segmentId, sequence, latitude, longitude, altitudeMeters,
        accuracyMeters, speedMetersPerSecond, capturedAtEpochMillis
    )

    private fun TrackPointEntity.toDomain() = TrackPoint(
        workoutId, segmentId, sequence, latitude, longitude, altitudeMeters,
        accuracyMeters, speedMetersPerSecond, capturedAtEpochMillis
    )

    private fun TrackPointEntity.toLocation() = Location("restored").apply {
        latitude = this@toLocation.latitude
        longitude = this@toLocation.longitude
        accuracy = this@toLocation.accuracyMeters
        time = this@toLocation.capturedAtEpochMillis
        this@toLocation.altitudeMeters?.let { altitude = it }
        this@toLocation.speedMetersPerSecond?.let { speed = it }
    }

    private fun WorkoutEntity.toDomain() = Workout(
        id = id,
        ownerUserId = ownerUserId,
        type = runCatching { WorkoutType.valueOf(type) }.getOrDefault(WorkoutType.RUNNING),
        status = runCatching { WorkoutStatus.valueOf(status) }.getOrDefault(WorkoutStatus.COMPLETED),
        title = title,
        startedAtEpochMillis = startedAtEpochMillis,
        finishedAtEpochMillis = finishedAtEpochMillis,
        activeDurationMillis = activeDurationMillis,
        pausedDurationMillis = pausedDurationMillis,
        activeStartedAtEpochMillis = activeStartedAtEpochMillis,
        pauseStartedAtEpochMillis = pauseStartedAtEpochMillis,
        distanceMeters = distanceMeters,
        visibility = runCatching { com.nth.thechatptit.data.model.WorkoutVisibility.valueOf(visibility) }
            .getOrDefault(com.nth.thechatptit.data.model.WorkoutVisibility.PRIVATE),
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.LOCAL_ONLY),
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
