package com.sih26001.mobilealert.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sih26001.mobilealert.core.util.Constants
import com.sih26001.mobilealert.presentation.alerts.AlertsScreen
import com.sih26001.mobilealert.presentation.alerts.AlertsViewModel
import com.sih26001.mobilealert.presentation.history.HistoryScreen
import com.sih26001.mobilealert.presentation.history.HistoryViewModel
import com.sih26001.mobilealert.presentation.home.HomeScreen
import com.sih26001.mobilealert.presentation.home.HomeViewModel

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
                }
            )
        }

        composable(route = Constants.ROUTE_ALERTS) {
            val alertsViewModel: AlertsViewModel = viewModel()
            AlertsScreen(
                viewModel = alertsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
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
    }
}
