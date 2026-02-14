package com.example.budgettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.budgettracker.ui.accounts.AccountsFragment
import com.example.budgettracker.ui.dashboard.DashboardFragment
import com.example.budgettracker.ui.transactions.TransactionScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BudgetTrackerApp()
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Transactions : Screen("transactions", "Transactions", Icons.Default.List)
    object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalance)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTrackerApp() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Transactions,
        Screen.Accounts,
        Screen.Dashboard
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Transactions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Transactions.route) { TransactionScreen() }
            composable(Screen.Accounts.route) { AccountsFragment() }
            composable(Screen.Dashboard.route) { DashboardFragment() }
        }
    }
}
