package com.nth.thechatptit.ui.splash

import com.nth.thechatptit.data.auth.AuthRepository
import com.nth.thechatptit.data.model.AuthState
import com.nth.thechatptit.data.sync.SyncScheduler
import androidx.lifecycle.viewModelScope
import com.nth.thechatptit.ui.base.BaseViewModel
import com.nth.thechatptit.ui.common.snackbar.Messenger
import com.nth.thechatptit.ui.navigation.Navigator
import com.nth.thechatptit.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val navigator: Navigator,
    val messenger: Messenger,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler
) : BaseViewModel() {
    companion object {
        const val TAG = "SplashViewModel"
    }

    init {
        viewModelScope.launch {
            authRepository.restore()
            val route = when (val state = authRepository.state.value) {
                is AuthState.LoggedIn -> {
                    syncScheduler.enqueueAccountSync(state.user.id)
                    Routes.Home.route
                }
                AuthState.LoggedOut -> if (authRepository.isConfigured) {
                    Routes.Login.route
                } else {
                    Routes.Home.route
                }
                AuthState.Restoring -> Routes.Home.route
            }
            navigator.navigateTo(route, true)
        }
    }
}
