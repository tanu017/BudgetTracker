package com.example.budgettracker.ui.chatbot

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.repository.*
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.TransactionViewModel

@Composable
fun EmailParserTestScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    
    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }
    val chatRepo = remember { ChatRepository(database.chatDao()) }

    val factory = BudgetViewModelFactory(transactionRepo, accountRepo, categoryRepo, reminderRepo, chatRepo)
    val viewModel: TransactionViewModel = viewModel(factory = factory)

    var emailText by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = emailText,
            onValueChange = { emailText = it },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            placeholder = { Text("Paste email content here...") }
        )
        Button(onClick = { /* test parsing */ }) {
            Text("Parse & Save")
        }
    }
}
