@file:OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)

package com.example.sentinel.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.SentinelScreen
import com.example.sentinel.domain.ActivityType
import com.example.sentinel.domain.IncidentReport
import com.example.sentinel.domain.RiskLevel
import com.example.sentinel.domain.SafetyActivity
import com.example.sentinel.viewmodel.DashboardViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.LatLng
import java.util.*

private fun getDeviceBatteryLevel(context: Context): Int {
    return try {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level != -1 && scale != -1) {
            ((level / scale.toFloat()) * 100).toInt()
        } else {
            100
        }
    } catch (e: Exception) {
        100
    }
}

@Composable
fun rememberBatteryLevel(): Int {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(getDeviceBatteryLevel(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                batteryLevel = getDeviceBatteryLevel(context)
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }
    return batteryLevel
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboard(viewModel: DashboardViewModel, onNavigate: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var showPermissionRationale by remember { mutableStateOf(false) }
    val batteryLevel = rememberBatteryLevel()
    
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            showPermissionRationale = true
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Location Access Required", fontWeight = FontWeight.Bold) },
            text = { Text("Sentinel uses your location to calculate real-time safety scores, show nearby risks within 1km, and allow safe route navigation. Please grant location access for the full experience.") },
            confirmButton = {
                Button(onClick = { 
                    locationPermissions.launchMultiplePermissionRequest()
                    showPermissionRationale = false 
                }) {
                    Text("Grant Access")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Maybe Later")
                }
            }
        )
    }

    val blue600 = Color(0xFF2563EB)
    val blue800 = Color(0xFF1E40AF)
    val slate50 = Color(0xFFF8FAFC)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        item {
            Header(userName = uiState.user?.name ?: "User")
            Spacer(modifier = Modifier.height(24.dp))
            StatusCard(blue600, blue800, isProtected = uiState.isProtected, score = uiState.safetyScore, batteryLevel = batteryLevel)
            Spacer(modifier = Modifier.height(24.dp))

            // ZONE RISK ALERT BANNER (If other users submitted reports in this area)
            val nearbyReports = uiState.nearbyZoneReports
            val count1km = remember(nearbyReports, uiState.currentLocation) {
                nearbyReports.count { calculateDistance(uiState.currentLocation.latitude, uiState.currentLocation.longitude, it.latitude, it.longitude) < 1.0 }
            }

            if (nearbyReports.isNotEmpty()) {
                val bannerBg = if (count1km > 0) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
                val bannerBorder = if (count1km > 0) Color(0xFFFCA5A5) else Color(0xFFFDE68A)
                val iconTint = if (count1km > 0) Color(0xFFDC2626) else Color(0xFFD97706)
                val textPrimary = if (count1km > 0) Color(0xFF991B1B) else Color(0xFF92400E)
                val textSecondary = if (count1km > 0) Color(0xFF7F1D1D) else Color(0xFF78350F)

                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(SentinelScreen.CommunityRisk.route) },
                    shape = RoundedCornerShape(16.dp),
                    color = bannerBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, bannerBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = iconTint
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (count1km > 0) Icons.Default.Warning else Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (count1km > 0) "HIGH RISK ZONE ALERT" else "CAUTION ZONE ALERT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textPrimary
                            )
                            Text(
                                if (count1km > 0) "$count1km active report(s) within 1km of your location" else "${nearbyReports.size} active report(s) affecting your 5km zone",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "View Map", tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            QuickActionsGrid(onNavigate)
            Spacer(modifier = Modifier.height(24.dp))

            // NEARBY ZONE REPORTS FROM OTHER USERS
            if (uiState.nearbyZoneReports.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Community Zone Reports", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Other users' reports affecting your zone risk", color = Color.Gray, fontSize = 12.sp)
                    }
                    TextButton(onClick = { onNavigate(SentinelScreen.CommunityRisk.route) }) {
                        Text("View Map", color = blue600, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Display reports from other users in area
        items(uiState.nearbyZoneReports.take(4)) { report ->
            NearbyZoneReportItem(
                report = report,
                userLocation = uiState.currentLocation,
                onClick = { onNavigate(SentinelScreen.CommunityRisk.route) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Recent Activity Log", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (uiState.recentActivities.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = slate50
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = blue600, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("All Quiet Around You", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("No recent alerts logged. Your safety updates will appear here.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(uiState.recentActivities) { activity ->
                RecentActivityItem(activity, slate50)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
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
fun StatusCard(blue600: Color, blue800: Color, isProtected: Boolean, score: Int, batteryLevel: Int) {
    val risk = RiskLevel.fromScore(score)
    val cardColors = when (risk) {
        RiskLevel.SAFE -> listOf(blue600, blue800)
        RiskLevel.CAUTION -> listOf(Color(risk.color), Color(0xFFD97706))
        RiskLevel.HIGH_RISK -> listOf(Color(risk.color), Color(0xFF991B1B))
    }
    
    val statusColor = if (risk == RiskLevel.SAFE) Color(0xFF86EFAC) else Color.White

    Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(cardColors), shape = RoundedCornerShape(24.dp))) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Surface(modifier = Modifier.size(8.dp), color = statusColor, shape = CircleShape) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            risk.label, 
                            color = statusColor, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(risk.label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Current Status", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (risk == RiskLevel.SAFE) "All Systems Active" 
                else if (risk == RiskLevel.CAUTION) "Caution Advised" 
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
                StatusItem(Icons.Default.BatteryFull, "${batteryLevel}%", statusColor)
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
                onNavigate(SentinelScreen.Emergency.route)
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
fun NearbyZoneReportItem(report: IncidentReport, userLocation: LatLng, onClick: () -> Unit) {
    val dist = calculateDistance(userLocation.latitude, userLocation.longitude, report.latitude, report.longitude)
    val is1km = dist < 1.0

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (is1km) Color(0xFFFCA5A5) else Color(0xFFE2E8F0))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (is1km) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = if (is1km) Color(0xFFDC2626) else Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(report.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (is1km) Color(0xFFDC2626) else Color(0xFFD97706),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            String.format(Locale.getDefault(), "%.1f km away", dist),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    if (report.description.isNotBlank()) report.description else report.address,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            val timeString = remember(report.timestamp) {
                java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(report.timestamp))
            }
            Text(timeString, color = Color.Gray, fontSize = 11.sp)
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
                java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(activity.timestamp))
            }
            Text(timeString, color = Color.LightGray, fontSize = 12.sp)
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
