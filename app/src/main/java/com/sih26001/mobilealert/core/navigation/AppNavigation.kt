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
import com.sih26001.mobilealert.presentation.history.HistoryScreen
import com.sih26001.mobilealert.presentation.history.HistoryViewModel
import com.sih26001.mobilealert.presentation.home.HomeScreen
import com.sih26001.mobilealert.presentation.home.HomeViewModel
import com.sih26001.mobilealert.presentation.route.RouteScreen
import com.sih26001.mobilealert.presentation.safeplace.SafePlaceScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Constants.ROUTE_HOME,
        modifier = modifier
    ) {
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
                onNavigateToAlertDetails = { alertId ->
                    navController.navigate("active_alarm/$alertId")
                },
                onNavigateToSafePlace = { alertId ->
                    navController.navigate("safe_place/$alertId")
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
                    navController.navigate("active_alarm/$alertId")
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
