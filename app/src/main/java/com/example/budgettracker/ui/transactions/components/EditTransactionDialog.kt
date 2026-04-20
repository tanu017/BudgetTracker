package com.example.budgettracker.ui.transactions.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.repository.*
import com.example.budgettracker.ui.transactions.utils.TransactionDateUtils
import com.example.budgettracker.viewmodel.BudgetViewModelFactory
import com.example.budgettracker.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    
    val transactionRepo = remember { TransactionRepository(database.transactionDao()) }
    val accountRepo = remember { AccountRepository(database.accountDao()) }
    val categoryRepo = remember { CategoryRepository(database.categoryDao()) }
    val reminderRepo = remember { ReminderRepository(database.reminderDao()) }
    val chatRepo = remember { ChatRepository(database.chatDao()) }

    val viewModel: TransactionViewModel = viewModel(
        factory = BudgetViewModelFactory(
            transactionRepo,
            accountRepo,
            categoryRepo,
            reminderRepo,
            chatRepo
        )
    )

    val accounts by viewModel.allAccounts.observeAsState(initial = emptyList())

    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    
    // Category state with pre-fill logic
    var selectedCategory by remember { 
        mutableStateOf(
            defaultCategories.find { it.name == transaction.category } 
            ?: defaultCategories.find { it.isCustom }
        ) 
    }
    var customCategoryText by remember { 
        mutableStateOf(if (selectedCategory?.isCustom == true) transaction.category else "") 
    }
    var expanded by remember { mutableStateOf(false) }

    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedDate by remember { mutableStateOf(transaction.timestamp) }
    
    var selectedAccountId by remember { mutableStateOf(transaction.accountId) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: transaction.timestamp
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Transaction",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    )
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        placeholder = { Text("Select a category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        defaultCategories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    selectedCategory = option
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // Custom Category Field
                AnimatedVisibility(visible = selectedCategory?.isCustom == true) {
                    OutlinedTextField(
                        value = customCategoryText,
                        onValueChange = { customCategoryText = it },
                        label = { Text("Enter category name") },
                        placeholder = { Text("e.g. Gift, Bonus") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )
                }

                // Account Dropdown
                AccountDropdown(
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onAccountSelected = { account ->
                        selectedAccountId = account.id
                    }
                )

                // Date Field
                Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                    OutlinedTextField(
                        value = TransactionDateUtils.formatDate(selectedDate),
                        onValueChange = { },
                        label = { Text("Date") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = Color.Transparent
                        )
                    )
                }

                // Income/Expense Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val incomeSelected = selectedType == "INCOME"
                    val expenseSelected = selectedType == "EXPENSE"

                    Surface(
                        onClick = { selectedType = "INCOME" },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        color = if (incomeSelected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (incomeSelected) Color(0xFF4CAF50).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Income",
                                color = if (incomeSelected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (incomeSelected) FontWeight.SemiBold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Surface(
                        onClick = { selectedType = "EXPENSE" },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        color = if (expenseSelected) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (expenseSelected) Color(0xFFEF5350).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Expense",
                                color = if (expenseSelected) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (expenseSelected) FontWeight.SemiBold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val finalCategory = if (selectedCategory?.isCustom == true) {
                                customCategoryText
                            } else {
                                selectedCategory?.name ?: transaction.category
                            }
                            
                            onSave(
                                transaction.copy(
                                    amount = amountText.toDoubleOrNull() ?: transaction.amount,
                                    category = finalCategory,
                                    type = selectedType,
                                    timestamp = selectedDate,
                                    accountId = selectedAccountId,
                                    accountName = selectedAccount?.accountName ?: transaction.accountName
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Save Changes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
