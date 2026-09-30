package com.example.sentinel.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.util.QrCodeGenerator
import com.example.sentinel.viewmodel.AuthViewModel
import com.example.sentinel.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    onBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToDevice: () -> Unit,
    onNavigateToActivity: () -> Unit
) {
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val user = dashboardState.user
    val blue600 = Color(0xFF2563EB)
    val blue800 = Color(0xFF1E40AF)
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        containerColor = Color(0xFFF1F5F9),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            // Profile Hero
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(blue600, blue800))).statusBarsPadding().padding(horizontal = 16.dp, vertical = 24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
                        Box(contentAlignment = Alignment.Center) { Text(user?.name?.take(1)?.uppercase() ?: "U", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(user?.name ?: "User", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text(user?.email ?: "email@example.com", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    
                    // Sentinel User ID Section
                    Spacer(modifier = Modifier.height(12.dp))
                    var showQrDialog by remember { mutableStateOf(false) }

                    Surface(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.clickable { 
                                user?.id?.let { clipboardManager.setText(AnnotatedString(it)) }
                            }) {
                                Text("SENTINEL ID", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(user?.id ?: "Loading...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { user?.id?.let { clipboardManager.setText(AnnotatedString(it)) } },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { showQrDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.QrCode, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (showQrDialog && user != null) {
                        AlertDialog(
                            onDismissRequest = { showQrDialog = false },
                            title = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text("Sentinel ID QR Code", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(user.name, fontSize = 14.sp, color = Color.Gray)
                                }
                            },
                            text = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    val qrBitmap = remember(user.id) {
                                        QrCodeGenerator.generateQrCode(user.id, size = 512)
                                    }
                                    if (qrBitmap != null) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Image(
                                                bitmap = qrBitmap,
                                                contentDescription = "Sentinel ID QR Code",
                                                modifier = Modifier.size(200.dp).padding(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable {
                                            clipboardManager.setText(AnnotatedString(user.id))
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(user.id, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                            Spacer(Modifier.width(8.dp))
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                        }
                                    }
                                    Text("Scan to quickly add as an Emergency Contact", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showQrDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    if (user?.isPro == true) {
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Shield, null, tint = Color(0xFF166534), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Protected · Sentinel Pro", color = Color(0xFF166534), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickNavCard(Modifier.weight(1f), "Contacts", Icons.Default.Group, onNavigateToContacts)
                    QuickNavCard(Modifier.weight(1f), "Device", Icons.Default.Devices, onNavigateToDevice)
                    QuickNavCard(Modifier.weight(1f), "Activity", Icons.Default.History, onNavigateToActivity)
                }
                Spacer(modifier = Modifier.height(32.dp))
                SettingsSection("PRIVACY & SECURITY") {
                    SettingsRow("Location Sharing", true)
                    SettingsRow("Anonymous Reporting", false)
                    SettingsRow("Data Sharing", null)
                }
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSection("NOTIFICATIONS") {
                    SettingsRow("Push Notifications", true)
                    SettingsRow("SMS Backup Alerts", true)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Surface(modifier = Modifier.fillMaxWidth().clickable { authViewModel.logout() }, shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Sign Out", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun QuickNavCard(modifier: Modifier, label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable { onClick() }, shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = Color(0xFFEFF6FF)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) { Column { content() } }
    }
}

@Composable
fun SettingsRow(label: String, toggleState: Boolean? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium)
        if (toggleState != null) {
            var checked by remember { mutableStateOf(toggleState) }
            Switch(checked = checked, onCheckedChange = { checked = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB)))
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
        }
    }
}
