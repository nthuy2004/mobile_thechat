package com.nth.thechatptit.data.repo

import com.nth.thechatptit.data.local.AppDatabase
import com.nth.thechatptit.data.local.PostEntity
import com.nth.thechatptit.data.model.Post
import com.nth.thechatptit.data.model.SyncStatus
import com.nth.thechatptit.data.model.WorkoutVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val database: AppDatabase
) {
    suspend fun createPost(
        workoutId: String,
        caption: String?,
        imageUrl: String?,
        visibility: WorkoutVisibility
    ): Post = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = database.postDao().getForWorkout(workoutId)
        val post = PostEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            ownerUserId = existing?.ownerUserId ?: database.workoutDao().getById(workoutId)?.ownerUserId,
            workoutId = workoutId,
            caption = caption?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = imageUrl,
            visibility = visibility.name,
            syncStatus = SyncStatus.LOCAL_ONLY.name,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now
        )
        database.postDao().upsert(post)
        post.toDomain()
    }

    suspend fun getRecentPosts(userId: String?): List<Post> = withContext(Dispatchers.IO) {
        database.postDao().getRecentForUser(userId).map { it.toDomain() }
    }

    suspend fun getPostForWorkout(workoutId: String): Post? = withContext(Dispatchers.IO) {
        database.postDao().getForWorkout(workoutId)?.toDomain()
    }

    suspend fun updateSyncStatus(postId: String, status: SyncStatus) = withContext(Dispatchers.IO) {
        database.postDao().updateSyncStatus(postId, status.name, System.currentTimeMillis())
    }

    suspend fun claimForUser(postId: String, userId: String) = withContext(Dispatchers.IO) {
        database.postDao().claimForUser(postId, userId)
    }

    suspend fun claimUnownedForUser(userId: String) = withContext(Dispatchers.IO) {
        database.postDao().claimUnownedForUser(userId)
    }

    suspend fun getPendingSyncForUser(userId: String): List<Post> = withContext(Dispatchers.IO) {
        database.postDao().getPendingSyncForUser(userId).map { it.toDomain() }
    }

    suspend fun importRemotePost(post: Post, userId: String) = withContext(Dispatchers.IO) {
        val existing = database.postDao().getForWorkout(post.workoutId)
        if (existing?.ownerUserId != null && existing.ownerUserId != userId) return@withContext
        if (existing != null && existing.syncStatus != SyncStatus.SYNCED.name) return@withContext
        database.postDao().upsert(
            PostEntity(
                id = post.id,
                ownerUserId = userId,
                workoutId = post.workoutId,
                caption = post.caption,
                imageUrl = post.imageUrl,
                visibility = post.visibility.name,
                syncStatus = SyncStatus.SYNCED.name,
                createdAtEpochMillis = post.createdAtEpochMillis,
                updatedAtEpochMillis = post.updatedAtEpochMillis
            )
        )
    }

    private fun PostEntity.toDomain() = Post(
        id = id,
        authorUserId = ownerUserId,
        workoutId = workoutId,
        caption = caption,
        imageUrl = imageUrl,
        visibility = runCatching { WorkoutVisibility.valueOf(visibility) }
            .getOrDefault(WorkoutVisibility.PRIVATE),
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }
            .getOrDefault(SyncStatus.LOCAL_ONLY),
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
