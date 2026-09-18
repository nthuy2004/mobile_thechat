package com.nth.thechatptit.ui.common.bottombar

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nth.thechatptit.R
import com.nth.thechatptit.ui.navigation.Routes
import com.nth.thechatptit.ui.theme.AppTheme

@Keep
enum class MainTab(
    @StringRes val title: Int,
    @DrawableRes val unselectedIcon: Int,
    @DrawableRes val selectedIcon: Int,
    val route: String,
) {
    HOME(
        R.string.menu_feed,
        R.drawable.ic_feed,
        R.drawable.ic_feed,
        Routes.Home.route,
    ),
    RECORD(
        R.string.menu_record,
        R.drawable.ic_feed,
        R.drawable.ic_feed,
        Routes.Map.route,
    ),
    PROFILE(
        R.string.menu_me,
        R.drawable.ic_feed,
        R.drawable.ic_feed,
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
            Modifier
                .windowInsetsBottomHeight(
                    WindowInsets.navigationBars.add(WindowInsets(bottom = 56.dp))
                ),
            tonalElevation = 4.dp
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    icon = {
                        Icon(
                            painterResource(if (selected) tab.selectedIcon else tab.unselectedIcon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    selected = selected,
                    onClick = { tabClick(tab) },
                    alwaysShowLabel = false,
                    modifier = Modifier.navigationBarsPadding(),
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
    AppTheme(dark = true) {
        MainBottomBarView(
            tabs = MainTab.entries.toTypedArray().asList(),
            routes = MainTab.entries.map { it.route },
            currentRoute = MainTab.HOME.route,
        ) {}
    }
}