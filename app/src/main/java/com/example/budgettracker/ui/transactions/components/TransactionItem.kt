package com.example.budgettracker.ui.transactions.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.ui.transactions.utils.TransactionDateUtils

private val TransferBlue = Color(0xFF2962FF)

/**
 * Reusable Transaction Item component.
 * Supports both standard transactions and consolidated transfers.
 */
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    overrideTitle: String? = null,
    isTransfer: Boolean = false
) {
    val title = overrideTitle ?: transaction.category
    
    val accentColor = when {
        isTransfer -> TransferBlue
        transaction.type == "INCOME" -> Color(0xFF2E7D32)
        else -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTransfer) 
                MaterialTheme.colorScheme.surfaceContainerHighest 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = if (isTransfer) MaterialTheme.shapes.large else MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (isTransfer) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = TransferBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                }
                
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isTransfer) TransferBlue else Color.Unspecified
                    )
                    
                    val detailText = if (isTransfer) {
                        "Internal Transfer • ${TransactionDateUtils.formatDate(transaction.timestamp)}"
                    } else {
                        "${transaction.type} • ${TransactionDateUtils.formatDate(transaction.timestamp)}"
                    }
                    
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTransfer) TransferBlue.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹%.2f".format(transaction.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = accentColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFB71C1C),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
