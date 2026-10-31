package com.nth.thechatptit.ui.common.bottombar

import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nth.thechatptit.R
import com.nth.thechatptit.ui.navigation.Routes
import com.nth.thechatptit.ui.theme.AppTheme

@Keep
enum class MainTab(
    @StringRes val title: Int,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val route: String,
) {
    HOME(
        R.string.menu_feed,
        Icons.Outlined.Home,
        Icons.Filled.Home,
        Routes.Home.route,
    ),
    RECORD(
        R.string.menu_record,
        Icons.AutoMirrored.Outlined.DirectionsRun,
        Icons.AutoMirrored.Filled.DirectionsRun,
        Routes.Record.route,
    ),
    PROFILE(
        R.string.menu_me,
        Icons.Outlined.Person,
        Icons.Filled.Person,
        Routes.Profile.route
    ),
}

@Composable
fun MainBottomBar(navController: NavController, onTabSelected: (String) -> Unit = {}) {

    val tabs = remember { MainTab.entries.toTypedArray().asList() }
    val routes = remember { MainTab.entries.map { it.route } }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
        ?: Routes.Home.route

    if (currentRoute == Routes.Record.route) return

    MainBottomBarView(
        tabs = tabs,
        routes = routes,
        currentRoute = currentRoute,
        tabClick = {
            if (it.route != currentRoute) {
                navController.navigate(it.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            onTabSelected(it.route)
        }
    )
}

@Composable
private fun MainBottomBarView(
    tabs: List<MainTab>,
    routes: List<String>,
    currentRoute: String,
    tabClick: (MainTab) -> Unit
) {

    if (currentRoute in routes) {
        NavigationBar(
            tonalElevation = 4.dp
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = stringResource(tab.title),
                        )
                    },
                    selected = selected,
                    onClick = { tabClick(tab) },
                    label = { Text(stringResource(tab.title)) },
                    alwaysShowLabel = true,
                )
            }
        }
    }
}

@Preview("Light")
@Composable
private fun HomeBottomBarLightPreview() {
    AppTheme {
        MainBottomBarView(
            tabs = MainTab.entries.toTypedArray().asList(),
            routes = MainTab.entries.map { it.route },
            currentRoute = MainTab.HOME.route,
        ) {}
    }
}

@Preview("Dark")
@Composable
private fun HomeBottomBarDarkPreview() {
    AppTheme {
        MainBottomBarView(
            tabs = MainTab.entries.toTypedArray().asList(),
            routes = MainTab.entries.map { it.route },
            currentRoute = MainTab.HOME.route,
        ) {}
    }
}
