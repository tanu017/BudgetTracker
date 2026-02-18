package com.example.budgettracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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

/**
 * MainActivity - Entry point for FinFlow.
 * Enhanced with premium navigation animations and edge-to-edge support.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
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
    val navItems = listOf(Screen.Home, Screen.Transactions, Screen.Accounts, Screen.Dashboard)

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                navItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    
                    // micro-UX: Animated scaling for selected icon
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.2f else 1.0f,
                        label = "iconScale"
                    )

                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = screen.icon, 
                                contentDescription = null,
                                modifier = Modifier.scale(iconScale)
                            ) 
                        },
                        label = { Text(stringResource(screen.labelId)) },
                        selected = selected,
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
        // Premium transition: Smooth fade between tabs
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel = dashboardViewModel) }
            composable(Screen.Transactions.route) { TransactionScreen() }
            composable(Screen.Accounts.route) { AccountsFragment() }
            composable(Screen.Dashboard.route) { DashboardFragment(viewModel = dashboardViewModel) }
        }
    }
}
