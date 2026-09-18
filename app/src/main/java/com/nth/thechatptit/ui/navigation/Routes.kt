package com.nth.thechatptit.ui.navigation

class Routes {
    abstract class Screen(baseRoute : String) {
        open val route = baseRoute
        open val deepLink = "app://nth/$baseRoute"
    }

    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Map : Screen("map")
    data object Profile : Screen("profile")
    data object Setting : Screen("setting")
    data object Login : Screen("login")
    data object Register : Screen("register")
}