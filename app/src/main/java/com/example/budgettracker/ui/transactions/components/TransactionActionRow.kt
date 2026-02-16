package com.example.budgettracker.ui.transactions.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TransactionActionRow(onPasteClick: () -> Unit) {
    OutlinedButton(
        onClick = onPasteClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Icon(Icons.Default.Email, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Paste Email / SMS")
    }
}
