package com.example.sentinel.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.viewmodel.EmergencyViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EmergencyScreen(viewModel: EmergencyViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    
    val smsPermissionState = rememberPermissionState(
        android.Manifest.permission.SEND_SMS
    )

    val red600 = Color(0xFFDC2626)
    val red950 = Color(0xFF450A0A)
    val slate900 = Color(0xFF0F172A)
    val slate400 = Color(0xFF94A3B8)

    val bgColor by animateColorAsState(
        targetValue = if (uiState.isActivated) red950 else slate900,
        animationSpec = tween(500),
        label = "bgColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = slate400) }
            Text("EMERGENCY", color = slate400, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            if (uiState.isActivated) {
                Text("SOS ACTIVATED", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                uiState.statusMessage?.let {
                    Text(it, color = Color.Yellow, fontSize = 14.sp, modifier = Modifier.padding(bottom = 24.dp))
                }
            } else {
                Text("Press and hold for 3 seconds to trigger SOS", color = slate400, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(bottom = 32.dp))
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                if (uiState.isActivated) {
                    repeat(2) { i -> PulseRing(delay = i * 1000) }
                }

                Canvas(modifier = Modifier.size(208.dp)) {
                    drawArc(Color.White.copy(alpha = 0.1f), 0f, 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(Color.Red, -90f, uiState.progress * 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                }

                Surface(
                    modifier = Modifier.size(160.dp).pointerInput(Unit) {
                        detectTapGestures(onPress = { 
                            if (smsPermissionState.status.isGranted) {
                                viewModel.startHolding()
                            } else {
                                smsPermissionState.launchPermissionRequest()
                            }
                            try { awaitRelease() } finally { viewModel.stopHolding() }
                        })
                    },
                    shape = CircleShape,
                    color = red600,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(40.dp))
                            Text("SOS", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (uiState.isHolding && !uiState.isActivated) {
                Text(text = String.format("%.1fs", uiState.countdown), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
            }

            if (uiState.isActivated) {
                Spacer(modifier = Modifier.height(48.dp))
                Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(10.dp), color = Color.Green, shape = CircleShape) {}
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Live Location: ${uiState.location}", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        if (uiState.isActivated) {
            Button(onClick = { viewModel.deactivateSos() }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                Text("Cancel SOS Emergency", color = Color.White)
            }
        } else {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
                Text("Cancel", color = slate400)
            }
        }
    }
}

@Composable
fun PulseRing(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 2f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing, delayMillis = delay), repeatMode = RepeatMode.Restart), label = "scale")
    val alpha by infiniteTransition.animateFloat(initialValue = 0.5f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing, delayMillis = delay), repeatMode = RepeatMode.Restart), label = "alpha")
    Box(modifier = Modifier.size(160.dp).scale(scale).alpha(alpha).background(Color.Red.copy(alpha = 0.5f), CircleShape))
}
