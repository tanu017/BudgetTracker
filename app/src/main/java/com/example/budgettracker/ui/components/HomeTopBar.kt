package com.example.budgettracker.ui.components

import androidx.compose.foundation.background
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
    currentTheme: String,
    onEditName: () -> Unit,
    onLogout: () -> Unit,
    onThemeChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                    modifier = Modifier.width(220.dp).background(MaterialTheme.colorScheme.surface)
                ) {
                    // Header with name
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

                    // Theme Selection
                    DropdownMenuItem(
                        text = { Text("Light Theme") },
                        onClick = {
                            onThemeChange("light")
                            expanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null) },
                        trailingIcon = {
                            if (currentTheme == "light") {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Dark Theme") },
                        onClick = {
                            onThemeChange("dark")
                            expanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        trailingIcon = {
                            if (currentTheme == "dark") {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    DropdownMenuItem(
                        text = { Text("Edit Name") },
                        onClick = {
                            expanded = false
                            onEditName()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    
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
