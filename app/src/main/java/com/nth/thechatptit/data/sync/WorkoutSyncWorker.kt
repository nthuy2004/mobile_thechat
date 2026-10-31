package com.nth.thechatptit.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import com.nth.thechatptit.data.repo.PostRepository
import com.nth.thechatptit.data.repo.WorkoutRepository
import com.nth.thechatptit.data.model.SyncStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WorkoutSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val workoutRepository: WorkoutRepository,
    private val postRepository: PostRepository,
    private val cloudRepository: FirebaseSyncRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val userId = inputData.getString(USER_ID) ?: return Result.failure()
        if (!cloudRepository.isConfigured || !cloudRepository.isCurrentUser(userId)) {
            return Result.success()
        }

        return try {
            val workoutId = inputData.getString(WORKOUT_ID)
            val workouts = if (workoutId == null) {
                workoutRepository.getPendingSyncForUser(userId)
            } else {
                listOfNotNull(workoutRepository.getWorkout(workoutId))
            }

            for (workout in workouts) {
                if (workout.ownerUserId != null && workout.ownerUserId != userId) continue
                workoutRepository.claimForUser(workout.id, userId)
                val ownedWorkout = workoutRepository.getWorkout(workout.id) ?: continue
                if (ownedWorkout.ownerUserId != userId) continue

                cloudRepository.syncWorkout(
                    ownedWorkout,
                    workoutRepository.getRoute(ownedWorkout.id),
                    userId
                )
                workoutRepository.updateSyncStatus(ownedWorkout.id, SyncStatus.SYNCED)

            }

            for (post in postRepository.getPendingSyncForUser(userId)) {
                if (post.authorUserId != null && post.authorUserId != userId) continue
                val workout = workoutRepository.getWorkout(post.workoutId) ?: continue
                if (workout.ownerUserId != null && workout.ownerUserId != userId) continue
                workoutRepository.claimForUser(workout.id, userId)
                if (workoutRepository.getWorkout(workout.id)?.ownerUserId != userId) continue
                postRepository.claimForUser(post.id, userId)
                val ownedPost = post.copy(authorUserId = userId)
                cloudRepository.syncPost(ownedPost, userId)
                postRepository.updateSyncStatus(post.id, SyncStatus.SYNCED)
            }

            cloudRepository.loadWorkouts(userId).forEach { remote ->
                workoutRepository.importRemoteWorkout(remote.workout, remote.route, userId)
            }
            cloudRepository.loadPosts(userId).forEach { post ->
                postRepository.importRemotePost(post, userId)
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: UploadApiException) {
            if (error.retryable) Result.retry() else Result.failure()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORKOUT_ID = "workout_id"
        const val USER_ID = "user_id"
    }
}
