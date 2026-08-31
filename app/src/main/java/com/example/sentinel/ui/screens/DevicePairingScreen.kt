package com.example.sentinel.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Radio
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
import com.example.sentinel.domain.SentinelDevice
import com.example.sentinel.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairingScreen(viewModel: DeviceViewModel, onPaired: () -> Unit, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val blue600 = Color(0xFF2563EB)
    val slate50 = Color(0xFFF8FAFC)
    val slate400 = Color(0xFF94A3B8)

    LaunchedEffect(Unit) {
        viewModel.startScanning()
    }

    LaunchedEffect(uiState.connectedDevice) {
        if (uiState.connectedDevice != null) {
            onPaired()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            Text("Pair Your Device", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("Connect your Sentinel hardware for the full experience.", fontSize = 16.sp, color = slate400, modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(48.dp))

            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                if (uiState.isScanning) { SonarAnimation(blue600) }
                Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = blue600, shadowElevation = 8.dp) {
                    Icon(Icons.Default.Bluetooth, null, modifier = Modifier.padding(20.dp).size(40.dp), tint = Color.White)
                }
            }

            Text(
                text = if (uiState.isScanning) "Scanning for devices..." else "Scan complete",
                color = blue600,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text("NEARBY DEVICES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = slate400, letterSpacing = 1.sp)

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.devices) { device ->
                    DeviceRow(device, blue600, slate50) { viewModel.connectDevice(device) }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onPaired, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Skip for now", color = slate400)
            }
        }
    }
}

@Composable
fun SonarAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonar")
    @Composable fun SonarRing(delay: Int) {
        val animationProgress by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing, delayMillis = delay), repeatMode = RepeatMode.Restart), label = "ring")
        Box(modifier = Modifier.size(144.dp).scale(0.5f + (animationProgress * 0.5f)).alpha(1f - animationProgress).background(color.copy(alpha = 0.2f), CircleShape))
    }
    SonarRing(0)
    SonarRing(600)
    SonarRing(1200)
}

@Composable
fun DeviceRow(device: SentinelDevice, blue600: Color, slate50: Color, onConnect: () -> Unit) {
    Surface(color = slate50, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), color = blue600, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Radio, null, modifier = Modifier.padding(8.dp), tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Bold)
                Text("${device.serialNumber} · ${(device.batteryLevel * 100).toInt()}%", fontSize = 12.sp, color = Color.Gray)
            }
            Button(onClick = onConnect, colors = ButtonDefaults.buttonColors(containerColor = blue600), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp), modifier = Modifier.height(32.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Connect", fontSize = 12.sp)
            }
        }
    }
}
