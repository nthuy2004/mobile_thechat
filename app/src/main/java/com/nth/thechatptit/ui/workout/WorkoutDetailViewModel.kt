package com.nth.thechatptit.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nth.thechatptit.data.model.Post
import com.nth.thechatptit.data.model.TrackPoint
import com.nth.thechatptit.data.model.Workout
import com.nth.thechatptit.data.model.WorkoutSegment
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.data.repo.PostRepository
import com.nth.thechatptit.data.repo.WorkoutRepository
import com.nth.thechatptit.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val postRepository: PostRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(WorkoutDetailUiState())
    val state: StateFlow<WorkoutDetailUiState> = _state.asStateFlow()
    val distanceUnit: StateFlow<DistanceUnit> = settingsRepository.settings
        .map { it.distanceUnit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DistanceUnit.KILOMETERS)

    fun load(workoutId: String) {
        if (_state.value.workout?.id == workoutId && !_state.value.isLoading) return
        viewModelScope.launch {
            _state.value = WorkoutDetailUiState(isLoading = true)
            val workout = workoutRepository.getWorkout(workoutId)
            if (workout == null) {
                _state.value = WorkoutDetailUiState(isLoading = false)
                return@launch
            }
            _state.value = WorkoutDetailUiState(
                isLoading = false,
                workout = workout,
                route = workoutRepository.getRoute(workoutId),
                segments = workoutRepository.getSegments(workoutId),
                post = postRepository.getPostForWorkout(workoutId)
            )
        }
    }
}

data class WorkoutDetailUiState(
    val isLoading: Boolean = true,
    val workout: Workout? = null,
    val route: List<TrackPoint> = emptyList(),
    val segments: List<WorkoutSegment> = emptyList(),
    val post: Post? = null
)
