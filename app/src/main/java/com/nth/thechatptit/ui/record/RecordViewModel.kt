package com.nth.thechatptit.ui.record

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nth.thechatptit.data.model.TrackingState
import com.nth.thechatptit.data.model.TrackingStatus
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.data.tracking.TrackingService
import com.nth.thechatptit.data.tracking.TrackingRepository
import com.nth.thechatptit.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    application: Application,
    private val repository: TrackingRepository,
    settingsRepository: SettingsRepository
) : AndroidViewModel(application) {
    val state: StateFlow<TrackingState> = repository.state
    val route = repository.route
    val defaultWorkoutType: StateFlow<WorkoutType> = settingsRepository.settings
        .map { it.defaultWorkoutType }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), WorkoutType.RUNNING)
    val distanceUnit: StateFlow<DistanceUnit> = settingsRepository.settings
        .map { it.distanceUnit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DistanceUnit.KILOMETERS)

    private val _isStopping = MutableStateFlow(false)
    val isStopping: StateFlow<Boolean> = _isStopping.asStateFlow()
    private val _stopError = MutableStateFlow<String?>(null)
    val stopError: StateFlow<String?> = _stopError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.restore()
            if (repository.state.value.status != com.nth.thechatptit.data.model.TrackingStatus.IDLE) {
                ContextCompat.startForegroundService(
                    getApplication(),
                    Intent(getApplication(), TrackingService::class.java)
                )
            }
        }
    }

    fun start(type: WorkoutType) = send(TrackingService.ACTION_START, type)
    fun pause() = send(TrackingService.ACTION_PAUSE)
    fun resume() = send(TrackingService.ACTION_RESUME)
    fun refreshLocation() = send(TrackingService.ACTION_RETRY_LOCATION)
    fun stop() = send(TrackingService.ACTION_STOP)

    fun stop(onStopped: (String?) -> Unit) {
        if (_isStopping.value) return
        val workoutId = state.value.workoutId
        if (workoutId == null) {
            _stopError.value = "Không tìm thấy hoạt động đang ghi."
            return
        }
        _stopError.value = null
        _isStopping.value = true
        send(TrackingService.ACTION_STOP)
        viewModelScope.launch {
            try {
                repository.stopAndWait()
                onStopped(workoutId)
            } catch (_: TimeoutCancellationException) {
                _stopError.value = "Chưa thể hoàn tất hoạt động. Hãy thử lại."
            } finally {
                _isStopping.value = false
            }
        }
    }

    fun dismissStopError() {
        _stopError.value = null
    }

    private fun send(action: String, type: WorkoutType? = null) {
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            this.action = action
            type?.let { putExtra(TrackingService.EXTRA_TYPE, it.name) }
        }
        if (action == TrackingService.ACTION_START) {
            ContextCompat.startForegroundService(getApplication(), intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }
}
