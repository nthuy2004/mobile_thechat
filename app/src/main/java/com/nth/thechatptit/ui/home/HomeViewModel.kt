package com.nth.thechatptit.ui.home

import com.nth.thechatptit.ui.base.BaseViewModel
import com.nth.thechatptit.ui.common.snackbar.Messenger
import com.nth.thechatptit.ui.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(val navigator: Navigator,
                            val messenger: Messenger) : BaseViewModel() {
    companion object {
        const val TAG = "HomeViewModel"
    }
}