package com.example.sentinel.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.sentinel.domain.IncidentReport
import com.example.sentinel.domain.RiskLevel
import com.example.sentinel.viewmodel.DashboardViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedIncident by remember { mutableStateOf<IncidentReport?>(null) }

    val blue600 = Color(0xFF2563EB)
    val red600 = Color(0xFFDC2626)
    val red500 = Color(0xFFEF4444)
    val slate900 = Color(0xFF0F172A)
    val slate400 = Color(0xFF94A3B8)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(uiState.currentLocation, 14f)
    }

    val nearbyRisk = RiskLevel.fromScore(uiState.immediateSafetyScore)
    val mapRisk = RiskLevel.fromScore(mapCenterScore)
    val zoneColor = Color(nearbyRisk.color)

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            mapCenterScore = viewModel.getSafetyScoreForLocation(target.latitude, target.longitude)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Google Map Area (Full Screen)
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
        ) {
            // Real Incident Markers from Firestore
            uiState.incidents.forEach { incident ->
                Marker(
                    state = rememberMarkerState(position = LatLng(incident.latitude, incident.longitude)),
                    title = incident.category,
                    onClick = {
                        selectedIncident = incident
                        true
                    }
                )
            }

            // 1 KM RADIUS ZONE AROUND USER
            Circle(
                center = uiState.currentLocation,
                radius = 1000.0, // 1 km
                fillColor = zoneColor.copy(alpha = 0.15f),
                strokeColor = zoneColor,
                strokeWidth = 4f
            )

            // Show Active Contacts/Users Locations
            uiState.activeLocations.forEach { (userId, location) ->
                if (userId != uiState.user?.id) {
                    MarkerComposable(
                        state = rememberMarkerState(position = location),
                        title = "Active Sentinel User"
                    ) {
                        Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
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
            modifier = Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = slate400)
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search location...", color = slate400) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        scope.launch {
                            try {
                                val results = geocoder.getFromLocationName(searchQuery, 1)
                                results?.firstOrNull()?.let { address ->
                                    val newLatLng = LatLng(address.latitude, address.longitude)
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(newLatLng, 14f))
                                }
                            } catch (e: Exception) { }
                        }
                    })
                )
            }
        }

        // FLOATING: Danger Level View Selector (Choose Nearby vs Map View)
        var isNearbyMode by remember { mutableStateOf(true) }
        val activeRisk = if (isNearbyMode) nearbyRisk else mapRisk
        val activeColor = Color(activeRisk.color)

        Surface(
            modifier = Modifier.statusBarsPadding().padding(top = 80.dp).align(Alignment.TopCenter),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { isNearbyMode = true },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isNearbyMode) activeColor else Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (nearbyRisk == RiskLevel.HIGH_RISK) Icons.Default.Warning else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isNearbyMode) Color.White else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Nearby: ${nearbyRisk.label}",
                            color = if (isNearbyMode) Color.White else Color.DarkGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    onClick = { isNearbyMode = false },
                    shape = RoundedCornerShape(20.dp),
                    color = if (!isNearbyMode) activeColor else Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = if (!isNearbyMode) Color.White else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Map View: ${mapRisk.label}",
                            color = if (!isNearbyMode) Color.White else Color.DarkGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // FLOATING: Scrollable Risk Analysis Card above BottomNavBar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 88.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Drag handle indicator
                Surface(
                    modifier = Modifier
                        .size(36.dp, 4.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(2.dp),
                    color = Color(0xFFCBD5E1)
                ) {}
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("1 km Radius Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Surface(color = zoneColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            nearbyRisk.label,
                            color = zoneColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    LegendItem(red600, "Nearby Risks", "${uiState.incidents.filter { calculateDistance(uiState.currentLocation.latitude, uiState.currentLocation.longitude, it.latitude, it.longitude) < 1.0 }.size}", Modifier.weight(1f))
                    LegendItem(blue600, "Active Users", "${uiState.activeLocations.size}", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onReportIncident,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = slate900),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Report Incident", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onNavigateToSafeRoute,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = blue600),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Safe Route", fontSize = 12.sp)
                    }
                }

                // Scrollable Nearby Incident List inside the card
                val nearbyIncidents = remember(uiState.incidents, uiState.currentLocation) {
                    uiState.incidents.filter { incident ->
                        calculateDistance(uiState.currentLocation.latitude, uiState.currentLocation.longitude, incident.latitude, incident.longitude) < 5.0
                    }.take(5)
                }

                if (nearbyIncidents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Scroll for Local Incident Details", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    nearbyIncidents.forEach { incident ->
                        val dist = calculateDistance(uiState.currentLocation.latitude, uiState.currentLocation.longitude, incident.latitude, incident.longitude)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedIncident = incident },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = red600, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(incident.category, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        if (incident.description.isNotBlank()) incident.description else incident.address,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    String.format(Locale.getDefault(), "%.1f km", dist),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedIncident != null) {
        ModalBottomSheet(onDismissRequest = { selectedIncident = null }, containerColor = Color.White) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(8.dp)) {
                        Text(selectedIncident!!.category.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    val date = java.text.SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(selectedIncident!!.timestamp))
                    Text(date, color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(selectedIncident!!.category, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(selectedIncident!!.description, color = Color.DarkGray, fontSize = 16.sp)
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(selectedIncident!!.address, color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
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
