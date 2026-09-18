package com.nth.thechatptit.ui

import androidx.lifecycle.viewModelScope
import com.nth.thechatptit.ui.base.BaseViewModel
import com.nth.thechatptit.ui.common.snackbar.Messenger
import com.nth.thechatptit.ui.navigation.Navigator
import com.nth.thechatptit.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val messenger: Messenger,
    val navigator: Navigator
)  : BaseViewModel() {
    init {

    }
}