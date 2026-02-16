package com.example.budgettracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.CategoryRepository
import com.example.budgettracker.repository.ReminderRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.ui.accounts.AccountsFragment
import com.example.budgettracker.ui.dashboard.DashboardFragment
import com.example.budgettracker.ui.home.HomeScreen
import com.example.budgettracker.ui.security.AppLockGate
import com.example.budgettracker.ui.transactions.TransactionScreen
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.DashboardViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppLockGate {
                BudgetTrackerApp()
            }
        }
    }
}

sealed class Screen(val route: String, val labelId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", R.string.title_home, Icons.Default.Home)
    object Transactions : Screen("transactions", R.string.title_transactions, Icons.Default.List)
    object Accounts : Screen("accounts", R.string.title_accounts, Icons.Default.AccountBalance)
    object Dashboard : Screen("dashboard", R.string.title_dashboard, Icons.Default.Dashboard)
}

@Composable
fun BudgetTrackerApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    
    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }

    val factory = BudgetViewModelFactory(transactionRepo, accountRepo, categoryRepo, reminderRepo)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)

    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
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
                        label = { Text(stringResource(screen.labelId)) },
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel = dashboardViewModel) }
            composable(Screen.Transactions.route) { TransactionScreen() }
            composable(Screen.Accounts.route) { AccountsFragment() }
            composable(Screen.Dashboard.route) { DashboardFragment(viewModel = dashboardViewModel) }
        }
    }
}
