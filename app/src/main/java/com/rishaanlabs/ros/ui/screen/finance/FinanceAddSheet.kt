package com.rishaanlabs.ros.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rishaanlabs.ros.data.local.entity.FinanceAccountType
import com.rishaanlabs.ros.data.local.entity.FinanceTransactionType
import com.rishaanlabs.ros.data.local.entity.LoanInterestMethod
import com.rishaanlabs.ros.data.local.entity.SavingsGoalType
import java.time.LocalDate

private enum class AddFinanceKind { ACCOUNT, TRANSACTION, GOAL, LOAN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAddSheet(
    state: FinanceUiState,
    onDismiss: () -> Unit,
    onAddAccount: (String, String, FinanceAccountType, String, String) -> Unit,
    onAddTransaction: (FinanceTransactionType, String, String, String?, String?, String, String) -> Unit,
    onAddGoal: (String, SavingsGoalType, String, String, LocalDate?, String) -> Unit,
    onAddLoan: (String, String, String, String, String, String, String, LoanInterestMethod) -> Unit
) {
    var kind by remember { mutableStateOf(AddFinanceKind.TRANSACTION) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AddFinanceKind.entries.forEach { option ->
                    FilterChip(
                        selected = kind == option,
                        onClick = { kind = option },
                        label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            when (kind) {
                AddFinanceKind.ACCOUNT -> AddAccountForm(onSave = { a, b, c, d, e -> onAddAccount(a, b, c, d, e); onDismiss() })
                AddFinanceKind.TRANSACTION -> AddTransactionForm(state, onSave = { a, b, c, d, e, f, g -> onAddTransaction(a, b, c, d, e, f, g); onDismiss() })
                AddFinanceKind.GOAL -> AddGoalForm(onSave = { a, b, c, d, e, f -> onAddGoal(a, b, c, d, e, f); onDismiss() })
                AddFinanceKind.LOAN -> AddLoanForm(onSave = { a, b, c, d, e, f, g, h -> onAddLoan(a, b, c, d, e, f, g, h); onDismiss() })
            }
        }
    }
}

@Composable
private fun AddAccountForm(onSave: (String, String, FinanceAccountType, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("0") }
    var currency by remember { mutableStateOf("MVR") }
    var type by remember { mutableStateOf(FinanceAccountType.BANK) }
    Text("Add account")
    OutlinedTextField(name, { name = it }, label = { Text("Account name") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(institution, { institution = it }, label = { Text("Bank / institution") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(opening, { opening = it }, label = { Text("Current/opening balance") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(currency, { currency = it.uppercase() }, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(FinanceAccountType.BANK, FinanceAccountType.CASH, FinanceAccountType.SAVINGS).forEach { option ->
            FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option.name) })
        }
    }
    Button(onClick = { onSave(name, institution, type, currency, opening) }, enabled = name.isNotBlank()) { Text("Save account") }
}

@Composable
private fun AddTransactionForm(
    state: FinanceUiState,
    onSave: (FinanceTransactionType, String, String, String?, String?, String, String) -> Unit
) {
    var type by remember { mutableStateOf(FinanceTransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var accountId by remember(state.accounts) { mutableStateOf(state.accounts.firstOrNull()?.account?.id ?: "") }
    var destinationId by remember { mutableStateOf<String?>(null) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    Text("Add transaction")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(FinanceTransactionType.EXPENSE, FinanceTransactionType.INCOME, FinanceTransactionType.TRANSFER).forEach { option ->
            FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option.name) })
        }
    }
    OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
    Text("Account")
    state.accounts.forEach { row ->
        FilterChip(selected = accountId == row.account.id, onClick = { accountId = row.account.id }, label = { Text(row.account.name) })
    }
    if (type == FinanceTransactionType.TRANSFER) {
        Text("Destination")
        state.accounts.filter { it.account.id != accountId }.forEach { row ->
            FilterChip(selected = destinationId == row.account.id, onClick = { destinationId = row.account.id }, label = { Text(row.account.name) })
        }
    }
    if (type == FinanceTransactionType.EXPENSE) {
        Text("Category")
        state.categories.forEach { category ->
            FilterChip(selected = categoryId == category.id, onClick = { categoryId = category.id }, label = { Text(category.name) })
        }
        OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant / description") }, modifier = Modifier.fillMaxWidth())
    }
    OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
    Button(
        onClick = { onSave(type, amount, accountId, destinationId, categoryId, merchant, note) },
        enabled = amount.isNotBlank() && accountId.isNotBlank() && (type != FinanceTransactionType.TRANSFER || destinationId != null)
    ) { Text("Save transaction") }
}

@Composable
private fun AddGoalForm(onSave: (String, SavingsGoalType, String, String, LocalDate?, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SavingsGoalType.EMERGENCY) }
    var target by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("") }
    var targetDateText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("MVR") }
    Text("Add savings goal")
    OutlinedTextField(name, { name = it }, label = { Text("Goal name") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(SavingsGoalType.EMERGENCY, SavingsGoalType.MEDICAL, SavingsGoalType.TRAVEL, SavingsGoalType.STUDY).forEach { option ->
            FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option.name) })
        }
    }
    OutlinedTextField(target, { target = it }, label = { Text("Target amount") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(monthly, { monthly = it }, label = { Text("Planned monthly contribution") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(targetDateText, { targetDateText = it }, label = { Text("Target date YYYY-MM-DD (optional)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(currency, { currency = it.uppercase() }, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())
    val parsedTargetDate = targetDateText.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val dateValid = targetDateText.isBlank() || parsedTargetDate != null
    Button(onClick = { onSave(name, type, target, currency, parsedTargetDate, monthly) }, enabled = name.isNotBlank() && target.isNotBlank() && dateValid) { Text("Save goal") }
}

@Composable
private fun AddLoanForm(onSave: (String, String, String, String, String, String, String, LoanInterestMethod) -> Unit) {
    var name by remember { mutableStateOf("") }
    var lender by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("MVR") }
    var original by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(LoanInterestMethod.AMORTIZED_MONTHLY) }
    Text("Add loan")
    OutlinedTextField(name, { name = it }, label = { Text("Loan name") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(lender, { lender = it }, label = { Text("Lender") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(original, { original = it }, label = { Text("Original principal") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(current, { current = it }, label = { Text("Current principal") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(rate, { rate = it }, label = { Text("Annual interest %") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(payment, { payment = it }, label = { Text("Minimum monthly payment") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(currency, { currency = it.uppercase() }, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(LoanInterestMethod.AMORTIZED_MONTHLY, LoanInterestMethod.SIMPLE_DAILY, LoanInterestMethod.MANUAL).forEach { option ->
            FilterChip(selected = method == option, onClick = { method = option }, label = { Text(option.name.replace('_', ' ')) })
        }
    }
    Button(onClick = { onSave(name, lender, currency, original, current, rate, payment, method) }, enabled = name.isNotBlank() && original.isNotBlank() && payment.isNotBlank()) { Text("Save loan") }
}
