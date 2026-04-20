package com.example.budgettracker.ui.transactions.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.ui.transactions.utils.TransactionDateUtils
import com.example.budgettracker.utils.Constants

data class CategoryOption(
    val name: String,
    val isCustom: Boolean = false
)

val defaultCategories = listOf(
    CategoryOption("Food"),
    CategoryOption("Bills"),
    CategoryOption("Groceries"),
    CategoryOption("Travel"),
    CategoryOption("Entertainment"),
    CategoryOption("Shopping"),
    CategoryOption("Health"),
    CategoryOption("Investments"),
    CategoryOption("Education"),
    CategoryOption("Other", isCustom = true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionForm(
    accounts: List<AccountEntity>,
    onSave: (TransactionEntity) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryOption?>(null) }
    var customCategoryText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Account Selection State
    var selectedAccountId by remember(accounts) { 
        mutableStateOf(
            accounts.find { it.id == Constants.DEFAULT_ACCOUNT_ID }?.id 
            ?: accounts.firstOrNull()?.id 
            ?: ""
        ) 
    }
    
    val selectedAccount = accounts.find { it.id == selectedAccountId }

    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Amount Field
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it; errorMessage = null },
            label = { Text("Amount") },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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
                            errorMessage = null
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        // Custom Category Field (Only visible when "Other" is selected)
        AnimatedVisibility(visible = selectedCategory?.isCustom == true) {
            OutlinedTextField(
                value = customCategoryText,
                onValueChange = { customCategoryText = it; errorMessage = null },
                label = { Text("Enter category name") },
                placeholder = { Text("e.g. Gift, Bonus") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        // Account Selector
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

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Save Button
        Button(
            onClick = {
                val amount = amountText.toDoubleOrNull()
                val finalCategory = if (selectedCategory?.isCustom == true) {
                    customCategoryText
                } else {
                    selectedCategory?.name
                }

                if (amount != null && amount > 0 && !finalCategory.isNullOrBlank() && selectedAccount != null) {
                    onSave(
                        TransactionEntity(
                            amount = amount,
                            type = selectedType,
                            category = finalCategory,
                            accountId = selectedAccountId,
                            accountName = selectedAccount.accountName,
                            source = "MANUAL",
                            timestamp = selectedDate
                        )
                    )
                    amountText = ""
                    selectedCategory = null
                    customCategoryText = ""
                    selectedDate = System.currentTimeMillis()
                    errorMessage = null
                } else if (selectedAccount == null) {
                    errorMessage = "Please create an account first"
                } else {
                    errorMessage = "Please enter valid amount and category"
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                "Save Transaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
