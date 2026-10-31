package com.nth.thechatptit.ui.summary

import android.app.Application
import android.location.Geocoder
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nth.thechatptit.data.model.TrackPoint
import com.nth.thechatptit.data.model.Workout
import com.nth.thechatptit.data.model.WorkoutVisibility
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.data.auth.AuthRepository
import com.nth.thechatptit.data.model.AuthState
import com.nth.thechatptit.data.repo.PostRepository
import com.nth.thechatptit.data.repo.RouteImageGenerator
import com.nth.thechatptit.data.repo.WorkoutRepository
import com.nth.thechatptit.data.settings.SettingsRepository
import com.nth.thechatptit.data.sync.FirebaseSyncRepository
import com.nth.thechatptit.data.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface SummarySaveResult {
    data object Synced : SummarySaveResult
    data object LocalOnly : SummarySaveResult
    data class Failed(val reason: String) : SummarySaveResult
}

data class SummaryActionState(
    val isSaving: Boolean = false,
    val isCancelling: Boolean = false,
    val result: SummarySaveResult? = null
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    application: Application,
    private val workoutRepository: WorkoutRepository,
    private val postRepository: PostRepository,
    private val routeImageGenerator: RouteImageGenerator,
    settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: FirebaseSyncRepository
) : AndroidViewModel(application) {

    private val _workout = MutableStateFlow<Workout?>(null)
    private val _route = MutableStateFlow<List<TrackPoint>>(emptyList())
    private val _imagePath = MutableStateFlow<String?>(null)
    private val _selectedImage = MutableStateFlow<String?>(null)
    private val _locationName = MutableStateFlow("")
    private val _visibility = MutableStateFlow(WorkoutVisibility.PRIVATE)
    private val _actionState = MutableStateFlow(SummaryActionState())
    private val _cancelError = MutableStateFlow<String?>(null)

    val workout: StateFlow<Workout?> = _workout.asStateFlow()
    val route: StateFlow<List<TrackPoint>> = _route.asStateFlow()
    val imagePath: StateFlow<String?> = _imagePath.asStateFlow()
    val selectedImage: StateFlow<String?> = _selectedImage.asStateFlow()
    val locationName: StateFlow<String> = _locationName.asStateFlow()
    val visibility: StateFlow<WorkoutVisibility> = _visibility.asStateFlow()
    val actionState: StateFlow<SummaryActionState> = _actionState.asStateFlow()
    val cancelError: StateFlow<String?> = _cancelError.asStateFlow()
    val distanceUnit: StateFlow<DistanceUnit> = settingsRepository.settings
        .map { it.distanceUnit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DistanceUnit.KILOMETERS)

    fun load(workoutId: String) {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkout(workoutId) ?: return@launch
            val route = workoutRepository.getRoute(workoutId)
            _workout.value = workout
            _route.value = route
            _imagePath.value = routeImageGenerator.generate(
                getApplication(),
                route.map { it.latitude to it.longitude }
            )
            _selectedImage.value = _imagePath.value
            _locationName.value = route.firstOrNull()
                ?.let { withContext(Dispatchers.IO) { resolveLocationName(it.latitude, it.longitude) } }
                .orEmpty()
            postRepository.getPostForWorkout(workoutId)?.let {
                _visibility.value = it.visibility
            }
        }
    }

    fun selectImage(uri: Uri?) {
        if (uri == null) {
            _selectedImage.value = _imagePath.value
            return
        }
        viewModelScope.launch {
            _selectedImage.value = withContext(Dispatchers.IO) { copyImage(uri) } ?: _imagePath.value
        }
    }

    private fun resolveLocationName(latitude: Double, longitude: Double): String {
        return runCatching {
            Geocoder(getApplication()).getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.let { address ->
                    listOfNotNull(address.subAdminArea, address.adminArea, address.countryName)
                        .distinct()
                        .joinToString(", ")
                }
        }.getOrNull() ?: String.format("%.4f, %.4f", latitude, longitude)
    }

    private fun copyImage(uri: Uri): String? = runCatching {
        val resolver = getApplication<Application>().contentResolver
        val sourceName = java.io.File(queryName(uri) ?: "shared.jpg").name
        val extension = sourceName.substringAfterLast('.', "jpg")
            .filter(Char::isLetterOrDigit)
            .take(8)
            .ifBlank { "jpg" }
        val file = java.io.File(getApplication<Application>().filesDir, "posts/${UUID.randomUUID()}.$extension")
        file.parentFile?.mkdirs()
        resolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        file.absolutePath
    }.getOrNull()

    private fun queryName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return null
    }

    fun setVisibility(value: WorkoutVisibility) {
        _visibility.value = value
    }

    fun save(title: String, message: String) {
        val workout = _workout.value ?: return
        if (_actionState.value.isSaving || _actionState.value.isCancelling) return
        _actionState.value = SummaryActionState(isSaving = true)
        viewModelScope.launch {
            var postId: String? = null
            try {
                workoutRepository.updateShareInfo(workout.id, title, _visibility.value)
                val post = if (_selectedImage.value != null || message.isNotBlank()) {
                    postRepository.createPost(
                        workoutId = workout.id,
                        caption = message,
                        imageUrl = _selectedImage.value,
                        visibility = _visibility.value
                    ).also { postId = it.id }
                } else null

                val userId = (authRepository.state.value as? AuthState.LoggedIn)?.user?.id
                if (userId == null) {
                    _actionState.value = SummaryActionState(result = SummarySaveResult.LocalOnly)
                    return@launch
                }

                _actionState.value = _actionState.value.copy(isSaving = true)
                workoutRepository.claimForUser(workout.id, userId)
                post?.let { postRepository.claimForUser(it.id, userId) }
                val ownedWorkout = workoutRepository.getWorkout(workout.id)
                    ?: error("Không tìm thấy hoạt động vừa lưu.")
                syncRepository.syncWorkout(ownedWorkout, workoutRepository.getRoute(workout.id), userId)
                post?.let { syncRepository.syncPost(it.copy(authorUserId = userId), userId) }
                workoutRepository.updateSyncStatus(workout.id, SyncStatus.SYNCED)
                post?.let { postRepository.updateSyncStatus(it.id, SyncStatus.SYNCED) }
                _actionState.value = SummaryActionState(result = SummarySaveResult.Synced)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                runCatching { workoutRepository.updateSyncStatus(workout.id, SyncStatus.FAILED) }
                postId?.let { id ->
                    runCatching { postRepository.updateSyncStatus(id, SyncStatus.FAILED) }
                }
                val reason = error.localizedMessage
                    ?.takeIf { it.isNotBlank() }
                    ?: "Không thể tải hoạt động lên máy chủ."
                _actionState.value = SummaryActionState(result = SummarySaveResult.Failed(reason))
            }
        }
    }

    fun cancel(onCancelled: () -> Unit) {
        val workoutId = _workout.value?.id ?: return
        if (_actionState.value.isSaving || _actionState.value.isCancelling) return
        _cancelError.value = null
        _actionState.value = _actionState.value.copy(isCancelling = true)
        viewModelScope.launch {
            try {
                val postImage = workoutRepository.discardWorkout(workoutId)
                listOfNotNull(postImage, _selectedImage.value, _imagePath.value)
                    .distinct()
                    .forEach(::deleteAppFile)
                onCancelled()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _cancelError.value = error.localizedMessage ?: "Không thể huỷ hoạt động."
            } finally {
                _actionState.value = _actionState.value.copy(isCancelling = false)
            }
        }
    }

    fun dismissCancelError() {
        _cancelError.value = null
    }

    private fun deleteAppFile(path: String) {
        val appFiles = getApplication<Application>().filesDir.canonicalFile
        val file = java.io.File(path).canonicalFile
        if (file.path.startsWith(appFiles.path + java.io.File.separator)) file.delete()
    }
}
