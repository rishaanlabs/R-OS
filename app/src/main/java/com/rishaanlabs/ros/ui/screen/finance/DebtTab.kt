package com.rishaanlabs.ros.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.rishaanlabs.ros.data.local.entity.LoanInterestMethod
import com.rishaanlabs.ros.data.local.entity.LoanStatus
import com.rishaanlabs.ros.domain.finance.FinanceCalculator
import com.rishaanlabs.ros.domain.finance.formatMoney
import com.rishaanlabs.ros.domain.finance.parseMajorToMinor

@Composable
fun DebtTab(
    state: FinanceUiState,
    contentPadding: PaddingValues,
    onRecordPayment: (String, String, String, String, String, String, String) -> Unit
) {
    val active = state.loans.filter { it.loan.status == LoanStatus.ACTIVE && it.currentPrincipalMinor > 0L }
    val avalanche = active.maxWithOrNull(compareBy<com.rishaanlabs.ros.data.local.model.LoanProgressRow> { it.loan.annualInterestRateBps }.thenBy { -it.currentPrincipalMinor })
    val snowball = active.minByOrNull { it.currentPrincipalMinor }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Debt", style = MaterialTheme.typography.headlineSmall) }
        if (active.size > 1) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text("Payoff priorities", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
                    Text("Avalanche: ${avalanche?.loan?.name ?: "—"} (highest rate)", modifier = Modifier.padding(horizontal = 16.dp))
                    Text("Snowball: ${snowball?.loan?.name ?: "—"} (smallest balance)", modifier = Modifier.padding(16.dp, 2.dp, 16.dp, 14.dp))
                }
            }
        }
        if (state.loans.isEmpty()) item { Text("Add a loan to track principal, interest and payoff progress.") }
        items(state.loans.size) { index ->
            val row = state.loans[index]
            val account = state.accounts.firstOrNull { it.account.currency.equals(row.loan.currency, ignoreCase = true) }?.account
            val baseProjection = if (row.loan.interestMethod == LoanInterestMethod.AMORTIZED_MONTHLY) {
                FinanceCalculator.amortizedLoanProjection(
                    row.currentPrincipalMinor,
                    row.loan.annualInterestRateBps,
                    row.loan.minimumPaymentMinor
                )
            } else null
            var extra by remember(row.loan.id) { mutableStateOf("") }
            val extraMinor = runCatching { if (extra.isBlank()) 0L else parseMajorToMinor(extra) }.getOrDefault(0L)
            val whatIfProjection = if (extraMinor > 0L && row.loan.interestMethod == LoanInterestMethod.AMORTIZED_MONTHLY) {
                FinanceCalculator.amortizedLoanProjection(
                    row.currentPrincipalMinor,
                    row.loan.annualInterestRateBps,
                    row.loan.minimumPaymentMinor,
                    extraMinor
                )
            } else null
            var total by remember(row.loan.id) { mutableStateOf("") }
            var principal by remember(row.loan.id) { mutableStateOf("") }
            var interest by remember(row.loan.id) { mutableStateOf("") }
            Card(Modifier.fillMaxWidth()) {
                Text(row.loan.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 2.dp))
                Text(row.loan.lender, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
                Text("Balance ${formatMoney(row.currentPrincipalMinor, row.loan.currency)}", modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp))
                Text("Interest paid since tracking ${formatMoney(row.interestPaidMinor, row.loan.currency)}", modifier = Modifier.padding(horizontal = 16.dp))
                Text("Accrued / statement interest ${formatMoney(row.loan.manualAccruedInterestMinor, row.loan.currency)}", modifier = Modifier.padding(horizontal = 16.dp))
                baseProjection?.let {
                    Text("Projected payoff ${it.payoffDate} · future interest ${formatMoney(it.totalFutureInterestMinor, row.loan.currency)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp, 6.dp, 16.dp, 0.dp))
                    OutlinedTextField(extra, { extra = it }, label = { Text("What if I pay extra monthly?") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 2.dp))
                    if (whatIfProjection != null) {
                        val monthsSaved = (it.monthsRemaining - whatIfProjection.monthsRemaining).coerceAtLeast(0)
                        val interestSaved = (it.totalFutureInterestMinor - whatIfProjection.totalFutureInterestMinor).coerceAtLeast(0L)
                        Text(
                            "New payoff ${whatIfProjection.payoffDate} · $monthsSaved months earlier · save about ${formatMoney(interestSaved, row.loan.currency)} interest",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp, 4.dp, 16.dp, 0.dp)
                        )
                    }
                }
                if (account != null) {
                    OutlinedTextField(total, { total = it }, label = { Text("Payment total") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp))
                    OutlinedTextField(principal, { principal = it }, label = { Text("Principal") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
                    OutlinedTextField(interest, { interest = it }, label = { Text("Interest") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
                    Button(
                        onClick = {
                            if (total.isNotBlank()) {
                                onRecordPayment(row.loan.id, account.id, total, principal, interest, "0", "")
                                total = ""; principal = ""; interest = ""
                            }
                        },
                        modifier = Modifier.padding(16.dp, 4.dp, 16.dp, 16.dp)
                    ) { Text("Record payment") }
                }
            }
        }
    }
}
