package com.nth.thechatptit.ui

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.nth.thechatptit.ui.common.bottombar.MainBottomBar
import com.nth.thechatptit.ui.common.snackbar.AppSnackbar
import com.nth.thechatptit.ui.common.snackbar.Messenger
import com.nth.thechatptit.data.model.TrackingStatus
import com.nth.thechatptit.data.tracking.TrackingRepository
import com.nth.thechatptit.data.tracking.TrackingService
import com.nth.thechatptit.data.settings.SettingsRepository
import com.nth.thechatptit.data.model.AppSettings
import com.nth.thechatptit.ui.navigation.NavGraph
import com.nth.thechatptit.ui.navigation.Navigator
import com.nth.thechatptit.ui.navigation.Routes
import com.nth.thechatptit.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject lateinit var trackingRepository: TrackingRepository
    @javax.inject.Inject lateinit var settingsRepository: SettingsRepository

    val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            trackingRepository.restore()
            if (trackingRepository.state.value.status != TrackingStatus.IDLE) {
                ContextCompat.startForegroundService(
                    this@MainActivity,
                    Intent(this@MainActivity, TrackingService::class.java)
                )
            }
        }
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            Main(
                navigator = viewModel.navigator,
                messenger = viewModel.messenger,
                settings = settings,
                finish = { finish() }
            )
        }
    }
}

@Composable
fun Main(
    navigator: Navigator,
    messenger: Messenger,
    settings: AppSettings = AppSettings(),
    finish: () -> Unit
) {
    val navController = rememberNavController()

    val snackbarHostState = remember { SnackbarHostState() }

    AppTheme(themeMode = settings.themeMode) {
        Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = { MainBottomBar(navController = navController) }, snackbarHost = { AppSnackbar(snackbarHostState, messenger) }) { innerPaddingModifier ->
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPaddingModifier),
                navigator = navigator,
                finish = finish
            )
        }
    }
}
