package com.nth.thechatptit.ui.profile

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import com.nth.thechatptit.data.sync.PhpUploadClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.Calendar
import java.util.TimeZone
import androidx.lifecycle.viewModelScope
import com.nth.thechatptit.data.auth.AuthRepository
import com.nth.thechatptit.data.model.AppSettings
import com.nth.thechatptit.data.model.AuthState
import com.nth.thechatptit.data.model.Goal
import com.nth.thechatptit.data.model.GoalMetric
import com.nth.thechatptit.data.model.GoalPeriod
import com.nth.thechatptit.data.model.GoalProgress
import com.nth.thechatptit.data.model.GoalReminder
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.data.model.ThemeMode
import com.nth.thechatptit.data.model.Workout
import com.nth.thechatptit.data.model.WorkoutStatistics
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.data.repo.WorkoutRepository
import com.nth.thechatptit.data.repo.GoalRepository
import com.nth.thechatptit.data.settings.SettingsRepository
import com.nth.thechatptit.data.sync.SyncScheduler
import com.nth.thechatptit.data.sync.GoalReminderScheduler
import com.nth.thechatptit.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    private val uploadClient: PhpUploadClient,
    private val goalRepository: GoalRepository,
    private val goalReminderScheduler: GoalReminderScheduler
) : BaseViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.value = _state.value.copy(settings = settings)
            }
        }
        viewModelScope.launch {
            authRepository.state.collect { authState ->
                _state.value = _state.value.copy(authState = authState, avatarImagePath = null)
                val user = (authState as? AuthState.LoggedIn)?.user
                val avatarUrl = user?.avatarUrl
                if (user != null && uploadClient.isApiFilePath(avatarUrl)) {
                    val cachedPath = runCatching {
                        uploadClient.downloadImage(user.id, avatarUrl!!)
                    }.getOrNull()
                    if ((authRepository.state.value as? AuthState.LoggedIn)?.user?.id == user.id) {
                        _state.value = _state.value.copy(avatarImagePath = cachedPath)
                    }
                }
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val userId = (authRepository.state.value as? AuthState.LoggedIn)?.user?.id
            if (userId != null) syncScheduler.enqueueAccountSync(userId)
            if (userId != null) goalRepository.claimUnownedForUser(userId)
            val workouts = workoutRepository.getCompletedWorkouts(userId)
            val goals = goalRepository.getActiveGoals(userId)
            val progress = goals.associate { goal -> goal.id to goalRepository.getProgress(goal) }
            val total = WorkoutStatistics(
                fromEpochMillis = 0L,
                toEpochMillis = System.currentTimeMillis(),
                workoutCount = workouts.size,
                totalDistanceMeters = workouts.sumOf { it.distanceMeters },
                totalActiveDurationMillis = workouts.sumOf { it.activeDurationMillis }
            )
            _state.value = _state.value.copy(
                isLoading = false,
                statistics = total,
                workouts = workouts,
                goals = goals,
                goalProgress = progress
            )
        }
    }

    fun saveGoal(
        goalId: String?,
        title: String,
        metric: GoalMetric,
        targetValue: Double,
        reminder: GoalReminder?
    ) {
        if (title.isBlank() || targetValue <= 0.0) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = goalId?.let { goalRepository.getGoal(it) }
            val userId = (authRepository.state.value as? AuthState.LoggedIn)?.user?.id
            val goal = Goal(
                id = existing?.id ?: UUID.randomUUID().toString(),
                ownerUserId = userId ?: existing?.ownerUserId,
                title = title.trim(),
                metric = metric,
                workoutType = existing?.workoutType,
                period = GoalPeriod.WEEKLY,
                targetValue = targetValue,
                startDateEpochDay = existing?.startDateEpochDay ?: localEpochDay(now),
                endDateEpochDay = existing?.endDateEpochDay,
                reminder = reminder,
                isActive = true,
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                updatedAtEpochMillis = now
            )
            goalRepository.save(goal)
            if (reminder == null) goalReminderScheduler.cancel(goal.id)
            else goalReminderScheduler.schedule(goal)
            refresh()
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            goalReminderScheduler.cancel(goalId)
            goalRepository.delete(goalId)
            refresh()
        }
    }

    fun setAutoPauseEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPauseEnabled(enabled) }
    }

    fun cycleTheme() {
        val next = when (_state.value.settings.themeMode) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        viewModelScope.launch { settingsRepository.setThemeMode(next) }
    }

    fun toggleDistanceUnit() {
        val next = if (_state.value.settings.distanceUnit == DistanceUnit.KILOMETERS) {
            DistanceUnit.MILES
        } else {
            DistanceUnit.KILOMETERS
        }
        viewModelScope.launch { settingsRepository.setDistanceUnit(next) }
    }

    fun toggleDefaultWorkoutType() {
        val next = if (_state.value.settings.defaultWorkoutType == WorkoutType.RUNNING) {
            WorkoutType.CYCLING
        } else {
            WorkoutType.RUNNING
        }
        viewModelScope.launch { settingsRepository.setDefaultWorkoutType(next) }
    }

    fun logout() {
        authRepository.signOut()
    }

    fun updateAvatar(uri: Uri) {
        val user = (authRepository.state.value as? AuthState.LoggedIn)?.user ?: return
        viewModelScope.launch {
            val previousAvatarPath = _state.value.avatarImagePath
            _state.value = _state.value.copy(isSavingProfile = true, accountError = null)
            val localFile = withContext(Dispatchers.IO) { copyAvatar(uri) }
            if (localFile == null) {
                _state.value = _state.value.copy(
                    isSavingProfile = false,
                    accountError = "Không thể đọc ảnh đã chọn."
                )
                return@launch
            }

            _state.value = _state.value.copy(avatarImagePath = localFile.absolutePath)
            val result = runCatching {
                val filePath = uploadClient.uploadFile(user.id, "avatar", localFile)
                authRepository.updateProfile(user.displayName, uploadClient.absoluteUrl(filePath))
                    .getOrThrow()
                filePath
            }
            val filePath = result.getOrNull()
            if (filePath == null) {
                _state.value = _state.value.copy(
                    isSavingProfile = false,
                    accountError = result.exceptionOrNull()?.message ?: "Không thể cập nhật ảnh đại diện.",
                    avatarImagePath = previousAvatarPath
                )
                localFile.delete()
            } else {
                val cachedPath = runCatching { uploadClient.downloadImage(user.id, filePath) }.getOrNull()
                _state.value = _state.value.copy(
                    isSavingProfile = false,
                    accountError = null,
                    avatarImagePath = cachedPath ?: localFile.absolutePath
                )
                if (cachedPath != null) localFile.delete()
            }
        }
    }

    fun updateDisplayName(name: String, onComplete: (Boolean) -> Unit) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(accountError = "Nhập tên hiển thị.")
            onComplete(false)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingProfile = true, accountError = null)
            authRepository.updateProfile(name.trim())
                .onSuccess {
                    _state.value = _state.value.copy(isSavingProfile = false, accountError = null)
                    onComplete(true)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSavingProfile = false,
                        accountError = error.message ?: "Không thể cập nhật hồ sơ."
                    )
                    onComplete(false)
                }
        }
    }

    fun clearAccountError() {
        _state.value = _state.value.copy(accountError = null)
    }

    private fun copyAvatar(uri: Uri): File? = runCatching {
        val directory = File(context.cacheDir, "pending_avatars").apply { mkdirs() }
        val output = File(directory, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            output.outputStream().use { stream -> input.copyTo(stream) }
        } ?: return null
        output
    }.getOrNull()

    private fun localEpochDay(nowEpochMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = nowEpochMillis }
        val utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        return utcDate.timeInMillis / 86_400_000L
    }

}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val statistics: WorkoutStatistics? = null,
    val workouts: List<Workout> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val goalProgress: Map<String, GoalProgress> = emptyMap(),
    val settings: AppSettings = AppSettings(),
    val authState: AuthState = AuthState.LoggedOut,
    val isSavingProfile: Boolean = false,
    val accountError: String? = null,
    val avatarImagePath: String? = null
)
