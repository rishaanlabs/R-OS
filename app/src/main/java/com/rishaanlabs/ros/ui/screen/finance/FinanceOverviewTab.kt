package com.rishaanlabs.ros.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rishaanlabs.ros.domain.finance.FinanceCalculator
import com.rishaanlabs.ros.domain.finance.formatMoney

@Composable
fun FinanceOverviewTab(state: FinanceUiState, contentPadding: PaddingValues) {
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
            Text("Financial snapshot", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Metric("Cash", formatMoney(state.totalCashMinor, state.selectedCurrency))
                    Metric("Free", formatMoney(state.freeToAllocateMinor, state.selectedCurrency))
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Metric("Income", formatMoney(state.monthlyTotals.incomeMinor, state.selectedCurrency))
                    Metric("Expenses", formatMoney(state.monthlyTotals.expenseMinor, state.selectedCurrency))
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Metric("Debt", formatMoney(state.monthlyTotals.debtPaymentMinor, state.selectedCurrency))
                    Metric("Net flow", formatMoney(state.monthlyTotals.netCashFlowMinor, state.selectedCurrency))
                }
            }
        }
        state.emergencyRunwayMonths?.let { runway ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text("Emergency runway", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp))
                    Text("%.1f months of essential spending".format(runway), modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp))
                }
            }
        }
        if (state.goals.isNotEmpty()) {
            item { Text("Goals", style = MaterialTheme.typography.titleMedium) }
            items(state.goals.size) { index ->
                val row = state.goals[index]
                val projection = FinanceCalculator.goalProjection(
                    currentMinor = row.currentMinor,
                    targetMinor = row.goal.targetMinor,
                    plannedMonthlyMinor = row.goal.monthlyPlannedMinor,
                    targetDate = row.goal.targetDate
                )
                Card(Modifier.fillMaxWidth()) {
                    Text(row.goal.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
                    Text(
                        "${formatMoney(row.currentMinor, row.goal.currency)} / ${formatMoney(row.goal.targetMinor, row.goal.currency)}",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LinearProgressIndicator(
                        progress = { projection.progress.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 6.dp)
                    )
                    val timing = when {
                        projection.remainingMinor == 0L -> "Target reached"
                        projection.projectedCompletionDate != null -> "Projected ${projection.projectedCompletionDate}"
                        projection.requiredMonthlyMinor != null -> "Need ${formatMoney(projection.requiredMonthlyMinor, row.goal.currency)} / month"
                        else -> "Set a monthly contribution to forecast completion"
                    }
                    Text(timing, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 14.dp))
                }
            }
        }
        if (state.categorySpend.isNotEmpty()) {
            item { Text("Top spending this month", style = MaterialTheme.typography.titleMedium) }
            items(minOf(5, state.categorySpend.size)) { index ->
                val spend = state.categorySpend[index]
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${spend.groupName} · ${spend.categoryName}")
                    Text(formatMoney(spend.amountMinor, state.selectedCurrency))
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    androidx.compose.foundation.layout.Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
