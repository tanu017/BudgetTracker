package com.example.budgettracker.ui.transactions.engine

import com.example.budgettracker.data.local.entities.TransactionEntity
import com.example.budgettracker.ui.transactions.model.TransactionListItem

/**
 * Presentation-layer engine to consolidate double-entry ledger records
 * into a single unified UI model for transfers.
 */
object TransactionConsolidationEngine {

    fun consolidate(
        transactions: List<TransactionEntity>
    ): List<TransactionListItem> {
        val result = mutableListOf<TransactionListItem>()
        val visited = mutableSetOf<Long>()

        // Ensure we process latest transactions first
        val sorted = transactions.sortedByDescending { it.timestamp }

        for (tx in sorted) {
            if (visited.contains(tx.id)) continue

            if (tx.type == "TRANSFER") {
                // We anchor the consolidation on the "OUT" direction to ensure From -> To consistency
                if (tx.transferDirection == "OUT") {
                    val pair = sorted.firstOrNull {
                        it.type == "TRANSFER" &&
                        it.transferDirection == "IN" &&
                        it.amount == tx.amount &&
                        it.accountName == tx.relatedAccountName &&
                        it.relatedAccountName == tx.accountName &&
                        !visited.contains(it.id)
                    }

                    if (pair != null) {
                        result.add(
                            TransactionListItem.Transfer(
                                id = tx.id,
                                amount = tx.amount,
                                fromAccount = tx.accountName,
                                toAccount = pair.accountName,
                                timestamp = tx.timestamp,
                                sourceEntity = tx,
                                destinationEntity = pair
                            )
                        )
                        visited.add(tx.id)
                        visited.add(pair.id)
                        continue
                    }
                }

                // Safety: prevent orphan IN rendering or handle unpaired OUTs
                if (tx.transferDirection == "IN") {
                    // Skip orphan IN rows. They should only render via OUT anchor.
                    visited.add(tx.id)
                } else {
                    result.add(TransactionListItem.Regular(tx))
                    visited.add(tx.id)
                }
            } else {
                result.add(TransactionListItem.Regular(tx))
                visited.add(tx.id)
            }
        }

        return result
    }
}
