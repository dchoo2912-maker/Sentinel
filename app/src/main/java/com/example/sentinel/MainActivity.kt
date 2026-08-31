package com.example.sentinel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sentinel.data.LocationService
import com.example.sentinel.navigation.SentinelNavGraph
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import com.example.sentinel.ui.components.BottomNavBar
import com.example.sentinel.ui.theme.SentinelTheme
import com.example.sentinel.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SentinelTheme {
                SentinelApp()
            }
        }
    }
}

@Composable
fun SentinelApp(startDestination: String = SentinelScreen.Splash.route) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    val authViewModel: AuthViewModel = viewModel()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val deviceViewModel: DeviceViewModel = viewModel()
    val emergencyViewModel: EmergencyViewModel = viewModel()
    val incidentViewModel: IncidentViewModel = viewModel()
    val contactsViewModel: ContactsViewModel = viewModel()
    val navigationViewModel: NavigationViewModel = viewModel()

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            val user = (authState as AuthState.Authenticated).user
            dashboardViewModel.setUser(user)
            incidentViewModel.setUser(user.id)
            contactsViewModel.setUser(user.id)
            emergencyViewModel.setUser(user.id)
            
            // Start location updates in a separate coroutine
            launch {
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    locationService.getLocationUpdates().collect { location ->
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        dashboardViewModel.updateLocation(location.latitude, location.longitude)
                        navigationViewModel.setLocation(currentLatLng)
                        
                        // Reverse geocode to get a readable address
                        try {
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val addressName = addresses?.firstOrNull()?.getAddressLine(0) ?: "Current Location"
                            incidentViewModel.updateLocation(location.latitude, location.longitude, addressName)
                        } catch (e: Exception) {
                            incidentViewModel.updateLocation(location.latitude, location.longitude, "GPS Location")
                        }
                    }
                } catch (e: SecurityException) {
                    android.util.Log.w("SentinelApp", "Location permission not granted yet")
                } catch (e: Exception) {
                    android.util.Log.e("SentinelApp", "Error starting location updates", e)
                }
            }
        } else if (authState is AuthState.Idle && currentRoute != SentinelScreen.Splash.route && currentRoute != SentinelScreen.Auth.route) {
            dashboardViewModel.clearUser()
            // Use a safer popUpTo that doesn't rely on Splash being there
            navController.navigate(SentinelScreen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val bottomBarRoutes = listOf<String>(
        SentinelScreen.Home.route,
        SentinelScreen.CommunityRisk.route,
        SentinelScreen.ActivityHistory.route,
        SentinelScreen.ProfileSettings.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route: String ->
                        navController.navigate(route) {
                            popUpTo(SentinelScreen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SentinelNavGraph(
                navController = navController,
                startDestination = startDestination,
                authViewModel = authViewModel,
                dashboardViewModel = dashboardViewModel,
                deviceViewModel = deviceViewModel,
                emergencyViewModel = emergencyViewModel,
                incidentViewModel = incidentViewModel,
                contactsViewModel = contactsViewModel,
                navigationViewModel = navigationViewModel,
                contentPadding = innerPadding
            )
        }
    }
}

@Preview(name = "Splash Screen", showBackground = true, showSystemUi = true)
@Composable
fun SplashPreview() {
    SentinelTheme {
        SentinelApp(SentinelScreen.Splash.route)
    }
}

@Preview(name = "Home Dashboard", showBackground = true, showSystemUi = true)
@Composable
fun HomePreview() {
    SentinelTheme {
        SentinelApp(SentinelScreen.Home.route)
    }
}

@Preview(name = "Emergency SOS", showBackground = true, showSystemUi = true)
@Composable
fun EmergencyPreview() {
    SentinelTheme {
        SentinelApp(SentinelScreen.Emergency.route)
    }
}

@Preview(name = "Risk Map", showBackground = true, showSystemUi = true)
@Composable
fun MapPreview() {
    SentinelTheme {
        SentinelApp(SentinelScreen.CommunityRisk.route)
    }
}

@Preview(name = "Profile & Settings", showBackground = true, showSystemUi = true)
@Composable
fun ProfilePreview() {
    SentinelTheme {
        SentinelApp(SentinelScreen.ProfileSettings.route)
    }
}
