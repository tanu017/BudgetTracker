package com.example.budgettracker.ui.transactions.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Updated Transaction Item with consistent STEP 6 Card styling.
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
    
    val amountColor = when {
        isTransfer -> MaterialTheme.colorScheme.secondary
        transaction.type == "INCOME" -> Color(0xFF2E7D32)
        else -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // STEP 6
        colors = CardDefaults.cardColors(
            containerColor = if (isTransfer) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp) // STEP 6: Consistent rounded corners
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp) // STEP 6: Reduced padding as per STEP 4
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                }
                
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isTransfer) TransferBlue else MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = if (isTransfer) "Transfer" else transaction.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹%.0f".format(transaction.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = amountColor,
                    modifier = Modifier.padding(end = if (showDelete) 4.dp else 0.dp)
                )
                
                if (showDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFB71C1C).copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
