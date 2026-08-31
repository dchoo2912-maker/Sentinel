package com.example.sentinel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.viewmodel.IncidentViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(viewModel: IncidentViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var showMapPicker by remember { mutableStateOf(false) }

    val blue600 = Color(0xFF2563EB)
    val slate50 = Color(0xFFF8FAFC)
    val slate200 = Color(0xFFE2E8F0)
    val slate400 = Color(0xFF94A3B8)
    val slate900 = Color(0xFF0F172A)

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            onBack()
            viewModel.resetSubmission()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Incident", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
            ReportCard("Incident Category") {
                val categories = listOf("Theft", "Assault", "Suspicious", "Vandalism", "Harassment", "Other")
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(180.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        val isSelected = category == uiState.category
                        Surface(modifier = Modifier.clickable { viewModel.updateCategory(category) }, shape = RoundedCornerShape(12.dp), color = if (isSelected) blue600 else slate50, border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, slate200)) {
                            Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text(category, color = if (isSelected) Color.White else slate900, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ReportCard("Description") {
                OutlinedTextField(value = uiState.description, onValueChange = { viewModel.updateDescription(it) }, modifier = Modifier.fillMaxWidth().height(120.dp), placeholder = { Text("What happened? Provide any details...") }, shape = RoundedCornerShape(12.dp), colors = TextFieldDefaults.colors(focusedContainerColor = slate50, unfocusedContainerColor = slate50))
            }
            Spacer(modifier = Modifier.height(16.dp))
            ReportCard("Photo") {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(slate50, RoundedCornerShape(12.dp)).border(1.dp, slate200, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, null, tint = slate400)
                        Text("Tap to add photo", color = slate400, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ReportCard("Location") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, tint = blue600)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(uiState.address, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text(
                            "Change", 
                            color = blue600, 
                            fontWeight = FontWeight.Bold, 
                            modifier = Modifier.clickable { showMapPicker = true }
                        )
                    }
                    if (uiState.isLocationManual) {
                        Text("Manually selected", color = Color(0xFF16A34A), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = slate50) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = slate900) }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anonymous Reporting", fontWeight = FontWeight.Bold)
                        Text("Your identity will be hidden", color = slate400, fontSize = 12.sp)
                    }
                    Switch(checked = uiState.isAnonymous, onCheckedChange = { viewModel.updateAnonymous(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = blue600))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.error != null) {
                Text(uiState.error!!, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
            }

            Button(
                onClick = { viewModel.submitReport() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = blue600),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Submit Report", fontSize = 16.sp)
                }
            }
        }
    }

    if (showMapPicker) {
        LocationPickerDialog(
            initialLocation = LatLng(uiState.latitude, uiState.longitude),
            onDismiss = { showMapPicker = false },
            onConfirm = { location, address ->
                viewModel.updateLocation(location.latitude, location.longitude, address)
                showMapPicker = false
            }
        )
    }
}

@Composable
fun LocationPickerDialog(
    initialLocation: LatLng,
    onDismiss: () -> Unit,
    onConfirm: (LatLng, String) -> Unit
) {
    var selectedLocation by remember { mutableStateOf(initialLocation) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 16f)
    }
    val context = LocalContext.current
    val geocoder = remember { android.location.Geocoder(context, java.util.Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Incident Location", fontWeight = FontWeight.Bold) },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.LightGray, RoundedCornerShape(12.dp))) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { selectedLocation = it }
                ) {
                    Marker(state = rememberMarkerState(position = selectedLocation))
                }
                
                Icon(
                    Icons.Default.Place, 
                    contentDescription = null, 
                    tint = Color.Red, 
                    modifier = Modifier.align(Alignment.Center).size(32.dp).offset(y = (-16).dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val targetLoc = cameraPositionState.position.target
                val addresses = geocoder.getFromLocation(targetLoc.latitude, targetLoc.longitude, 1)
                val addressName = addresses?.firstOrNull()?.getAddressLine(0) ?: "Custom Location"
                onConfirm(targetLoc, addressName)
            }) {
                Text("Select This Spot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReportCard(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
