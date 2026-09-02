package com.rishaanlabs.ros.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rishaanlabs.ros.data.local.entity.FinanceCategory
import com.rishaanlabs.ros.domain.finance.formatMoney

@Composable
fun PlanTab(
    state: FinanceUiState,
    contentPadding: PaddingValues,
    onSetBudget: (FinanceCategory, String) -> Unit
) {
    val currentByCategory = state.categorySpend.associateBy { it.categoryId }
    val previousByCategory = state.previousCategorySpend.associateBy { it.categoryId }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Monthly plan", style = MaterialTheme.typography.headlineSmall) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Text("Planned goal contributions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 2.dp))
                Text(formatMoney(state.plannedGoalContributionsMinor, state.selectedCurrency), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 4.dp))
                Text(
                    "Income this month: ${formatMoney(state.monthlyTotals.incomeMinor, state.selectedCurrency)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 14.dp)
                )
            }
        }
        item { Text("Spending limits", style = MaterialTheme.typography.titleMedium) }
        items(state.categories.size, key = { state.categories[it].id }) { index ->
            val category = state.categories[index]
            val current = currentByCategory[category.id]?.amountMinor ?: 0L
            val previous = previousByCategory[category.id]?.amountMinor ?: 0L
            var budgetInput by remember(category.id, category.monthlyBudgetMinor) {
                mutableStateOf(category.monthlyBudgetMinor?.let { (it / 100.0).toString() } ?: "")
            }
            val budget = category.monthlyBudgetMinor
            Card(Modifier.fillMaxWidth()) {
                Text("${category.groupName} · ${category.name}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 2.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("This month ${formatMoney(current, state.selectedCurrency)}", style = MaterialTheme.typography.bodySmall)
                    Text("Last month ${formatMoney(previous, state.selectedCurrency)}", style = MaterialTheme.typography.bodySmall)
                }
                if (budget != null && budget > 0L) {
                    val progress = (current.toDouble() / budget.toDouble()).coerceIn(0.0, 1.0).toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 2.dp)
                    )
                    Text(
                        if (current > budget) "Over by ${formatMoney(current - budget, state.selectedCurrency)}" else "Remaining ${formatMoney(budget - current, state.selectedCurrency)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
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
            }
        }
    }
}
