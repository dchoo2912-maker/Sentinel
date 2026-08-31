package com.example.sentinel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.SentinelScreen

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val blue600 = Color(0xFF2563EB)
    val slate400 = Color(0xFF94A3B8)
    val red600 = Color(0xFFDC2626)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
    ) {
        // Background strip
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem("Home", Icons.Default.Home, SentinelScreen.Home.route, currentRoute, onNavigate, blue600, slate400)
                NavBarItem("Map", Icons.Default.Map, SentinelScreen.CommunityRisk.route, currentRoute, onNavigate, blue600, slate400)
                
                // Spacer for SOS button
                Spacer(modifier = Modifier.width(56.dp))
                
                NavBarItem("Activity", Icons.Default.History, SentinelScreen.ActivityHistory.route, currentRoute, onNavigate, blue600, slate400)
                NavBarItem("Profile", Icons.Default.Person, SentinelScreen.ProfileSettings.route, currentRoute, onNavigate, blue600, slate400)
            }
        }

        // Raised SOS Button
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(64.dp)
                .offset(y = (-4).dp)
                .clickable { onNavigate(SentinelScreen.Emergency.route) },
            shape = CircleShape,
            color = red600,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "SOS",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun NavBarItem(
    label: String,
    icon: ImageVector,
    route: String,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    activeColor: Color,
    inactiveColor: Color
) {
    val isSelected = currentRoute == route
    val color = if (isSelected) activeColor else inactiveColor

    Column(
        modifier = Modifier
            .clickable { onNavigate(route) }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = color,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
