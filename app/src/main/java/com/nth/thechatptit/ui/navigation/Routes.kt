package com.nth.thechatptit.ui.navigation

class Routes {
    abstract class Screen(baseRoute : String) {
        open val route = baseRoute
        open val deepLink = "app://nth/$baseRoute"
    }

    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Record : Screen("record")
    data object Map : Screen("map")
    data object Profile : Screen("profile")
    data object Summary : Screen("summary/{workoutId}") {
        val routePattern = route
        fun createRoute(workoutId: String) = "summary/$workoutId"
    }
    data object WorkoutDetail : Screen("workout/{workoutId}") {
        fun createRoute(workoutId: String) = "workout/$workoutId"
    }
    data object Setting : Screen("setting")
    data object Login : Screen("login")
    data object Register : Screen("register")
}
