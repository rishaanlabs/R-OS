package com.rishaanlabs.ros.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rishaanlabs.ros.data.local.entity.FinanceTransactionType
import com.rishaanlabs.ros.domain.finance.formatMoney

@Composable
fun TransactionsTab(state: FinanceUiState, contentPadding: PaddingValues) {
    val accountNames = state.accounts.associate { it.account.id to it.account.name }
    val categoryNames = state.categories.associate { it.id to it.name }
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Transactions", style = MaterialTheme.typography.headlineSmall) }
        if (state.transactions.isEmpty()) item { Text("Record income, expenses and transfers here.") }
        items(state.transactions.size) { index ->
            val tx = state.transactions[index]
            val sign = when (tx.type) {
                FinanceTransactionType.INCOME -> "+"
                FinanceTransactionType.EXPENSE, FinanceTransactionType.DEBT_PAYMENT -> "−"
                FinanceTransactionType.TRANSFER -> "↔"
            }
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text(
                            tx.merchant.ifBlank {
                                when (tx.type) {
                                    FinanceTransactionType.EXPENSE -> categoryNames[tx.categoryId] ?: "Expense"
                                    FinanceTransactionType.INCOME -> "Income"
                                    FinanceTransactionType.TRANSFER -> "Transfer"
                                    FinanceTransactionType.DEBT_PAYMENT -> "Debt payment"
                                }
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${accountNames[tx.accountId] ?: "Account"} · ${tx.occurredAt.toLocalDate()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("$sign ${formatMoney(tx.amountMinor, tx.currency)}", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
