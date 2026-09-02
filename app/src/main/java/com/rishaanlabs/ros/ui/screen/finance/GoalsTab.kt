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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rishaanlabs.ros.domain.finance.FinanceCalculator
import com.rishaanlabs.ros.domain.finance.formatMoney

@Composable
fun GoalsTab(
    state: FinanceUiState,
    contentPadding: PaddingValues,
    onAllocate: (String, String, String?, String) -> Unit,
    onOpen: (FinanceTarget) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Savings funds", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Text(
                "Money set aside here still sits in your accounts. Use Edit to change a target " +
                    "or remove a goal, and a negative amount to release money back.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.goals.isEmpty()) item { Text("Create Emergency, Medical, Travel, Study or any custom goal.") }
        items(state.goals.size) { index ->
            val row = state.goals[index]
            val projection = FinanceCalculator.goalProjection(
                row.currentMinor, row.goal.targetMinor, row.goal.monthlyPlannedMinor,
                targetDate = row.goal.targetDate
            )
            var allocation by remember(row.goal.id) { mutableStateOf("") }
            Card(Modifier.fillMaxWidth()) {
                Text(row.goal.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 2.dp))
                Text(row.goal.type.name.replace('_', ' '), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    "${formatMoney(row.currentMinor, row.goal.currency)} of ${formatMoney(row.goal.targetMinor, row.goal.currency)}",
                    modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp)
                )
                LinearProgressIndicator(
                    progress = { projection.progress.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 8.dp)
                )
                projection.projectedCompletionDate?.let {
                    Text("Projected completion: $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
                }
                projection.requiredMonthlyMinor?.let {
                    Text("Required for deadline: ${formatMoney(it, row.goal.currency)} / month", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
                }
                OutlinedTextField(
                    value = allocation,
                    onValueChange = { allocation = it },
                    label = { Text("Add to goal (negative releases)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 8.dp)
                )
                Row(
                    modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (allocation.isNotBlank()) {
                                onAllocate(row.goal.id, allocation, null, "Manual allocation")
                                allocation = ""
                            }
                        }
                    ) { Text("Allocate") }
                    TextButton(onClick = { onOpen(FinanceTarget.Goal(row.goal)) }) { Text("Edit") }
                }
            }
        }
    }
}
