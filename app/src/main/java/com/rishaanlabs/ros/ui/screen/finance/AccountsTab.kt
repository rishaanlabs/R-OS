package com.rishaanlabs.ros.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rishaanlabs.ros.domain.finance.formatMoney

@Composable
fun AccountsTab(state: FinanceUiState, contentPadding: PaddingValues) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Where your money is", style = MaterialTheme.typography.headlineSmall) }
        if (state.accounts.isEmpty()) {
            item { Text("Add your first bank, cash or wallet account.") }
        }
        items(state.accounts.size) { index ->
            val row = state.accounts[index]
            Card(Modifier.fillMaxWidth()) {
                Text(row.account.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 2.dp))
                if (row.account.institution.isNotBlank()) {
                    Text(row.account.institution, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
                }
                Text(
                    formatMoney(row.balanceMinor, row.account.currency),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 14.dp)
                )
            }
        }
    }
}
