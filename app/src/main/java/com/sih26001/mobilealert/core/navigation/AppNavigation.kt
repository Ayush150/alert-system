package com.sih26001.mobilealert.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.sih26001.mobilealert.core.util.Constants
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.presentation.activealarm.ActiveAlarmScreen
import com.sih26001.mobilealert.presentation.activealarm.ActiveAlarmViewModel
import com.sih26001.mobilealert.presentation.alerts.AlertsScreen
import com.sih26001.mobilealert.presentation.alerts.AlertsViewModel
import com.sih26001.mobilealert.presentation.emergency.EmergencyAlertScreen
import com.sih26001.mobilealert.presentation.emergency.EmergencyAlertViewModel
import com.sih26001.mobilealert.presentation.history.HistoryScreen
import com.sih26001.mobilealert.presentation.history.HistoryViewModel
import com.sih26001.mobilealert.presentation.home.HomeScreen
import com.sih26001.mobilealert.presentation.home.HomeViewModel
import com.sih26001.mobilealert.presentation.roleselection.RoleSelectionScreen
import com.sih26001.mobilealert.presentation.roleselection.RoleSelectionViewModel
import com.sih26001.mobilealert.presentation.route.RouteScreen
import com.sih26001.mobilealert.presentation.safeplace.SafePlaceScreen
import com.sih26001.mobilealert.presentation.settings.SettingsScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    // Determine start destination based on persisted role
    val startDestination = if (DependencyContainer.rolePreferences.hasSelectedRole()) {
        Constants.ROUTE_HOME
    } else {
        Constants.ROUTE_ROLE_SELECTION
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // --- Role Selection (first launch only) ---
        composable(route = Constants.ROUTE_ROLE_SELECTION) {
            val roleSelectionViewModel: RoleSelectionViewModel = viewModel()
            RoleSelectionScreen(
                viewModel = roleSelectionViewModel,
                onRoleConfirmed = {
                    navController.navigate(Constants.ROUTE_HOME) {
                        popUpTo(Constants.ROUTE_ROLE_SELECTION) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Constants.ROUTE_HOME) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToAlerts = {
                    navController.navigate(Constants.ROUTE_ALERTS)
                },
                onNavigateToHistory = {
                    navController.navigate(Constants.ROUTE_HISTORY)
                },
                onNavigateToSettings = {
                    navController.navigate(Constants.ROUTE_SETTINGS)
                },
                onNavigateToAlertDetails = { alertId ->
                    navController.navigate("emergency_alert/$alertId")
                },
                onNavigateToSafePlace = { alertId ->
                    navController.navigate("safe_place/$alertId")
                },
                onChangeRole = {
                    DependencyContainer.rolePreferences.clearRole()
                    navController.navigate(Constants.ROUTE_ROLE_SELECTION) {
                        popUpTo(Constants.ROUTE_HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Constants.ROUTE_SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onChangeRole = {
                    DependencyContainer.rolePreferences.clearRole()
                    navController.navigate(Constants.ROUTE_ROLE_SELECTION) {
                        popUpTo(Constants.ROUTE_HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Constants.ROUTE_ALERTS) {
            val alertsViewModel: AlertsViewModel = viewModel()
            AlertsScreen(
                viewModel = alertsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAlertDetails = { alertId ->
                    navController.navigate("emergency_alert/$alertId")
                }
            )
        }

        composable(route = Constants.ROUTE_HISTORY) {
            val historyViewModel: HistoryViewModel = viewModel()
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Dedicated Full-Screen Emergency Screen Route
        composable(
            route = "emergency_alert/{alertId}",
            arguments = listOf(navArgument("alertId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "sih26001://emergency/{alertId}" })
        ) { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
            val emergencyViewModel = viewModel {
                EmergencyAlertViewModel(alertId = alertId)
            }
            EmergencyAlertScreen(
                viewModel = emergencyViewModel,
                onNavigateToRoute = { id ->
                    navController.navigate("route/$id")
                }
            )
        }

        composable(
            route = "active_alarm/{alertId}",
            arguments = listOf(navArgument("alertId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "sih26001://alert/{alertId}" })
        ) { backStackEntry ->
            val activeAlarmViewModel = viewModel {
                ActiveAlarmViewModel(
                    savedStateHandle = backStackEntry.savedStateHandle
                )
            }
            ActiveAlarmScreen(
                viewModel = activeAlarmViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSafePlace = { alertId ->
                    navController.navigate("safe_place/$alertId")
                }
            )
        }

        composable(
            route = "safe_place/{alertId}",
            arguments = listOf(navArgument("alertId") { type = NavType.StringType })
        ) { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
            SafePlaceScreen(
                alertId = alertId,
                alertRepository = DependencyContainer.alertRepository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRoute = { id ->
                    navController.navigate("route/$id")
                }
            )
        }

        composable(
            route = "route/{alertId}",
            arguments = listOf(navArgument("alertId") { type = NavType.StringType })
        ) { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
            RouteScreen(
                alertId = alertId,
                alertRepository = DependencyContainer.alertRepository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
