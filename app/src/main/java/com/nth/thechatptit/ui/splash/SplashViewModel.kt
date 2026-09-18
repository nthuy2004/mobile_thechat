package com.nth.thechatptit.ui.splash

import android.util.Log
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
    val messenger: Messenger
) : BaseViewModel() {
    companion object {
        const val TAG = "SplashViewModel"
    }

    init {
        viewModelScope.launch {
            Log.d(TAG, "Hello world")
            navigator.navigateTo(Routes.Home.route, true)
        }
    }
}