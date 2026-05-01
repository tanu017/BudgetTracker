package com.example.budgettracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    name: String,
    isDarkMode: Boolean,
    onEditName: () -> Unit,
    onLogout: () -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onClearChatClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Clear Chat Confirmation Dialog
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear Finn Chat?") },
            text = { Text("This will permanently delete all messages with Finn. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearChatClick()
                        showClearChatDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Budget Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = getGreeting(name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            Box {
                UserAvatar(
                    name = name,
                    onClick = { expanded = true }
                )
                
                ProfileDropdownMenu(
                    expanded = expanded,
                    onDismiss = { expanded = false },
                    name = name,
                    isDarkMode = isDarkMode,
                    onEditName = onEditName,
                    onThemeChange = onThemeChange,
                    onClearChat = { showClearChatDialog = true },
                    onLogout = { showLogoutDialog = true }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

fun getGreeting(name: String): String {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val emoji = when (hour) {
        in 5..11 -> "👋"
        in 12..16 -> "☀️"
        else -> "🌙"
    }
    return "$greeting, $name $emoji"
}
