package com.example.sentinel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.SentinelScreen
import com.example.sentinel.domain.ActivityType
import com.example.sentinel.domain.SafetyActivity
import com.example.sentinel.viewmodel.DashboardViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeDashboard(viewModel: DashboardViewModel, onNavigate: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    val blue600 = Color(0xFF2563EB)
    val blue800 = Color(0xFF1E40AF)
    val slate50 = Color(0xFFF8FAFC)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Header(userName = uiState.user?.name ?: "User")
            Spacer(modifier = Modifier.height(32.dp))
            StatusCard(blue600, blue800, isProtected = uiState.isProtected, score = uiState.safetyScore)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            QuickActionsGrid(onNavigate)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Recent Activity", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(uiState.recentActivities) { activity ->
            RecentActivityItem(activity, slate50)
            Spacer(modifier = Modifier.height(12.dp))
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun Header(userName: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Good Morning ☀️", color = Color.Gray, fontSize = 14.sp)
            Text(userName.split(" ").first(), fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Row {
            IconButton(onClick = { }) {
                Box {
                    Icon(Icons.Outlined.Notifications, null)
                    Surface(modifier = Modifier.size(8.dp).align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp), color = Color(0xFF2563EB), shape = CircleShape) {}
                }
            }
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color(0xFF2563EB)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(userName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusCard(blue600: Color, blue800: Color, isProtected: Boolean, score: Int) {
    val cardColors = when {
        score >= 90 -> listOf(blue600, blue800)
        score >= 70 -> listOf(Color(0xFFF59E0B), Color(0xFFD97706)) // Orange/Amber
        else -> listOf(Color(0xFFDC2626), Color(0xFF991B1B)) // Red
    }
    
    val statusColor = if (score >= 90) Color(0xFF86EFAC) else Color.White

    Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(cardColors), shape = RoundedCornerShape(24.dp))) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Surface(modifier = Modifier.size(8.dp), color = statusColor, shape = CircleShape) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isProtected) "PROTECTED" else "VULNERABLE", 
                            color = statusColor, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("$score", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text("Safety Score", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (score >= 90) "All Systems Active" 
                else if (score >= 70) "Caution Advised" 
                else "High Risk Detected", 
                color = Color.White, 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold
            )
            Text("Sentinel Intelligence Monitoring", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusItem(Icons.Default.BatteryFull, "87%", statusColor)
                StatusItem(Icons.Default.GpsFixed, "GPS", statusColor)
                StatusItem(Icons.Default.Bluetooth, "BT", statusColor)
                StatusItem(Icons.Default.NetworkCell, "4G", statusColor)
            }
        }
    }
}

@Composable
fun StatusItem(icon: ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun QuickActionsGrid(onNavigate: (String) -> Unit) {
    Column {
        Row {
            ActionCard(Modifier.weight(1f), "Emergency SOS", Icons.Default.Warning, Color(0xFFFEE2E2), Color(0xFFDC2626)) {
                onNavigate(SentinelScreen.Emergency.route)
            }
            Spacer(modifier = Modifier.width(16.dp))
            ActionCard(Modifier.weight(1f), "Share Location", Icons.Default.Share, Color(0xFFDBEAFE), Color(0xFF2563EB)) {
                onNavigate(SentinelScreen.LiveTracking.route)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            ActionCard(Modifier.weight(1f), "Check In", Icons.Default.CheckCircle, Color(0xFFDCFCE7), Color(0xFF16A34A)) {
                onNavigate(SentinelScreen.ActivityHistory.route)
            }
            Spacer(modifier = Modifier.width(16.dp))
            ActionCard(Modifier.weight(1f), "Call Emergency", Icons.Default.Phone, Color(0xFFFEF3C7), Color(0xFFD97706)) {
                onNavigate(SentinelScreen.Emergency.route) // Reuse Emergency for now
            }
        }
    }
}

@Composable
fun ActionCard(modifier: Modifier, label: String, icon: ImageVector, bgColor: Color, iconColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(modifier = Modifier.size(48.dp), color = bgColor, shape = RoundedCornerShape(12.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconColor) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun RecentActivityItem(activity: SafetyActivity, slate50: Color) {
    val icon = when(activity.type) {
        ActivityType.SAFE_TRIP -> Icons.Default.Route
        ActivityType.CHECK_IN -> Icons.Default.CheckCircle
        ActivityType.SOS -> Icons.Default.Warning
        else -> Icons.Default.Notifications
    }
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = slate50) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color(0xFFDBEAFE)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp)) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(activity.description, color = Color.Gray, fontSize = 12.sp)
            }
            val timeString = remember(activity.timestamp) {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(activity.timestamp))
            }
            Text(timeString, color = Color.LightGray, fontSize = 12.sp)
        }
    }
}
