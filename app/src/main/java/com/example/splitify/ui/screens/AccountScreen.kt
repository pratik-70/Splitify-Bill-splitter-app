package com.example.splitify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.splitify.navigation.Screen
import com.example.splitify.ui.components.SplitifyTopAppBar
import com.example.splitify.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(viewModel: MainViewModel, navController: NavHostController) {
    val user by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            SplitifyTopAppBar(title = "Account")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Added null-safe calls and default values for the nullable user object
            Text(user?.name ?: "Guest", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(user?.email ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AccountOptionItem(icon = Icons.Default.QrCode, title = "Scan code")
            AccountOptionItem(icon = Icons.Default.Star, title = "Splitify Pro")
            AccountOptionItem(icon = Icons.Default.Settings, title = "Settings")
            // Used AutoMirrored icon to resolve deprecation
            AccountOptionItem(icon = Icons.AutoMirrored.Filled.ContactSupport, title = "Contact Support")
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Implemented logout functionality
            TextButton(onClick = {
                viewModel.signOut()
                navController.navigate(Screen.Splash.route) {
                    popUpTo(0)
                }
            }) {
                Text("Log out", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AccountOptionItem(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 16.sp)
    }
}
