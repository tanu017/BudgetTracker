package com.example.budgettracker.ui.chatbot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.parser.EmailParser
import com.example.budgettracker.parser.ParsedTransaction
import com.example.budgettracker.parser.toTransactionEntity
import com.example.budgettracker.repository.AccountRepository
import com.example.budgettracker.repository.CategoryRepository
import com.example.budgettracker.repository.ReminderRepository
import com.example.budgettracker.repository.TransactionRepository
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.TransactionViewModel

/**
 * Test Screen for the Email Parsing Engine.
 * Allows users to paste raw text and verify the extraction logic.
 */
@Composable
fun EmailParserTestScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    
    // Repositories and ViewModel setup for saving functionality
    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }

    val viewModel: TransactionViewModel = viewModel(
        factory = BudgetViewModelFactory(
            transactionRepo, accountRepo, categoryRepo, reminderRepo
        )
    )

    var rawText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ParsedTransaction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Email Parsing Engine",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Paste bank notification text below to test extraction:",
            fontSize = 14.sp,
            color = Color.Gray
        )

        OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            placeholder = { Text("e.g. Your A/c XX1234 is debited for Rs. 1,500.00 at Amazon...") },
            shape = MaterialTheme.shapes.medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { result = EmailParser.parseEmail(rawText) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Parse Text")
            }
            
            OutlinedButton(
                onClick = { 
                    rawText = ""
                    result = null 
                },
                modifier = Modifier.weight(0.5f)
            ) {
                Text("Clear")
            }
        }

        HorizontalDivider()

        if (result != null) {
            ParsedResultCard(
                data = result!!,
                onSave = {
                    viewModel.insertTransaction(result!!.toTransactionEntity())
                    // Reset after saving
                    rawText = ""
                    result = null
                }
            )
        } else if (rawText.isNotEmpty()) {
            Text(
                text = "No transaction data detected in the text above.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ParsedResultCard(data: ParsedTransaction, onSave: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Extraction Successful",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ResultDetailRow("Amount", "₹%.2f".format(data.amount))
                ResultDetailRow("Type", data.type)
                ResultDetailRow("Category", data.category)
                ResultDetailRow("Merchant", data.merchant ?: "Unknown")
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Save to Transactions")
            }
        }
    }
}

@Composable
fun ResultDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Medium, color = Color.DarkGray)
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}
