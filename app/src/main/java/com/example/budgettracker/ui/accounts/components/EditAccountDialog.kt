package com.example.budgettracker.ui.accounts.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.budgettracker.data.local.entities.AccountEntity
import com.example.budgettracker.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDialog(
    account: AccountEntity,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    var name by remember { mutableStateOf(account.accountName) }
    var balanceText by remember { mutableStateOf(account.balance.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val isCashAccount = account.id == Constants.DEFAULT_ACCOUNT_ID

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (!isCashAccount) name = it },
                    label = { Text("Account Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCashAccount,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { 
                        balanceText = it
                        errorText = null
                    },
                    label = { Text("Current Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = errorText != null,
                    supportingText = { if (errorText != null) Text(errorText!!) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val balance = balanceText.toDoubleOrNull()
                            if (balance == null || balance < 0) {
                                errorText = "Balance must be 0 or positive"
                            } else if (name.isBlank()) {
                                errorText = "Name cannot be empty"
                            } else {
                                // Return updated AccountEntity as per Step 1
                                onSave(account.copy(accountName = name, balance = balance))
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
