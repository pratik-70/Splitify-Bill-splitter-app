package com.example.splitify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.splitify.model.Group
import com.example.splitify.navigation.Screen
import com.example.splitify.ui.components.SplitifySummaryCard
import com.example.splitify.ui.components.SplitifyTopAppBar
import com.example.splitify.viewmodel.MainViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(viewModel: MainViewModel, navController: NavHostController) {
    val groups by viewModel.groups.collectAsState()
    val totalOwed by viewModel.totalYouAreOwed.collectAsState()
    val totalOwe by viewModel.totalYouOwe.collectAsState()
    val usersMap by viewModel.usersMap.collectAsState()

    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredGroups = if (searchQuery.isEmpty()) {
        groups
    } else {
        groups.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            if (isSearchMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            isSearchMode = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search groups...", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.onPrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            singleLine = true
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                }
            } else {
                SplitifyTopAppBar(
                    title = "Splitify",
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.CreateGroup.route) }) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
                        }
                        IconButton(onClick = { isSearchMode = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isSearchMode) {
                SplitifySummaryCard(totalOwe = totalOwe, totalOwed = totalOwed)
            }

            if (filteredGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isEmpty()) "You have no groups yet." else "No groups found matching \"$searchQuery\"", 
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredGroups) { group ->
                        GroupItem(
                            group,
                            currentUserId = viewModel.currentUser.value?.id ?: "",
                            usersMap = usersMap,
                            onClick = {
                                navController.navigate(Screen.GroupDetail.createRoute(group.id))
                            }
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
    }
}

@Composable
fun TabItem(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable { onClick() }.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.height(2.dp).width(40.dp).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
fun GroupItem(group: Group, currentUserId: String, usersMap: Map<String, com.example.splitify.model.User>, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            val memberNames = group.members.mapNotNull { usersMap[it]?.name?.split(" ")?.firstOrNull() ?: "User" }
            val memberString = if (memberNames.size > 3) {
                memberNames.take(3).joinToString(", ") + "..."
            } else {
                memberNames.joinToString(", ")
            }
            Text(memberString, fontSize = 12.sp, color = Color.Gray)
        }
        Column(horizontalAlignment = Alignment.End) {
            val userBalance = group.balances[currentUserId] ?: 0.0
            Text(
                if (userBalance >= 0) "you are owed" else "you owe",
                fontSize = 12.sp,
                color = if (userBalance >= 0) Color(0xFF17B890) else Color(0xFFEF4444)
            )
            Text(
                String.format(java.util.Locale.US, "₹%.2f", abs(userBalance)),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (userBalance >= 0) Color(0xFF17B890) else Color(0xFFEF4444)
            )
        }
    }
}
