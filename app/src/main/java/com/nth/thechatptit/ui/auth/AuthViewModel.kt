package com.nth.thechatptit.ui.auth

import android.util.Patterns
import com.google.firebase.auth.FirebaseAuthException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nth.thechatptit.data.auth.AuthRepository
import com.nth.thechatptit.data.model.User
import com.nth.thechatptit.data.sync.SyncScheduler
import com.nth.thechatptit.ui.navigation.Navigator
import com.nth.thechatptit.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val navigator: Navigator,
    private val syncScheduler: SyncScheduler
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState(isConfigured = authRepository.isConfigured))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun signIn(email: String, password: String) {
        when {
            !isValidEmail(email) -> _state.value = _state.value.copy(error = "Nhập email hợp lệ.")
            password.isBlank() -> _state.value = _state.value.copy(error = "Nhập mật khẩu.")
            !authRepository.isConfigured -> _state.value = _state.value.copy(
                error = "Firebase chưa được cấu hình cho ứng dụng này."
            )
            else -> viewModelScope.launch {
                _state.value = _state.value.copy(isLoading = true, error = null)
                authRepository.signIn(email, password)
                    .onSuccess { user -> onSuccess(user) }
                    .onFailure { error ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = authError(error, registering = false)
                        )
                    }
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        when {
            displayName.isBlank() -> _state.value = _state.value.copy(error = "Nhập tên hiển thị.")
            !isValidEmail(email) -> _state.value = _state.value.copy(error = "Nhập email hợp lệ.")
            password.length < 6 -> _state.value = _state.value.copy(
                error = "Mật khẩu cần có ít nhất 6 ký tự."
            )
            !authRepository.isConfigured -> _state.value = _state.value.copy(
                error = "Firebase chưa được cấu hình cho ứng dụng này."
            )
            else -> viewModelScope.launch {
                _state.value = _state.value.copy(isLoading = true, error = null)
                authRepository.register(email, password, displayName)
                    .onSuccess { user -> onSuccess(user) }
                    .onFailure { error ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = authError(error, registering = true)
                        )
                    }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        when {
            !isValidEmail(email) -> _state.value = _state.value.copy(error = "Nhập email hợp lệ.", info = null)
            !authRepository.isConfigured -> _state.value = _state.value.copy(
                error = "Firebase chưa được cấu hình cho ứng dụng này.",
                info = null
            )
            else -> viewModelScope.launch {
                _state.value = _state.value.copy(isLoading = true, error = null, info = null)
                authRepository.sendPasswordReset(email)
                    .onSuccess {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            info = "Đã gửi hướng dẫn đặt lại mật khẩu đến email của bạn."
                        )
                    }
                    .onFailure { error ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = authError(error, registering = false)
                        )
                    }
            }
        }
    }

    fun continueOffline() {
        viewModelScope.launch { navigator.navigateTo(Routes.Home.route, true) }
    }

    fun openRegister() {
        viewModelScope.launch { navigator.navigateTo(Routes.Register.route) }
    }

    fun openLogin() {
        viewModelScope.launch { navigator.navigateTo(Routes.Login.route) }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null, info = null)
    }

    private suspend fun onSuccess(user: User) {
        _state.value = _state.value.copy(isLoading = false, user = user, error = null)
        syncScheduler.enqueueAccountSync(user.id)
        navigator.navigateTo(Routes.Home.route, true)
    }

    private fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    private fun authError(error: Throwable, registering: Boolean): String = when (
        (error as? FirebaseAuthException)?.errorCode
    ) {
        "ERROR_INVALID_EMAIL" -> "Email không hợp lệ."
        "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
            "Email hoặc mật khẩu không đúng."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Email này đã được đăng ký."
        "ERROR_WEAK_PASSWORD" -> "Mật khẩu quá yếu, hãy chọn mật khẩu khác."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Không có kết nối mạng. Thử lại khi có mạng."
        "ERROR_TOO_MANY_REQUESTS" -> "Bạn thử quá nhiều lần. Hãy đợi rồi thử lại."
        else -> if (registering) "Không thể tạo tài khoản. Hãy thử lại." else "Không thể đăng nhập. Hãy thử lại."
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isConfigured: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val info: String? = null
)
