package com.example.sentinel.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.viewmodel.DashboardViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val blue600 = Color(0xFF2563EB)
    val red600 = Color(0xFFDC2626)
    val slate400 = Color(0xFF94A3B8)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(uiState.currentLocation, 15f)
    }

    // Auto-enable sharing when entering this screen
    LaunchedEffect(Unit) {
        viewModel.toggleLocationSharing(true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Real Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            // Self Marker
            MarkerComposable(
                state = MarkerState(position = uiState.currentLocation)
            ) {
                LocationPulseMarker(blue600)
            }

            // Other Active Users / Contacts
            uiState.activeLocations.forEach { (userId, location) ->
                if (userId != uiState.user?.id) {
                    MarkerComposable(
                        state = rememberMarkerState(position = location),
                        title = "Contact Location"
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Back Button
        IconButton(
            onClick = {
                viewModel.toggleLocationSharing(false)
                onBack()
            },
            modifier = Modifier.statusBarsPadding().padding(16.dp).background(Color.White, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        // Live Tracking Chip
        Surface(
            modifier = Modifier.statusBarsPadding().padding(top = 16.dp).align(Alignment.TopCenter),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(8.dp), 
                    color = if (uiState.isSharingLocation) Color(0xFF16A34A) else Color(0xFF94A3B8), 
                    shape = CircleShape
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (uiState.isSharingLocation) "Live Sharing Active" else "Sharing Paused", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 12.sp
                )
            }
        }

        // Bottom Sheet
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.navigationBarsPadding().padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active connections: ${uiState.activeLocations.size - 1}", color = slate400, fontSize = 14.sp)
                        Text(if (uiState.isSharingLocation) "Broadcasting..." else "Standby", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                    Row {
                        repeat(minOf(uiState.activeLocations.size, 3)) { index ->
                            Surface(
                                modifier = Modifier.size(32.dp).offset(x = (index * -8).dp),
                                shape = CircleShape,
                                color = Color(0xFF16A34A),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                            ) {}
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TrackingStat(Icons.Default.Place, "GPS", "Strong", Color(0xFFDCFCE7), Color(0xFF16A34A))
                    TrackingStat(Icons.Default.BatteryFull, "Battery", "87%", Color(0xFFDBEAFE), Color(0xFF2563EB))
                    TrackingStat(Icons.Default.SignalCellularAlt, "Cloud", "Synced", Color(0xFFF3E8FF), Color(0xFF9333EA))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.toggleLocationSharing(false)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isSharingLocation) red600 else blue600
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (uiState.isSharingLocation) "Stop Sharing Location" else "Start Sharing")
                }
            }
        }
    }
}

@Composable
fun LocationPulseMarker(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ping"
    )
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(30.dp).scale(pingScale).alpha(pingAlpha).background(color.copy(alpha = 0.5f), CircleShape)
        )
        Box(
            modifier = Modifier.size(30.dp).background(color.copy(alpha = 0.2f), CircleShape)
        )
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = color,
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
        ) {}
    }
}

@Composable
fun TrackingStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, bgColor: Color, iconColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = bgColor) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
