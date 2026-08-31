package com.example.sentinel.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val navyBlue = Color(0xFF020617)
    val midBlue = Color(0xFF1E40AF)
    val lightBlue = Color(0xFF93C5FD)

    LaunchedEffect(Unit) {
        delay(2800)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(navyBlue, midBlue)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        RadarPulse(lightBlue)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.statusBarsPadding().navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .blur(10.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "SENTINEL",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )

            Text(
                text = "INTELLIGENT URBAN SAFETY",
                color = lightBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            LoadingDots()
        }

        Text(
            text = "© 2026 SENTINEL SAFETY SYSTEMS",
            color = navyBlue.copy(alpha = 0.5f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
fun RadarPulse(pulseColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )
    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse3"
    )

    Box(contentAlignment = Alignment.Center) {
        PulseCircle(pulse1, pulseColor)
        PulseCircle(pulse2, pulseColor)
        PulseCircle(pulse3, pulseColor)
    }
}

@Composable
fun PulseCircle(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .size(300.dp)
            .scale(progress)
            .alpha(1f - progress)
            .border(2.dp, color.copy(alpha = 0.2f), RoundedCornerShape(150.dp))
    )
}

@Composable
fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    @Composable
    fun BouncingDot(delay: Int) {
        val yOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -10f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(delay)
            ),
            label = "dot"
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(8.dp)
                .offset(y = yOffset.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
        )
    }

    Row {
        BouncingDot(0)
        BouncingDot(200)
        BouncingDot(400)
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen(onTimeout = {})
}
