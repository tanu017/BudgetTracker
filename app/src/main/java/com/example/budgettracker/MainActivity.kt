package com.example.budgettracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.budgettracker.repository.*
import com.example.budgettracker.ui.accounts.AccountsFragment
import com.example.budgettracker.ui.chatbot.ChatScreen
import com.example.budgettracker.ui.dashboard.DashboardFragment
import com.example.budgettracker.ui.home.HomeScreen
import com.example.budgettracker.ui.security.AppLockGate
import com.example.budgettracker.ui.transactions.TransactionScreen
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.ChatViewModel
import com.example.budgettracker.viewmodel.DashboardViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // STEP 2 — Ensure default system behavior
        WindowCompat.setDecorFitsSystemWindows(window, true)
        
        setContent {
            AppLockGate {
                // STEP 3 — Fix Compose root layout
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        BudgetTrackerApp()
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val labelId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", R.string.title_home, Icons.Default.Home)
    object Transactions : Screen("transactions", R.string.title_transactions, Icons.Default.List)
    object ChatBot : Screen("chatbot", R.string.title_chatbot, Icons.Default.Chat)
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
    val chatRepo = remember { ChatRepository(database.chatDao()) }

    val factory = BudgetViewModelFactory(transactionRepo, accountRepo, categoryRepo, reminderRepo, chatRepo)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val chatViewModel: ChatViewModel = viewModel(factory = factory)

    val navController = rememberNavController()
    val navItems = listOf(Screen.Home, Screen.Transactions, Screen.ChatBot, Screen.Accounts, Screen.Dashboard)

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.padding(top = 4.dp), // Removed windowInsetsPadding
                tonalElevation = 4.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                navItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1.0f,
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
                        label = { 
                            Text(
                                stringResource(screen.labelId),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding), // Use full innerPadding
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel = dashboardViewModel) }
            composable(Screen.Transactions.route) { TransactionScreen() }
            composable(Screen.ChatBot.route) { ChatScreen(viewModel = chatViewModel) }
            composable(Screen.Accounts.route) { AccountsFragment() }
            composable(Screen.Dashboard.route) { DashboardFragment(viewModel = dashboardViewModel) }
        }
    }
}
