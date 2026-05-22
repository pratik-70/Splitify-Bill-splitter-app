package com.example.splitify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.splitify.navigation.NavGraph
import com.example.splitify.navigation.Screen
import com.example.splitify.ui.screens.*
import com.example.splitify.ui.theme.SplitifyTheme
import com.example.splitify.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitifyTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        NavigationItem("Groups", Screen.Groups.route, Icons.Default.Groups),
        NavigationItem("Friends", Screen.Friends.route, Icons.Default.People),
        NavigationItem("Activity", Screen.Activity.route, Icons.Default.History),
        NavigationItem("Account", Screen.Account.route, Icons.Default.Person)
    )

    // Check if the current route is one of the pager-enabled screens
    val isPagerScreen = items.any { it.route == currentRoute }
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    // Sync Pager with NavController
    LaunchedEffect(currentRoute) {
        val index = items.indexOfFirst { it.route == currentRoute }
        if (index != -1 && pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
    }

    // Sync NavController with Pager
    LaunchedEffect(pagerState.currentPage) {
        val targetRoute = items[pagerState.currentPage].route
        if (currentRoute != targetRoute && isPagerScreen) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val isSystemScreen = currentRoute == Screen.Splash.route || 
                         currentRoute == Screen.Onboarding.route || 
                         currentRoute == Screen.Auth.route

    Scaffold(
        bottomBar = {
            if (!isSystemScreen && currentRoute != Screen.AddExpense.route) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    items.forEachIndexed { index, item ->
                        if (index == 2) Box(modifier = Modifier.weight(1f)) // Middle gap for FAB
                        
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = if (isPagerScreen) pagerState.currentPage == index 
                                      else navBackStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val hideFab = isSystemScreen || currentRoute == Screen.AddExpense.route
            
            if (!hideFab) {
                val groupId = navBackStackEntry?.arguments?.getString("groupId")
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddExpense.createRoute(groupId)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .offset(y = 50.dp)
                        .size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        if (isPagerScreen) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) { page ->
                when (items[page].route) {
                    Screen.Groups.route -> GroupsScreen(viewModel, navController)
                    Screen.Friends.route -> FriendsScreen(viewModel, navController)
                    Screen.Activity.route -> ActivityScreen(viewModel, navController)
                    Screen.Account.route -> AccountScreen(viewModel, navController)
                }
            }
        } else {
            NavGraph(
                navController = navController,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

data class NavigationItem(val label: String, val route: String, val icon: ImageVector)
