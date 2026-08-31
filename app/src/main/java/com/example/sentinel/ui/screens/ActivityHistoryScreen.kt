package com.example.sentinel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.domain.ActivityType
import com.example.sentinel.domain.SafetyActivity
import com.example.sentinel.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val blue600 = Color(0xFF2563EB)
    val green600 = Color(0xFF16A34A)
    val red600 = Color(0xFFDC2626)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { DateHeader("RECENT ACTIVITY") }
            items(uiState.recentActivities) { activity ->
                val icon = when(activity.type) {
                    ActivityType.SAFE_TRIP -> Icons.Default.Route
                    ActivityType.CHECK_IN -> Icons.Default.CheckCircle
                    ActivityType.SOS -> Icons.Default.Warning
                    else -> Icons.Default.Notifications
                }
                val color = when(activity.type) {
                    ActivityType.SAFE_TRIP -> green600
                    ActivityType.CHECK_IN -> blue600
                    ActivityType.SOS -> red600
                    else -> Color.Gray
                }
                val timeString = remember(activity.timestamp) {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(activity.timestamp))
                }
                HistoryItem(activity.title, activity.description, timeString, icon, color)
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DateHeader(text: String) {
    Text(text, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun HistoryItem(title: String, desc: String, time: String, icon: ImageVector, color: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = color.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, color = Color.Gray, fontSize = 12.sp)
            }
            Text(time, color = Color.LightGray, fontSize = 12.sp)
        }
    }
}
