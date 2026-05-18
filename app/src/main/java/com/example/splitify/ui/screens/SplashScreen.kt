package com.example.splitify.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.splitify.navigation.Screen
import com.example.splitify.ui.theme.NavySplit
import androidx.compose.runtime.collectAsState
import com.example.splitify.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController, viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    
    val scale = rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    LaunchedEffect(key1 = true) {
        delay(2000) // 2 seconds delay
        if (currentUser != null) {
            navController.navigate(Screen.Groups.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavySplit),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoComponent()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.scale(scale.value)) {
                Text(
                    text = "Split",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "ify",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
