package com.example.budgettracker.ui.transactions.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
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

/**
 * Updated Transaction Item showing the account name.
 */
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    overrideTitle: String? = null,
    isTransfer: Boolean = false,
    showDelete: Boolean = true
) {
    val title = overrideTitle ?: transaction.category
    val dateString = TransactionDateUtils.formatDate(transaction.timestamp)
    
    val amountColor = when {
        isTransfer -> MaterialTheme.colorScheme.onSurface
        transaction.type == "INCOME" -> Color(0xFF2E7D32)
        else -> Color(0xFFC62828)
    }

    // Showing Account Name in subtitle as per Step 7
    val subtitle = if (isTransfer) {
        "Transfer • $dateString"
    } else {
        "${transaction.accountName} • $dateString"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.weight(1f)
            ) {
                if (isTransfer) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Transfer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                }
                
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹%.2f".format(transaction.amount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = amountColor,
                    modifier = Modifier.padding(end = if (showDelete) 8.dp else 0.dp)
                )
                
                if (showDelete) {
                    IconButton(
                        onClick = onDelete, 
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
