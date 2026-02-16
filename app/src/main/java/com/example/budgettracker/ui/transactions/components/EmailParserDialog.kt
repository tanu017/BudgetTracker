package com.example.budgettracker.ui.transactions.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.parser.EmailParser
import com.example.budgettracker.parser.ParsedTransaction

@Composable
fun EmailParserDialog(onDismiss: () -> Unit, onTransactionParsed: (ParsedTransaction) -> Unit) {
    var rawText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ParsedTransaction?>(null) }
    var hasAttemptedParse by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste Email / SMS Content") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it; hasAttemptedParse = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("e.g. Your A/c XX1234 debited for ₹500 at Starbucks...") }
                )
                
                Button(
                    onClick = { 
                        result = EmailParser.parseEmail(rawText)
                        hasAttemptedParse = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan Content")
                }

                if (result != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                        ) {
                            Text("Transaction Found!", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Amount: ₹${result!!.amount}")
                            Text("Type: ${result!!.type}")
                            Text("Merchant: ${result!!.merchant ?: "Unknown"}")
                        }
                    }
                } else if (hasAttemptedParse) {
                    Text(
                        "No transaction detected. Check text and try again.",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            if (result != null) {
                Button(onClick = { onTransactionParsed(result!!) }) {
                    Text("Add to List")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
