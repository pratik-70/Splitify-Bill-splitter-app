package com.example.splitify.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.splitify.ui.screens.*
import com.example.splitify.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Groups : Screen("groups")
    object CreateGroup : Screen("create_group")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    object Friends : Screen("friends")
    object Activity : Screen("activity")
    object Account : Screen("account")
    object AddExpense : Screen("add_expense?groupId={groupId}") {
        fun createRoute(groupId: String? = null) = if (groupId != null) "add_expense?groupId=$groupId" else "add_expense"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController, viewModel)
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController)
        }
        composable(Screen.Auth.route) {
            AuthScreen(viewModel, navController)
        }
        composable(Screen.Groups.route) {
            GroupsScreen(viewModel, navController)
        }
        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(viewModel, navController)
        }
        composable(Screen.GroupDetail.route) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            GroupDetailScreen(groupId, viewModel, navController)
        }
        composable(Screen.Friends.route) {
            FriendsScreen(viewModel, navController)
        }
        composable(Screen.Activity.route) {
            ActivityScreen(viewModel, navController)
        }
        composable(Screen.Account.route) {
            AccountScreen(viewModel, navController)
        }
        composable(Screen.AddExpense.route) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            AddExpenseScreen(viewModel, navController, groupId)
        }
    }
}
