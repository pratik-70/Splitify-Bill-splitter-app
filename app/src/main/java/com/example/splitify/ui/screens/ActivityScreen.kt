package com.example.splitify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.splitify.model.Expense
import com.example.splitify.ui.components.SplitifyTopAppBar
import com.example.splitify.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(viewModel: MainViewModel, navController: NavHostController) {
    val expenses by viewModel.expenses.collectAsState()
    val usersMap by viewModel.usersMap.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val groupsMap = groups.associateBy { it.id }
    val currentUserId = currentUser?.id ?: ""

    Scaffold(
        topBar = {
            SplitifyTopAppBar(title = "Recent activity")
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (expenses.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No recent activity yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(expenses) { expense ->
                        val groupName = expense.groupId?.let { groupsMap[it]?.name }
                        val payerName = if (expense.paidBy == currentUserId) "You" else (usersMap[expense.paidBy]?.name ?: "Someone")
                        val receiverName = if (expense.isSettlement) {
                            val receiverId = expense.involvedUserIds.firstOrNull()
                            if (receiverId == currentUserId) "you" else (usersMap[receiverId]?.name ?: "someone")
                        } else null
                        
                        ActivityItem(
                            expense = expense,
                            payerName = payerName,
                            receiverName = receiverName,
                            groupName = groupName,
                            currentUserId = currentUserId
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ActivityItem(expense: Expense, payerName: String, receiverName: String?, groupName: String?, currentUserId: String) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    
    val isPayer = expense.paidBy == currentUserId
    val isInvolved = expense.involvedUserIds.contains(currentUserId)
    val share = if (expense.involvedUserIds.isNotEmpty()) expense.amount / expense.involvedUserIds.size else 0.0
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        ) {
            Icon(
                Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                expense.description, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val splitText = buildString {
                if (expense.isSettlement) {
                    append(payerName)
                    append(" settled up with ")
                    append(receiverName)
                    if (groupName != null) append(" in \"$groupName\"")
                } else {
                    append(payerName)
                    append(" paid")
                    if (groupName != null) append(" in \"$groupName\"")
                    if (expense.involvedUserIds.isNotEmpty()) {
                        append(" • Split with ")
                        if (expense.involvedUserIds.contains(currentUserId)) {
                            append("you")
                            if (expense.involvedUserIds.size > 1) {
                                append(" & ${expense.involvedUserIds.size - 1} others")
                            }
                        } else {
                            append("${expense.involvedUserIds.size} members")
                        }
                    }
                }
            }
            Text(
                splitText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
            Text(
                sdf.format(Date(expense.date)),
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            val (statusText, statusColor, displayAmount) = when {
                expense.isSettlement -> {
                    if (expense.paidBy == currentUserId) {
                        Triple("You sent", Color.Gray, expense.amount)
                    } else if (expense.involvedUserIds.contains(currentUserId)) {
                        Triple("You received", Color(0xFF17B890), expense.amount)
                    } else {
                        Triple("Settlement", Color.Gray, expense.amount)
                    }
                }
                isPayer -> {
                    val lent = if (isInvolved) expense.amount - share else expense.amount
                    if (lent > 0) {
                        Triple("You lent", Color(0xFF17B890), lent)
                    } else {
                        Triple("You paid", Color.Gray, expense.amount)
                    }
                }
                isInvolved -> {
                    Triple("You owe", Color(0xFFEF4444), share)
                }
                else -> {
                    Triple("Not involved", Color.Gray, 0.0)
                }
            }

            if (displayAmount > 0 || isPayer) {
                Text(
                    statusText,
                    fontSize = 11.sp,
                    color = statusColor
                )
                Text(
                    String.format(Locale.US, "₹%.2f", if (displayAmount > 0) displayAmount else expense.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
