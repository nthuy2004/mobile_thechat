package com.nth.thechatptit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nth.thechatptit.ui.splash.SplashViewModel

import androidx.hilt.navigation.compose.hiltViewModel
import com.nth.thechatptit.ui.home.Home
import com.nth.thechatptit.ui.home.HomeViewModel
import com.nth.thechatptit.ui.splash.Splash

@Composable
internal fun NavGraph(modifier: Modifier = Modifier,
                      navController: NavHostController,
                      startDestination: String = Routes.Splash.route,
                      navigator: Navigator,
                      finish: () -> Unit = {}) {
    NavHandler(navController, navigator, finish)

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Splash.route) {
            val viewModel: SplashViewModel = hiltViewModel(key = SplashViewModel.TAG)
            Splash(modifier, viewModel)
        }
        composable(Routes.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel(key = HomeViewModel.TAG)
            Home(modifier, viewModel)
        }
    }
}