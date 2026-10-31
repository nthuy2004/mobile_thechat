package com.nth.thechatptit.ui.home

import androidx.lifecycle.viewModelScope
import com.nth.thechatptit.data.model.Post
import com.nth.thechatptit.data.model.Workout
import com.nth.thechatptit.data.model.WorkoutStatistics
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.data.auth.AuthRepository
import com.nth.thechatptit.data.model.AuthState
import com.nth.thechatptit.data.repo.PostRepository
import com.nth.thechatptit.data.repo.WorkoutRepository
import com.nth.thechatptit.data.settings.SettingsRepository
import com.nth.thechatptit.data.sync.SyncScheduler
import com.nth.thechatptit.ui.base.BaseViewModel
import com.nth.thechatptit.ui.common.snackbar.Messenger
import com.nth.thechatptit.ui.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(val navigator: Navigator,
                            val messenger: Messenger,
                            private val workoutRepository: WorkoutRepository,
                            private val postRepository: PostRepository,
                            settingsRepository: SettingsRepository,
                            private val authRepository: AuthRepository,
                            private val syncScheduler: SyncScheduler) : BaseViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    val distanceUnit: StateFlow<DistanceUnit> = settingsRepository.settings
        .map { it.distanceUnit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DistanceUnit.KILOMETERS)
    val displayName: StateFlow<String?> = authRepository.state
        .map { state ->
            (state as? AuthState.LoggedIn)?.user?.displayName?.takeIf { it.isNotBlank() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    companion object {
        const val TAG = "HomeViewModel"
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val now = System.currentTimeMillis()
            val start = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = now + 1L
            val userId = (authRepository.state.value as? AuthState.LoggedIn)?.user?.id
            if (userId != null) syncScheduler.enqueueAccountSync(userId)
            val statistics = workoutRepository.getStatistics(start, end, userId = userId)
            val posts = postRepository.getRecentPosts(userId).take(10).mapNotNull { post ->
                workoutRepository.getWorkout(post.workoutId)?.let { workout ->
                    HomePost(post, workout)
                }
            }
            _state.value = HomeUiState(
                isLoading = false,
                statistics = statistics,
                posts = posts
            )
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val statistics: WorkoutStatistics? = null,
    val posts: List<HomePost> = emptyList()
)

data class HomePost(
    val post: Post,
    val workout: Workout
)
