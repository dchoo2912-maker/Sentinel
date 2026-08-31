package com.example.sentinel.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
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
import com.example.sentinel.viewmodel.DashboardViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun CommunityRiskMapScreen(
    viewModel: DashboardViewModel, 
    onReportIncident: () -> Unit,
    onNavigateToSafeRoute: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    var searchQuery by remember { mutableStateOf("") }
    var mapCenterScore by remember { mutableStateOf(100) }

    val blue600 = Color(0xFF2563EB)
    val red600 = Color(0xFFDC2626)
    val slate900 = Color(0xFF0F172A)
    val slate400 = Color(0xFF94A3B8)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(uiState.currentLocation, 12f)
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            mapCenterScore = viewModel.getSafetyScoreForLocation(target.latitude, target.longitude)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Map Area
        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
            ) {
                // Real Incident Circles from Firestore
                uiState.incidents.forEach { incident ->
                    Circle(
                        center = LatLng(incident.latitude, incident.longitude),
                        radius = 300.0,
                        fillColor = red600.copy(alpha = 0.3f),
                        strokeColor = red600,
                        strokeWidth = 2f
                    )
                }

                // Show Active Contacts/Users Locations
                uiState.activeLocations.forEach { (userId, location) ->
                    if (userId != uiState.user?.id) {
                        MarkerComposable(
                            state = rememberMarkerState(position = location),
                            title = "Active Sentinel User"
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null, tint = blue600, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // FLOATING: Search Bar
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = slate400)
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search location...", color = slate400) },
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
                            scope.launch {
                                try {
                                    val results = geocoder.getFromLocationName(searchQuery, 1)
                                    results?.firstOrNull()?.let { address ->
                                        val newLatLng = LatLng(address.latitude, address.longitude)
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(newLatLng, 14f)
                                        )
                                    }
                                } catch (e: Exception) { }
                            }
                        })
                    )
                }
            }

            // FLOATING: Safety Badge
            Surface(
                color = if (mapCenterScore > 70) Color(0xFFDCFCE7).copy(alpha = 0.9f) else Color(0xFFFEE2E2).copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 80.dp)
                    .align(Alignment.TopCenter),
                shadowElevation = 4.dp
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, tint = if (mapCenterScore > 70) Color(0xFF16A34A) else Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Area Safety: $mapCenterScore", color = if (mapCenterScore > 70) Color(0xFF16A34A) else Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            // FLOATING: Safe Route FAB
            FloatingActionButton(
                onClick = onNavigateToSafeRoute,
                containerColor = blue600,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Navigation, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Safe Route")
                }
            }
        }

        // 2. Risk Analysis Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.navigationBarsPadding().padding(20.dp)) {
                Text("Risk Analysis", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    LegendItem(red600, "Active Alerts", "${uiState.incidents.size}", Modifier.weight(1f))
                    LegendItem(blue600, "Active Users", "${uiState.activeLocations.size}", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onReportIncident,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = slate900),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Report an Incident", fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, count: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = color) {}
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 11.sp)
            Text(count, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
