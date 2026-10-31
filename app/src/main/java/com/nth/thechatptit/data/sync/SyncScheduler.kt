package com.nth.thechatptit.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nth.thechatptit.data.repo.PostRepository
import com.nth.thechatptit.data.repo.WorkoutRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext context: Context,
    private val workoutRepository: WorkoutRepository,
    private val postRepository: PostRepository
) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueueWorkout(workoutId: String, userId: String) {
        enqueue(
            uniqueName = "sync-workout-$userId-$workoutId",
            data = Data.Builder()
                .putString(WorkoutSyncWorker.USER_ID, userId)
                .putString(WorkoutSyncWorker.WORKOUT_ID, workoutId)
                .build()
        )
    }

    suspend fun enqueueAccountSync(userId: String) {
        workoutRepository.claimUnownedCompletedForUser(userId)
        postRepository.claimUnownedForUser(userId)
        enqueue(
            uniqueName = "sync-account-$userId",
            data = Data.Builder().putString(WorkoutSyncWorker.USER_ID, userId).build()
        )
    }

    private fun enqueue(uniqueName: String, data: Data) {
        val request = OneTimeWorkRequestBuilder<WorkoutSyncWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                30_000L,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
        workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, request)
    }
}
