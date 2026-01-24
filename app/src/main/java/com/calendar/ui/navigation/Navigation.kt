package com.calendar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.calendar.ui.screen.CalendarScreen
import com.calendar.ui.screen.SettingsScreen
import com.calendar.ui.viewmodel.CalendarViewModel

sealed class Screen(val route: String) {
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: CalendarViewModel,
    onExit: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Calendar.route
    ) {
        composable(Screen.Calendar.route) {
            CalendarScreen(
                viewModel = viewModel,
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onExit = onExit
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
