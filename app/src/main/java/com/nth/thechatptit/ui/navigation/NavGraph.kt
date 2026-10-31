package com.nth.thechatptit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nth.thechatptit.ui.splash.SplashViewModel

import androidx.hilt.navigation.compose.hiltViewModel
import com.nth.thechatptit.ui.home.Home
import com.nth.thechatptit.ui.home.HomeViewModel
import com.nth.thechatptit.ui.splash.Splash
import com.nth.thechatptit.ui.record.Record
import com.nth.thechatptit.ui.record.RecordViewModel
import com.nth.thechatptit.ui.profile.Profile
import com.nth.thechatptit.ui.profile.ProfileViewModel
import com.nth.thechatptit.ui.summary.Summary
import com.nth.thechatptit.ui.summary.SummaryViewModel
import com.nth.thechatptit.ui.workout.WorkoutDetail
import com.nth.thechatptit.ui.workout.WorkoutDetailViewModel
import com.nth.thechatptit.ui.auth.AuthViewModel
import com.nth.thechatptit.ui.auth.LoginScreen
import com.nth.thechatptit.ui.auth.RegisterScreen

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
        composable(Routes.Login.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            LoginScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.Register.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel(key = HomeViewModel.TAG)
            Home(
                modifier = modifier,
                viewModel = viewModel,
                onWorkoutClick = { workoutId ->
                    navController.navigate(Routes.WorkoutDetail.createRoute(workoutId))
                }
            )
        }
        composable(Routes.Record.route) {
            val viewModel: RecordViewModel = hiltViewModel()
            Record(
                modifier,
                viewModel,
                onBack = { navController.popBackStack() },
                onWorkoutFinished = { workoutId ->
                    if (workoutId != null) {
                        navController.navigate("summary/$workoutId")
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(Routes.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            Profile(
                modifier = modifier,
                viewModel = viewModel,
                onWorkoutClick = { workoutId ->
                    navController.navigate(Routes.WorkoutDetail.createRoute(workoutId))
                },
                onLogin = { navController.navigate(Routes.Login.route) }
            )
        }
        composable(
            Routes.Summary.route,
            arguments = listOf(navArgument("workoutId") { defaultValue = "" })
        ) { entry ->
            val workoutId = entry.arguments?.getString("workoutId").orEmpty()
            val viewModel: SummaryViewModel = hiltViewModel()
            Summary(
                workoutId = workoutId,
                onBack = {
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }
        composable(
            Routes.WorkoutDetail.route,
            arguments = listOf(navArgument("workoutId") { defaultValue = "" })
        ) { entry ->
            val workoutId = entry.arguments?.getString("workoutId").orEmpty()
            val viewModel: WorkoutDetailViewModel = hiltViewModel()
            WorkoutDetail(
                modifier = modifier,
                workoutId = workoutId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
