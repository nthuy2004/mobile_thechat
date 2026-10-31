package com.nth.thechatptit.data.sync

import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nth.thechatptit.data.model.Post
import com.nth.thechatptit.data.model.TrackPoint
import com.nth.thechatptit.data.model.Workout
import com.nth.thechatptit.data.model.WorkoutStatus
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.data.model.WorkoutVisibility
import kotlinx.coroutines.tasks.await
import java.io.File
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteWorkoutRecord(val workout: Workout, val route: List<TrackPoint>)

@Singleton
class FirebaseSyncRepository @Inject constructor(
    private val uploadClient: PhpUploadClient
) {
    private val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    val isConfigured: Boolean
        get() = auth != null && firestore != null && uploadClient.isConfigured

    fun isCurrentUser(userId: String): Boolean = auth?.currentUser?.uid == userId

    suspend fun syncWorkout(workout: Workout, route: List<TrackPoint>, userId: String) {
        requireCurrentUser(userId)
        val store = firestore ?: error("Firebase chưa được cấu hình")
        val routePath = if (route.isEmpty()) {
            null
        } else {
            uploadClient.uploadBytes(
                userId = userId,
                kind = "route",
                fileName = "route.json",
                bytes = route.toJson().toByteArray(StandardCharsets.UTF_8)
            )
        }
        val data = mapOf(
            "id" to workout.id,
            "ownerUserId" to userId,
            "type" to workout.type.name,
            "status" to workout.status.name,
            "title" to workout.title,
            "startedAtEpochMillis" to workout.startedAtEpochMillis,
            "finishedAtEpochMillis" to workout.finishedAtEpochMillis,
            "activeDurationMillis" to workout.activeDurationMillis,
            "pausedDurationMillis" to workout.pausedDurationMillis,
            "distanceMeters" to workout.distanceMeters,
            "visibility" to workout.visibility.name,
            "routePath" to routePath,
            "updatedAtEpochMillis" to workout.updatedAtEpochMillis
        )
        store.collection("users").document(userId).collection("workouts")
            .document(workout.id).set(data).await()
    }

    suspend fun syncPost(post: Post, userId: String) {
        requireCurrentUser(userId)
        val store = firestore ?: error("Firebase chưa được cấu hình")
        var imageUrl = post.imageUrl
        val localImage = post.imageUrl?.takeUnless(uploadClient::isApiFilePath)?.let(::File)
        if (localImage?.isFile == true) {
            imageUrl = uploadClient.uploadFile(userId, "post_image", localImage)
        }
        store.collection("users").document(userId).collection("posts")
            .document(post.id)
            .set(
                mapOf(
                    "id" to post.id,
                    "ownerUserId" to userId,
                    "workoutId" to post.workoutId,
                    "caption" to post.caption,
                    "imageUrl" to imageUrl,
                    "visibility" to post.visibility.name,
                    "createdAtEpochMillis" to post.createdAtEpochMillis,
                    "updatedAtEpochMillis" to post.updatedAtEpochMillis
                )
            ).await()
    }

    suspend fun loadWorkouts(userId: String): List<RemoteWorkoutRecord> {
        requireCurrentUser(userId)
        val store = firestore ?: error("Firebase chưa được cấu hình")
        return store.collection("users").document(userId).collection("workouts")
            .get().await().documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                if (data["ownerUserId"] != userId) return@mapNotNull null
                val type = enumValue<WorkoutType>(data["type"]) ?: return@mapNotNull null
                val status = enumValue<WorkoutStatus>(data["status"]) ?: WorkoutStatus.COMPLETED
                val workout = Workout(
                    id = document.id,
                    ownerUserId = userId,
                    type = type,
                    status = status,
                    title = data["title"] as? String,
                    startedAtEpochMillis = data.long("startedAtEpochMillis") ?: return@mapNotNull null,
                    finishedAtEpochMillis = data.long("finishedAtEpochMillis"),
                    activeDurationMillis = data.long("activeDurationMillis") ?: 0L,
                    pausedDurationMillis = data.long("pausedDurationMillis") ?: 0L,
                    distanceMeters = data.double("distanceMeters") ?: 0.0,
                    visibility = enumValue<WorkoutVisibility>(data["visibility"])
                        ?: WorkoutVisibility.PRIVATE,
                    updatedAtEpochMillis = data.long("updatedAtEpochMillis") ?: 0L
                )
                RemoteWorkoutRecord(workout, loadRoute(data, userId))
            }
    }

    suspend fun loadPosts(userId: String): List<Post> {
        requireCurrentUser(userId)
        val store = firestore ?: error("Firebase chưa được cấu hình")
        return store.collection("users").document(userId).collection("posts")
            .get().await().documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                if (data["ownerUserId"] != userId) return@mapNotNull null
                val workoutId = data["workoutId"] as? String ?: return@mapNotNull null
                val imagePath = data["imageUrl"] as? String
                val localImagePath = if (uploadClient.isApiFilePath(imagePath)) {
                    uploadClient.downloadImage(userId, imagePath!!)
                } else {
                    imagePath
                }
                Post(
                    id = document.id,
                    authorUserId = userId,
                    workoutId = workoutId,
                    caption = data["caption"] as? String,
                    imageUrl = localImagePath,
                    visibility = enumValue<WorkoutVisibility>(data["visibility"])
                        ?: WorkoutVisibility.PRIVATE,
                    createdAtEpochMillis = data.long("createdAtEpochMillis") ?: 0L,
                    updatedAtEpochMillis = data.long("updatedAtEpochMillis") ?: 0L
                )
            }
    }

    private suspend fun loadRoute(data: Map<String, Any?>, userId: String): List<TrackPoint> {
        val path = (data["routePath"] as? String)
            ?: (data["routeStoragePath"] as? String)
        if (!path.isNullOrBlank() && uploadClient.isApiFilePath(path)) {
            val bytes = uploadClient.downloadBytes(userId, path, MAX_ROUTE_BYTES)
            return JSONArray(String(bytes, StandardCharsets.UTF_8)).toTrackPoints()
        }
        // Read routes stored in older Firestore documents.
        val oldRoute = data["route"] as? List<*> ?: return emptyList()
        return oldRoute.mapNotNull { value ->
            val point = value as? Map<*, *> ?: return@mapNotNull null
            point.toTrackPoint()
        }
    }

    private fun List<TrackPoint>.toJson(): String = JSONArray().apply {
        forEach { point ->
            put(
                JSONObject()
                    .put("workoutId", point.workoutId)
                    .put("segmentId", point.segmentId)
                    .put("sequence", point.sequence)
                    .put("latitude", point.latitude)
                    .put("longitude", point.longitude)
                    .put("altitudeMeters", point.altitudeMeters ?: JSONObject.NULL)
                    .put("accuracyMeters", point.accuracyMeters.toDouble())
                    .put("speedMetersPerSecond", point.speedMetersPerSecond ?: JSONObject.NULL)
                    .put("capturedAtEpochMillis", point.capturedAtEpochMillis)
            )
        }
    }.toString()

    private fun JSONArray.toTrackPoints(): List<TrackPoint> = buildList {
        for (index in 0 until length()) {
            val point = optJSONObject(index) ?: continue
            add(
                TrackPoint(
                    workoutId = point.optString("workoutId"),
                    segmentId = point.optString("segmentId"),
                    sequence = point.optLong("sequence"),
                    latitude = point.optDouble("latitude"),
                    longitude = point.optDouble("longitude"),
                    altitudeMeters = point.optionalDouble("altitudeMeters"),
                    accuracyMeters = point.optDouble("accuracyMeters").toFloat(),
                    speedMetersPerSecond = point.optionalDouble("speedMetersPerSecond")?.toFloat(),
                    capturedAtEpochMillis = point.optLong("capturedAtEpochMillis")
                )
            )
        }
    }

    private fun Map<*, *>.toTrackPoint(): TrackPoint? {
        val workoutId = this["workoutId"] as? String ?: return null
        val segmentId = this["segmentId"] as? String ?: return null
        return TrackPoint(
            workoutId = workoutId,
            segmentId = segmentId,
            sequence = (this["sequence"] as? Number)?.toLong() ?: return null,
            latitude = (this["latitude"] as? Number)?.toDouble() ?: return null,
            longitude = (this["longitude"] as? Number)?.toDouble() ?: return null,
            altitudeMeters = (this["altitudeMeters"] as? Number)?.toDouble(),
            accuracyMeters = (this["accuracyMeters"] as? Number)?.toFloat() ?: 0f,
            speedMetersPerSecond = (this["speedMetersPerSecond"] as? Number)?.toFloat(),
            capturedAtEpochMillis = (this["capturedAtEpochMillis"] as? Number)?.toLong() ?: return null
        )
    }

    private fun JSONObject.optionalDouble(key: String): Double? =
        if (isNull(key) || !has(key)) null else optDouble(key)

    private fun Map<String, Any?>.long(key: String): Long? = (this[key] as? Number)?.toLong()

    private fun Map<String, Any?>.double(key: String): Double? = (this[key] as? Number)?.toDouble()

    private inline fun <reified T : Enum<T>> enumValue(value: Any?): T? =
        (value as? String)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

    private fun requireCurrentUser(userId: String) {
        check(auth?.currentUser?.uid == userId) { "Tài khoản đã thay đổi, thử đồng bộ lại sau." }
    }

    companion object {
        private const val MAX_ROUTE_BYTES = 3 * 1024 * 1024
    }
}
