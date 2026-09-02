package com.rishaanlabs.ros.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rishaanlabs.ros.data.local.entity.FinanceCategory
import com.rishaanlabs.ros.domain.finance.formatMoney
import com.rishaanlabs.ros.domain.finance.minorToEditableMajor

/**
 * The financial position, in the order it is actually asked for: what have I got, what has this
 * month done to it, and where is it going.
 *
 * Two things changed here after real use. The goal cards that used to be repeated in full are
 * now one line — they were an exact duplicate of the Savings tab, so the same numbers appeared
 * twice at different levels of detail and neither read as the authoritative one. And the monthly
 * limits, previously a whole separate "Plan" tab, sit against the spending they constrain, which
 * is the only place a limit means anything.
 */
@Composable
fun FinanceOverviewTab(
    state: FinanceUiState,
    contentPadding: PaddingValues,
    onSetBudget: (FinanceCategory, String) -> Unit
) {
    val currentByCategory = state.categorySpend.associateBy { it.categoryId }
    val previousByCategory = state.previousCategorySpend.associateBy { it.categoryId }
    var editingLimits by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Metric("Cash", formatMoney(state.totalCashMinor, state.selectedCurrency))
                    Metric("Free to use", formatMoney(state.freeToAllocateMinor, state.selectedCurrency))
                }
                if (state.assignedToGoalsMinor > 0L) {
                    Text(
                        "${formatMoney(state.assignedToGoalsMinor, state.selectedCurrency)} is earmarked for goals " +
                            "and still sits in your accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 14.dp)
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "This month",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 6.dp)
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Metric("Income", formatMoney(state.monthlyTotals.incomeMinor, state.selectedCurrency))
                    Metric("Expenses", formatMoney(state.monthlyTotals.expenseMinor, state.selectedCurrency))
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Metric("Debt paid", formatMoney(state.monthlyTotals.debtPaymentMinor, state.selectedCurrency))
                    Metric("Net flow", formatMoney(state.monthlyTotals.netCashFlowMinor, state.selectedCurrency))
                }
            }
        }

        state.emergencyRunwayMonths?.let { runway ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Emergency runway",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp)
                    )
                    Text(
                        "%.1f months of essential spending".format(runway),
                        modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp)
                    )
                }
            }
        }

        if (state.goals.isNotEmpty()) {
            item {
                val reached = state.goals.count { it.currentMinor >= it.goal.targetMinor }
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Savings",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp)
                    )
                    Text(
                        "${state.goals.size} goals · ${formatMoney(state.assignedToGoalsMinor, state.selectedCurrency)} set aside" +
                            if (reached > 0) " · $reached reached" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 14.dp)
                    )
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Spending this month", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { editingLimits = !editingLimits }) {
                    Text(if (editingLimits) "Done" else "Limits")
                }
            }
        }

        if (state.categorySpend.isEmpty() && !editingLimits) {
            item {
                Text(
                    "Nothing spent yet this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Editing limits shows every category, because a limit is usually set on something that
        // has not been spent on yet. Otherwise only what actually moved is worth listing.
        val rows = if (editingLimits) {
            state.categories
        } else {
            state.categories.filter { (currentByCategory[it.id]?.amountMinor ?: 0L) > 0L }
        }

        items(rows.size, key = { rows[it].id }) { index ->
            val category = rows[index]
            val current = currentByCategory[category.id]?.amountMinor ?: 0L
            val previous = previousByCategory[category.id]?.amountMinor ?: 0L
            val budget = category.monthlyBudgetMinor

            Card(Modifier.fillMaxWidth()) {
                Text(
                    "${category.groupName} · ${category.name}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 2.dp)
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "This month ${formatMoney(current, state.selectedCurrency)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Last month ${formatMoney(previous, state.selectedCurrency)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (budget != null && budget > 0L) {
                    LinearProgressIndicator(
                        progress = { (current.toDouble() / budget.toDouble()).coerceIn(0.0, 1.0).toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 2.dp)
                    )
                    Text(
                        if (current > budget) {
                            "Over by ${formatMoney(current - budget, state.selectedCurrency)}"
                        } else {
                            "Remaining ${formatMoney(budget - current, state.selectedCurrency)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (current > budget) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (editingLimits) {
                    var budgetInput by remember(category.id, category.monthlyBudgetMinor) {
                        mutableStateOf(category.monthlyBudgetMinor?.let { minorToEditableMajor(it) } ?: "")
                    }
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Monthly limit (blank = none)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 6.dp)
                    )
                    Button(
                        onClick = { onSetBudget(category, budgetInput) },
                        modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 14.dp)
                    ) { Text("Save limit") }
                } else {
                    Text("", modifier = Modifier.padding(bottom = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
