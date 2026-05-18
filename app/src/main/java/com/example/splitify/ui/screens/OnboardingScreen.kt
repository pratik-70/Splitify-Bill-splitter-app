package com.example.splitify.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.splitify.navigation.Screen
import com.example.splitify.ui.theme.NavySplit
import com.example.splitify.ui.theme.TealSplit
import com.example.splitify.ui.theme.BlueSplit

@Composable
fun OnboardingScreen(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavySplit)
    ) {
        // Dotted lines background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = 300f,
                center = center.copy(y = center.y - 200f),
                style = Stroke(width = 2f, pathEffect = pathEffect)
            )
        }

        // Subtle background icons
        Box(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
            Icon(
                Icons.Default.Group, 
                contentDescription = null, 
                modifier = Modifier.size(120.dp).offset(x = (-20).dp, y = 100.dp),
                tint = Color.White
            )
            Icon(
                Icons.Default.CurrencyRupee, 
                contentDescription = null, 
                modifier = Modifier.size(80.dp).offset(x = 280.dp, y = 80.dp),
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            LogoComponent()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row {
                Text(
                    text = "Split",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "ify",
                    color = TealSplit,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .background(TealSplit, RoundedCornerShape(2.dp))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Split smarter, not harder.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Illustration
            IllustrationPlaceholder()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Bottom Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FeatureItem(
                            icon = Icons.Default.Group,
                            title = "Add Friends",
                            description = "Create your\ngroup"
                        )
                        FeatureItem(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = "Add Expenses",
                            description = "Track shared\nexpenses"
                        )
                        FeatureItem(
                            icon = Icons.Default.BarChart,
                            title = "Settle Up",
                            description = "See who owes\nwhom"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { 
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealSplit),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogoComponent() {
    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Circle
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = Color.White
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(TealSplit)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(end = 12.dp)
                                .size(36.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(BlueSplit)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(start = 12.dp)
                                .size(36.dp)
                        )
                    }
                }
                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
                
                // Rupee Symbol in Center
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "₹",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealSplit
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(LightGreySplit, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = NavySplit
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = NavySplit,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun IllustrationPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // Character 1 (Left)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = Color(0xFFFFD1BA)) {}
            Surface(
                modifier = Modifier.width(60.dp).height(80.dp),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                color = TealSplit
            ) {}
        }
        Spacer(modifier = Modifier.width((-10).dp))
        // Character 2 (Middle)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-15).dp)) {
            Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = Color(0xFFFFD1BA)) {}
            Surface(
                modifier = Modifier.width(70.dp).height(100.dp),
                shape = RoundedCornerShape(topStart = 35.dp, topEnd = 35.dp),
                color = BlueSplit
            ) {}
        }
        Spacer(modifier = Modifier.width((-10).dp))
        // Character 3 (Right)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = Color(0xFFFFD1BA)) {}
            Surface(
                modifier = Modifier.width(60.dp).height(80.dp),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                color = NavySplit
            ) {}
        }
    }
}

val LightGreySplit = Color(0xFFF4F6F8)
