package com.example.sentinel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.domain.RiskLevel
import com.example.sentinel.viewmodel.DashboardViewModel
import com.example.sentinel.viewmodel.NavigationViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@Composable
fun SafeRouteNavigationScreen(
    viewModel: NavigationViewModel,
    dashboardViewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }

    val blue600 = Color(0xFF2563EB)
    val green600 = Color(0xFF16A34A)
    val red500 = Color(0xFFEF4444)
    val slate400 = Color(0xFF94A3B8)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(dashboardState.currentLocation, 15f)
    }

    // Update camera when route is found
    LaunchedEffect(uiState.destination) {
        uiState.destination?.let { dest ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(dest, 14f))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            uiState.origin?.let {
                Marker(
                    state = rememberMarkerState(position = it),
                    title = "Start Point",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }

            uiState.routes.getOrNull(uiState.selectedRouteIndex)?.let { route ->
                Polyline(
                    points = route.points,
                    color = Color(RiskLevel.fromScore(route.safetyScore).color),
                    width = 10f
                )
                
                Marker(
                    state = rememberMarkerState(position = route.points.last()),
                    title = uiState.destinationName
                )
                
                // Adjust camera to show entire route
                val bounds = remember(route.points) {
                    val b = LatLngBounds.Builder()
                    route.points.forEach { b.include(it) }
                    b.build()
                }
                LaunchedEffect(bounds) {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                }
            }

            // Also show risk zones
            dashboardState.incidents.forEach { incident ->
                Circle(
                    center = LatLng(incident.latitude, incident.longitude),
                    radius = 200.0,
                    fillColor = red500.copy(alpha = 0.3f),
                    strokeColor = red500,
                    strokeWidth = 2f
                )
            }
        }

        // Header with Search
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.background(Color.White, CircleShape).size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                    }
                    
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Where to?", fontSize = 16.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                            viewModel.findSafeRoute(
                                context,
                                searchQuery, 
                                "AIzaSyCR1RVrLJCAH7DOPTw6qHVddHCu1-iTJ3M",
                                dashboardState.incidents
                            )
                        }),
                        trailingIcon = {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = {
                                    focusManager.clearFocus()
                                    viewModel.findSafeRoute(
                                        context,
                                        searchQuery, 
                                        "AIzaSyCR1RVrLJCAH7DOPTw6qHVddHCu1-iTJ3M",
                                        dashboardState.incidents
                                    )
                                }) {
                                    Icon(Icons.Default.Search, null, tint = slate400)
                                }
                            }
                        }
                    )
                }
            }
            
            if (uiState.error != null) {
                Surface(
                    color = Color.Red.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        uiState.error!!, 
                        color = Color.White, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Bottom Sheet for Route Info
        if (uiState.routes.isNotEmpty()) {
            val currentRoute = uiState.routes[uiState.selectedRouteIndex]
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.navigationBarsPadding().padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = Color(0xFFDCFCE7)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Navigation, contentDescription = null, tint = green600)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                Column {
                    val routeRisk = remember(currentRoute.safetyScore) { RiskLevel.fromScore(currentRoute.safetyScore) }
                    Text(routeRisk.label + " ROUTE", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(routeRisk.color))
                    Text("${currentRoute.distance} · ${currentRoute.duration}", color = slate400, fontSize = 14.sp)
                }
                Spacer(Modifier.weight(1f))
                val routeRiskIcon = remember(currentRoute.safetyScore) { RiskLevel.fromScore(currentRoute.safetyScore) }
                Surface(color = Color(routeRiskIcon.color).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = Color(routeRiskIcon.color), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(routeRiskIcon.label, color = Color(routeRiskIcon.color), fontWeight = FontWeight.Bold)
                    }
                }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        "This route avoids ${currentRoute.riskCount} reported risk zones.",
                        color = if (currentRoute.riskCount == 0) green600 else Color.Gray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = blue600),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start Safe Navigation", fontSize = 16.sp)
                    }
                }
            }
        } else if (uiState.destinationName.isNotEmpty() && !uiState.isLoading && uiState.error == null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Text(
                    "No safe route found to '${uiState.destinationName}'. Try a different search.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
