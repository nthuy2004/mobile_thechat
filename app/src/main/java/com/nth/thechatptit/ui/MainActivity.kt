package com.nth.thechatptit.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nth.thechatptit.ui.common.bottombar.MainBottomBar
import com.nth.thechatptit.ui.common.snackbar.AppSnackbar
import com.nth.thechatptit.ui.common.snackbar.Message
import com.nth.thechatptit.ui.common.snackbar.Messenger
import com.nth.thechatptit.ui.navigation.NavGraph
import com.nth.thechatptit.ui.navigation.Navigator
import com.nth.thechatptit.ui.navigation.Routes
import com.nth.thechatptit.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Main(navigator = viewModel.navigator, messenger = viewModel.messenger, finish = {finish()})
        }
    }
}

@Composable
fun Main(navigator: Navigator, messenger: Messenger, finish: () -> Unit) {
    val navController = rememberNavController()

    val snackbarHostState = remember { SnackbarHostState() }

    fun onTabSelected(route : String) {
        messenger.show(Message.Type.SUCCESS, "Redirect to ${route}")
    }

    AppTheme {
        Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = { MainBottomBar(navController = navController) {str -> onTabSelected(str)} }, snackbarHost = { AppSnackbar(snackbarHostState, messenger) },) { innerPaddingModifier ->
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPaddingModifier),
                navigator = navigator,
                finish = finish
            )
        }
    }
}