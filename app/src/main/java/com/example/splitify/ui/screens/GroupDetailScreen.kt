package com.example.splitify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.splitify.model.Expense
import com.example.splitify.ui.components.SplitifyTopAppBar
import com.example.splitify.navigation.Screen
import com.example.splitify.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(groupId: String?, viewModel: MainViewModel, navController: NavHostController) {
    val groups by viewModel.groups.collectAsState()
    val group = groups.find { it.id == groupId }
    val allExpenses by viewModel.expenses.collectAsState()
    val groupExpenses = allExpenses.filter { it.groupId == groupId }
    val currentUser by viewModel.currentUser.collectAsState()
    val usersMap by viewModel.usersMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserId = currentUser?.id ?: ""

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (group == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Group not found")
        }
        return
    }

    if (showMembersDialog) {
        GroupMembersDialog(
            group = group,
            usersMap = usersMap,
            onDismiss = { showMembersDialog = false }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { email ->
                groupId?.let {
                    viewModel.addMemberToGroup(it, email, 
                        onSuccess = { 
                            showAddMemberDialog = false 
                            android.widget.Toast.makeText(context, "Member added successfully", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onError = { errorMsg ->
                            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SplitifyTopAppBar(
                title = group.name,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMembersDialog = true }) {
                        Icon(Icons.Default.Groups, contentDescription = "View Members")
                    }
                    IconButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
                    }
                    IconButton(onClick = { 
                        viewModel.settleGroup(group.id)
                        android.widget.Toast.makeText(context, "Group balances settled!", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Settle Up")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddExpense.createRoute(groupId)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(padding)) {
                // Group Header Summary
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val userBalance = group.balances[currentUserId] ?: 0.0
                        Text("Your Balance", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (userBalance >= 0) "You are owed ₹${String.format(Locale.US, "%.2f", userBalance)}" 
                            else "You owe ₹${String.format(Locale.US, "%.2f", -userBalance)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (userBalance >= 0) Color(0xFF17B890) else Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                groupId?.let { viewModel.settleGroup(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Settle up")
                        }
                    }
                }

                if (groupExpenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No expenses in this group yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(groupExpenses) { expense ->
                            val payerName = if (expense.paidBy == currentUserId) "You" else (usersMap[expense.paidBy]?.name ?: "Someone")
                            GroupExpenseItem(
                                expense = expense,
                                currentUserId = currentUserId,
                                payerName = payerName,
                                onDelete = { viewModel.deleteExpense(expense) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                thickness = 0.5.dp,
                                color = Color.LightGray.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun GroupMembersDialog(
    group: com.example.splitify.model.Group,
    usersMap: Map<String, com.example.splitify.model.User>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Group Members") },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(group.members) { userId ->
                        val user = usersMap[userId]
                        val balance = group.balances[userId] ?: 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        (user?.name ?: "U").take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user?.name ?: "Unknown User", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(user?.email ?: "", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(
                                String.format(Locale.US, "₹%.2f", balance),
                                color = if (balance >= 0) Color(0xFF17B890) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun AddMemberDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the email address of the person you want to add to this group.")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (email.isNotBlank()) onConfirm(email) },
                enabled = email.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun GroupExpenseItem(expense: Expense, currentUserId: String, payerName: String, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("MMM dd", Locale.US)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(sdf.format(Date(expense.date)).split(" ")[0], fontSize = 10.sp, color = Color.Gray)
            Text(sdf.format(Date(expense.date)).split(" ")[1], fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.padding(8.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(expense.description, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            val splitText = buildString {
                append("Paid by $payerName")
                if (expense.involvedUserIds.isNotEmpty()) {
                    append(" • Split with ")
                    val isInvolved = expense.involvedUserIds.contains(currentUserId)
                    if (isInvolved) {
                        append("you")
                        if (expense.involvedUserIds.size > 1) {
                            append(" and ${expense.involvedUserIds.size - 1} others")
                        }
                    } else {
                        append("${expense.involvedUserIds.size} members")
                    }
                }
            }
            Text(splitText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(String.format(Locale.US, "₹%.2f", expense.amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}
