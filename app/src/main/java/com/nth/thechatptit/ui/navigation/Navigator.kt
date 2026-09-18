package com.nth.thechatptit.ui.navigation

import android.util.Log
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@ActivityRetainedScoped
class Navigator @Inject constructor() {
    private val _navigate =
        MutableSharedFlow<NavTarget>(extraBufferCapacity = 1)

    private val _back =
        MutableSharedFlow<NavBack>(extraBufferCapacity = 1)

    private val _end =
        MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    val navigate = _navigate.asSharedFlow()
    val back = _back.asSharedFlow()
    val end = _end.asSharedFlow()

    suspend fun navigateTo(route: String, popBackstack: Boolean = false) {
        //val test = _navigate.tryEmit(NavTarget(route, popBackstack))
        //Log.d("Navigator", "navigateTo ${route} -=> ${test}")

        _navigate.subscriptionCount
            .first { subscriberCount ->
                subscriberCount > 0
            }

        _navigate.emit(
            NavTarget(route, popBackstack)
        )
    }

    suspend fun navigateBack(recreate: Boolean = false) {
        _back.subscriptionCount.first { c -> c > 0 }
        _back.tryEmit(NavBack(recreate))
    }

    suspend fun finish() {
        _end.subscriptionCount.first { c -> c > 0 }
        _end.tryEmit(true)
    }

    data class NavTarget(val route: String, val popBackstack: Boolean = false)
    data class NavBack(val recreate: Boolean)


}