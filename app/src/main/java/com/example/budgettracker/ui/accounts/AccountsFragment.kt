package com.example.budgettracker.ui.accounts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Fragment for the Accounts UI.
 * Manages bank accounts, cash, and total balance views.
 */
@Composable
fun AccountsFragment() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Accounts Screen (Placeholder)")
    }
}
