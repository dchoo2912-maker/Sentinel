package com.example.sentinel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
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
import com.example.sentinel.domain.DeviceSensor
import com.example.sentinel.domain.SentinelDevice
import com.example.sentinel.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusScreen(viewModel: DeviceViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val device = uiState.connectedDevice
    val blue600 = Color(0xFF2563EB)
    val blue800 = Color(0xFF1E40AF)
    val slate400 = Color(0xFF94A3B8)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(device?.name ?: "No Device", fontWeight = FontWeight.Bold)
                        Text(device?.serialNumber ?: "N/A", color = slate400, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        if (device != null) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { DeviceHero(device, blue600, blue800) }
                item { SensorList(device.sensors) }
                item {
                    Button(onClick = { }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sync Device Now", color = Color.Black)
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No device connected", color = slate400)
            }
        }
    }
}

@Composable
fun DeviceHero(device: SentinelDevice, blue600: Color, blue800: Color) {
    Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(blue600, blue800)), shape = RoundedCornerShape(24.dp))) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.2f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Shield, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(device.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(8.dp)) {
                        Text("Fully Connected", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF166534), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Battery Life", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { device.batteryLevel }, modifier = Modifier.weight(1f).height(8.dp), color = Color(0xFF86EFAC), trackColor = Color.White.copy(alpha = 0.2f), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                Spacer(modifier = Modifier.width(16.dp))
                Text("${(device.batteryLevel * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SensorList(sensors: List<DeviceSensor>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sensors.forEach { sensor -> SensorRow(sensor) }
    }
}

@Composable
fun SensorRow(sensor: DeviceSensor) {
    val icon = when(sensor.iconName) {
        "favorite" -> Icons.Default.Favorite
        "gps_fixed" -> Icons.Default.GpsFixed
        "warning" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }
    val iconColor = if (sensor.isActive) Color(0xFF16A34A) else Color.Gray
    val bgColor = if (sensor.isActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)

    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = bgColor) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp)) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(sensor.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(sensor.status, color = iconColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
