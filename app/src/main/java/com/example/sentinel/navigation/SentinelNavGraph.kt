package com.example.sentinel.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sentinel.SentinelScreen
import com.example.sentinel.ui.screens.*
import com.example.sentinel.viewmodel.*

@Composable
fun SentinelNavGraph(
    navController: NavHostController,
    startDestination: String = SentinelScreen.Splash.route,
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    deviceViewModel: DeviceViewModel,
    emergencyViewModel: EmergencyViewModel,
    incidentViewModel: IncidentViewModel,
    contactsViewModel: ContactsViewModel,
    navigationViewModel: NavigationViewModel,
    contentPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(SentinelScreen.Splash.route) {
            SplashScreen(onTimeout = {
                navController.navigate(SentinelScreen.Auth.route) {
                    popUpTo(SentinelScreen.Splash.route) {
                        inclusive = true
                    }
                }
            })
        }
        composable(SentinelScreen.Auth.route) {
            AuthScreen(
                viewModel = authViewModel,
                onAuthenticated = {
                    navController.navigate(SentinelScreen.DevicePairing.route)
                }
            )
        }
        composable(SentinelScreen.DevicePairing.route) {
            DevicePairingScreen(
                viewModel = deviceViewModel,
                onPaired = {
                    navController.navigate(SentinelScreen.Home.route) {
                        popUpTo(SentinelScreen.DevicePairing.route) {
                            inclusive = true
                        }
                    }
                },
                onBack = { 
                    authViewModel.logout()
                    navController.popBackStack() 
                }
            )
        }
        composable(SentinelScreen.Home.route) { 
            HomeDashboard(
                viewModel = dashboardViewModel,
                onNavigate = { route -> navController.navigate(route) }
            ) 
        }
        composable(SentinelScreen.Emergency.route) { 
            EmergencyScreen(
                viewModel = emergencyViewModel,
                onBack = { navController.popBackStack() }
            ) 
        }
        composable(SentinelScreen.LiveTracking.route) { 
            LiveTrackingScreen(
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            ) 
        }
        composable(SentinelScreen.SafeRoute.route) { 
            SafeRouteNavigationScreen(
                viewModel = navigationViewModel,
                dashboardViewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            ) 
        }
        composable(SentinelScreen.CommunityRisk.route) { 
            CommunityRiskMapScreen(
                viewModel = dashboardViewModel,
                onReportIncident = { navController.navigate(SentinelScreen.ReportIncident.route) },
                onNavigateToSafeRoute = { navController.navigate(SentinelScreen.SafeRoute.route) }
            ) 
        }
        composable(SentinelScreen.ReportIncident.route) { 
            ReportIncidentScreen(
                viewModel = incidentViewModel,
                onBack = { navController.popBackStack() }
            ) 
        }
        composable(SentinelScreen.ActivityHistory.route) { 
            ActivityHistoryScreen(
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            ) 
        }
        composable(SentinelScreen.EmergencyContacts.route) { 
            EmergencyContactsScreen(
                viewModel = contactsViewModel,
                onBack = { navController.popBackStack() }
            ) 
        }
        composable(SentinelScreen.DeviceStatus.route) { 
            DeviceStatusScreen(
                viewModel = deviceViewModel,
                onBack = { navController.popBackStack() }
            ) 
        }
        composable(SentinelScreen.ProfileSettings.route) { 
            ProfileSettingsScreen(
                authViewModel = authViewModel,
                dashboardViewModel = dashboardViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToContacts = { navController.navigate(SentinelScreen.EmergencyContacts.route) },
                onNavigateToDevice = { navController.navigate(SentinelScreen.DeviceStatus.route) },
                onNavigateToActivity = { navController.navigate(SentinelScreen.ActivityHistory.route) }
            ) 
        }
    }
}
