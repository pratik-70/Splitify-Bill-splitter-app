package com.example.splitify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.splitify.ui.components.SplitifyTopAppBar
import com.example.splitify.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(viewModel: MainViewModel, navController: NavHostController, initialGroupId: String? = null) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    
    val friends by viewModel.friends.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val usersMap by viewModel.usersMap.collectAsState()
    
    var selectedGroupId by remember { mutableStateOf(initialGroupId) }
    val involvedUserIds = remember { mutableStateListOf<String>() }
    val currentUserId = currentUser?.id ?: ""
    
    // Initialize involved users when group or groups list changes
    LaunchedEffect(selectedGroupId, groups) {
        val currentGroup = groups.find { it.id == selectedGroupId }
        if (selectedGroupId != null && currentGroup != null) {
            involvedUserIds.clear()
            involvedUserIds.addAll(currentGroup.members)
        } else if (selectedGroupId == null) {
            if (involvedUserIds.isEmpty()) {
                 involvedUserIds.clear()
                 involvedUserIds.add(currentUserId)
            }
        }
    }
    
    var paidByUserId by remember { mutableStateOf(currentUserId) }
    var groupExpanded by remember { mutableStateOf(false) }
    var paidByExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SplitifyTopAppBar(
                title = "Add Expense",
                actions = {
                    TextButton(
                        onClick = {
                            if (description.isNotEmpty() && amount.isNotEmpty()) {
                                val amountVal = amount.toDoubleOrNull() ?: 0.0
                                val finalInvolved = if (involvedUserIds.isNotEmpty()) {
                                    involvedUserIds.toList()
                                } else {
                                    listOf(currentUserId)
                                }
                                
                                viewModel.addExpense(
                                    description = description,
                                    amount = amountVal,
                                    paidBy = paidByUserId,
                                    groupId = selectedGroupId,
                                    involvedUserIds = finalInvolved
                                )
                                navController.popBackStack()
                            }
                        },
                        enabled = description.isNotBlank() && amount.isNotBlank()
                    ) {
                        Text("Save", color = if (description.isNotBlank() && amount.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Amount and Description Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("₹", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        TextField(
                            value = amount,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                            placeholder = { Text("0.00", fontSize = 32.sp, fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }

                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("What was it for?") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            }

            // Paid By Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Paid by:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                ExposedDropdownMenuBox(
                    expanded = paidByExpanded,
                    onExpandedChange = { paidByExpanded = !paidByExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selectedPayerName = if (paidByUserId == currentUserId) "You" else (usersMap[paidByUserId]?.name ?: "Select Payer")
                    
                    OutlinedTextField(
                        value = selectedPayerName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paidByExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    ExposedDropdownMenu(
                        expanded = paidByExpanded,
                        onDismissRequest = { paidByExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("You") },
                            onClick = {
                                paidByUserId = currentUserId
                                paidByExpanded = false
                            }
                        )
                        
                        val potentialPayers = if (selectedGroupId != null) {
                            groups.find { it.id == selectedGroupId }?.members?.filter { it != currentUserId } ?: emptyList()
                        } else {
                            friends.map { it.user.id }
                        }
                        
                        potentialPayers.forEach { userId ->
                            val userName = usersMap[userId]?.name ?: "Unknown User"
                            DropdownMenuItem(
                                text = { Text(userName) },
                                onClick = {
                                    paidByUserId = userId
                                    paidByExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Split with:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                // Group Selection
                Text("Group (Optional)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                ExposedDropdownMenuBox(
                    expanded = groupExpanded,
                    onExpandedChange = { groupExpanded = !groupExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selectedGroup = groups.find { it.id == selectedGroupId }
                    OutlinedTextField(
                        value = selectedGroup?.name ?: "No group (Personal)",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No group (Personal)") },
                            onClick = {
                                selectedGroupId = null
                                groupExpanded = false
                            }
                        )
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    selectedGroupId = group.id
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }

                // Member Selection Chips
                val displayUsers = if (selectedGroupId != null) {
                    groups.find { it.id == selectedGroupId }?.members ?: emptyList()
                } else {
                    (listOf(currentUserId) + friends.map { it.user.id }).distinct()
                }

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayUsers.size) { index ->
                        val userId = displayUsers[index]
                        val userName = if (userId == currentUserId) "You" else (usersMap[userId]?.name ?: "Unknown")
                        val isSelected = involvedUserIds.contains(userId)
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    if (involvedUserIds.size > 1) involvedUserIds.remove(userId)
                                } else {
                                    involvedUserIds.add(userId)
                                }
                            },
                            label = { Text(userName) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            val payerDisplayName = if (paidByUserId == currentUserId) "you" else (usersMap[paidByUserId]?.name ?: "someone")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Text(
                    "Paid by $payerDisplayName and split equally among ${involvedUserIds.size} selected.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
