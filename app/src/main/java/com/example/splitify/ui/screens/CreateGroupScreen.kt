package com.example.splitify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.splitify.ui.components.SplitifyTopAppBar
import com.example.splitify.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(viewModel: MainViewModel, navController: NavHostController) {
    var groupName by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    val memberEmails = remember { mutableStateListOf<String>() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            SplitifyTopAppBar(
                title = "Create a group",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (groupName.isNotBlank()) {
                                // Add current email input if not empty but not added yet
                                val finalEmails = memberEmails.toMutableList()
                                val cleanInput = emailInput.trim().lowercase()
                                if (cleanInput.isNotBlank() && !finalEmails.contains(cleanInput)) {
                                    finalEmails.add(cleanInput)
                                }

                                viewModel.createGroup(
                                    name = groupName,
                                    memberEmails = finalEmails,
                                    onSuccess = {
                                        android.widget.Toast.makeText(context, "Group created!", android.widget.Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    },
                                    onError = { error ->
                                        android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        enabled = groupName.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Done", color = if (groupName.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                TextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Group name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }

            Text("Add members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    placeholder = { Text("Email address") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val cleanEmail = emailInput.trim().lowercase()
                        if (cleanEmail.isNotBlank() && !memberEmails.contains(cleanEmail)) {
                            memberEmails.add(cleanEmail)
                            emailInput = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Member")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(memberEmails) { email ->
                    ListItem(
                        headlineContent = { Text(email) },
                        trailingContent = {
                            IconButton(onClick = { memberEmails.remove(email) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    )
                }
            }
        }
    }
}
