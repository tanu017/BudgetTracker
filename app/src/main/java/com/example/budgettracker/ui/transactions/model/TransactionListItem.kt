package com.example.budgettracker.ui.transactions.model

import com.example.budgettracker.data.local.entities.TransactionEntity

/**
 * A sealed class to represent different types of items in the transaction list.
 * This allows us to consolidate internal transfers into a single visual row.
 */
sealed class TransactionListItem {
    abstract val timestamp: Long
    abstract val amount: Double

    data class Regular(val transaction: TransactionEntity) : TransactionListItem() {
        override val timestamp = transaction.timestamp
        override val amount = transaction.amount
    }

    data class Transfer(
        val id: Long,
        override val amount: Double,
        val fromAccount: String,
        val toAccount: String,
        override val timestamp: Long,
        // Keep reference to the actual entities for deletion
        val sourceEntity: TransactionEntity,
        val destinationEntity: TransactionEntity
    ) : TransactionListItem()
}
