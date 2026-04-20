package com.example.budgettracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(260.dp).background(MaterialTheme.colorScheme.surface)
                ) {
                    // 1. Header with name
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Local User",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = { },
                        enabled = false,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 2. Edit Name
                    DropdownMenuItem(
                        text = { Text("Edit Name") },
                        onClick = {
                            expanded = false
                            onEditName()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )

                    // 3. Theme Switcher Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeChange(!isDarkMode) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Dark Mode",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onThemeChange(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // 4. Clear Chat (Destructive)
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = "Clear Finn Chat", 
                                color = MaterialTheme.colorScheme.error 
                            ) 
                        },
                        onClick = {
                            expanded = false
                            showClearChatDialog = true
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        }
                    )
                    
                    // 5. Logout
                    DropdownMenuItem(
                        text = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expanded = false
                            onLogout()
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.ExitToApp, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        }
                    )
                }
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
