package com.rishaanlabs.ros.ui.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.rishaanlabs.ros.data.local.entity.FinanceAccount
import com.rishaanlabs.ros.data.local.entity.FinanceAccountType
import com.rishaanlabs.ros.data.local.entity.FinanceTransaction
import com.rishaanlabs.ros.data.local.entity.FinanceTransactionType
import com.rishaanlabs.ros.data.local.entity.Loan
import com.rishaanlabs.ros.data.local.entity.LoanInterestMethod
import com.rishaanlabs.ros.data.local.entity.SavingsGoal
import com.rishaanlabs.ros.data.local.entity.SavingsGoalType
import com.rishaanlabs.ros.domain.finance.minorToEditableMajor
import java.time.LocalDate

/** The kinds of record the add sheet can create. */
enum class FinanceAddKind { ACCOUNT, TRANSACTION, GOAL, LOAN }

/**
 * Creates a finance record.
 *
 * [fixedKind] is what the tab's own add button passes: on Accounts the button adds an account and
 * asks nothing else. The kind picker only appears on Overview, where there is no obvious default.
 * Every other tab used to route through that picker, which put two extra decisions in front of
 * every single entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAddSheet(
    state: FinanceUiState,
    fixedKind: FinanceAddKind?,
    onDismiss: () -> Unit,
    onAddAccount: (String, String, FinanceAccountType, String, String) -> Unit,
    onAddTransaction: (FinanceTransactionType, String, String, String?, String?, String, String) -> Unit,
    onAddGoal: (String, SavingsGoalType, String, String, LocalDate?, String) -> Unit,
    onAddLoan: (String, String, String, String, String, String, String, LoanInterestMethod) -> Unit
) {
    var kind by remember { mutableStateOf(fixedKind ?: FinanceAddKind.TRANSACTION) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetBody {
            if (fixedKind == null) {
                ChipRow {
                    FinanceAddKind.entries.forEach { option ->
                        FilterChip(
                            selected = kind == option,
                            onClick = { kind = option },
                            label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
            when (kind) {
                FinanceAddKind.ACCOUNT -> AccountForm(null, "Add account") { a, b, c, d, e ->
                    onAddAccount(a, b, c, d, e); onDismiss()
                }
                FinanceAddKind.TRANSACTION -> TransactionForm(state, null, "Add transaction") { a, b, c, d, e, f, g ->
                    onAddTransaction(a, b, c, d, e, f, g); onDismiss()
                }
                FinanceAddKind.GOAL -> GoalForm(null, "Add goal") { a, b, c, d, e, f ->
                    onAddGoal(a, b, c, d, e, f); onDismiss()
                }
                FinanceAddKind.LOAN -> LoanForm(null, "Add loan") { a, b, c, d, e, f, g, h ->
                    onAddLoan(a, b, c, d, e, f, g, h); onDismiss()
                }
            }
        }
    }
}

/**
 * Opens an existing record so it can be corrected or removed.
 *
 * It reuses the same forms as creating, so an edit cannot produce a record that could not have
 * been created in the first place — the repository runs identical validation for both.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceEditSheet(
    state: FinanceUiState,
    target: FinanceTarget,
    onDismiss: () -> Unit,
    onSaveAccount: (FinanceAccount, String, String, FinanceAccountType, String, String) -> Unit,
    onSaveTransaction: (FinanceTransaction, FinanceTransactionType, String, String, String?, String?, String, String) -> Unit,
    onSaveGoal: (SavingsGoal, String, SavingsGoalType, String, String, LocalDate?, String) -> Unit,
    onSaveLoan: (Loan, String, String, String, String, String, String, String, LoanInterestMethod) -> Unit,
    onArchiveAccount: (FinanceAccount) -> Unit,
    onDelete: (FinanceTarget) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetBody {
            when (target) {
                is FinanceTarget.Account -> {
                    AccountForm(target.account, "Save changes") { a, b, c, d, e ->
                        onSaveAccount(target.account, a, b, c, d, e); onDismiss()
                    }
                    DangerRow(
                        deleteLabel = "Delete account",
                        onDelete = { onDelete(target); onDismiss() },
                        secondaryLabel = "Archive instead",
                        onSecondary = { onArchiveAccount(target.account); onDismiss() }
                    )
                }

                is FinanceTarget.Transaction -> {
                    TransactionForm(state, target.transaction, "Save changes") { a, b, c, d, e, f, g ->
                        onSaveTransaction(target.transaction, a, b, c, d, e, f, g); onDismiss()
                    }
                    DangerRow("Delete transaction", { onDelete(target); onDismiss() })
                }

                is FinanceTarget.Goal -> {
                    GoalForm(target.goal, "Save changes") { a, b, c, d, e, f ->
                        onSaveGoal(target.goal, a, b, c, d, e, f); onDismiss()
                    }
                    DangerRow("Delete goal", { onDelete(target); onDismiss() })
                }

                is FinanceTarget.LoanRecord -> {
                    LoanForm(target.loan, "Save changes") { a, b, c, d, e, f, g, h ->
                        onSaveLoan(target.loan, a, b, c, d, e, f, g, h); onDismiss()
                    }
                    DangerRow("Delete loan", { onDelete(target); onDismiss() })
                }
            }
        }
    }
}

@Composable
private fun SheetBody(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    // Wrapping rather than a single row: there are twenty-one categories, and stacking them one
    // per line turned choosing one into a scroll through the whole list.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
private fun DangerRow(
    deleteLabel: String,
    onDelete: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onDelete) {
            Text(deleteLabel, color = MaterialTheme.colorScheme.error)
        }
        if (secondaryLabel != null && onSecondary != null) {
            TextButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}

@Composable
private fun AccountForm(
    initial: FinanceAccount?,
    saveLabel: String,
    onSave: (String, String, FinanceAccountType, String, String) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var institution by remember(initial?.id) { mutableStateOf(initial?.institution ?: "") }
    var opening by remember(initial?.id) {
        mutableStateOf(initial?.let { minorToEditableMajor(it.openingBalanceMinor) } ?: "0")
    }
    var currency by remember(initial?.id) { mutableStateOf(initial?.currency ?: "MVR") }
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: FinanceAccountType.BANK) }

    Text(if (initial == null) "Add account" else "Edit account", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(name, { name = it }, label = { Text("Account name") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(institution, { institution = it }, label = { Text("Bank / institution") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(opening, { opening = it }, label = { Text("Opening balance") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(currency, { currency = it.uppercase() }, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())
    ChipRow {
        FinanceAccountType.entries.forEach { option ->
            FilterChip(
                selected = type == option,
                onClick = { type = option },
                label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
    Button(
        onClick = { onSave(name, institution, type, currency, opening) },
        enabled = name.isNotBlank()
    ) { Text(saveLabel) }
}

@Composable
private fun TransactionForm(
    state: FinanceUiState,
    initial: FinanceTransaction?,
    saveLabel: String,
    onSave: (FinanceTransactionType, String, String, String?, String?, String, String) -> Unit
) {
    var type by remember(initial?.id) {
        mutableStateOf(initial?.type ?: FinanceTransactionType.EXPENSE)
    }
    var amount by remember(initial?.id) {
        mutableStateOf(initial?.let { minorToEditableMajor(it.amountMinor) } ?: "")
    }
    var accountId by remember(initial?.id, state.accounts) {
        mutableStateOf(initial?.accountId ?: state.accounts.firstOrNull()?.account?.id ?: "")
    }
    var destinationId by remember(initial?.id) { mutableStateOf(initial?.destinationAccountId) }
    var categoryId by remember(initial?.id) { mutableStateOf(initial?.categoryId) }
    var merchant by remember(initial?.id) { mutableStateOf(initial?.merchant ?: "") }
    var note by remember(initial?.id) { mutableStateOf(initial?.note ?: "") }

    Text(if (initial == null) "Add transaction" else "Edit transaction", style = MaterialTheme.typography.titleMedium)
    ChipRow {
        listOf(
            FinanceTransactionType.EXPENSE,
            FinanceTransactionType.INCOME,
            FinanceTransactionType.TRANSFER
        ).forEach { option ->
            FilterChip(
                selected = type == option,
                onClick = { type = option },
                label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
    OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())

    Text("Account", style = MaterialTheme.typography.labelLarge)
    ChipRow {
        state.accounts.forEach { row ->
            FilterChip(
                selected = accountId == row.account.id,
                onClick = { accountId = row.account.id },
                label = { Text(row.account.name) }
            )
        }
    }

    if (type == FinanceTransactionType.TRANSFER) {
        Text("Destination", style = MaterialTheme.typography.labelLarge)
        ChipRow {
            state.accounts.filter { it.account.id != accountId }.forEach { row ->
                FilterChip(
                    selected = destinationId == row.account.id,
                    onClick = { destinationId = row.account.id },
                    label = { Text(row.account.name) }
                )
            }
        }
    }

    if (type == FinanceTransactionType.EXPENSE) {
        Text("Category", style = MaterialTheme.typography.labelLarge)
        ChipRow {
            state.categories.forEach { category ->
                FilterChip(
                    selected = categoryId == category.id,
                    onClick = { categoryId = if (categoryId == category.id) null else category.id },
                    label = { Text(category.name) }
                )
            }
        }
        OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant / description") }, modifier = Modifier.fillMaxWidth())
    }
    OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
    Button(
        onClick = {
            val destination = if (type == FinanceTransactionType.TRANSFER) destinationId else null
            val category = if (type == FinanceTransactionType.EXPENSE) categoryId else null
            onSave(type, amount, accountId, destination, category, merchant, note)
        },
        enabled = amount.isNotBlank() && accountId.isNotBlank() &&
            (type != FinanceTransactionType.TRANSFER || destinationId != null)
    ) { Text(saveLabel) }
}

@Composable
private fun GoalForm(
    initial: SavingsGoal?,
    saveLabel: String,
    onSave: (String, SavingsGoalType, String, String, LocalDate?, String) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: SavingsGoalType.EMERGENCY) }
    var target by remember(initial?.id) {
        mutableStateOf(initial?.let { minorToEditableMajor(it.targetMinor) } ?: "")
    }
    var monthly by remember(initial?.id) {
        mutableStateOf(initial?.let { minorToEditableMajor(it.monthlyPlannedMinor) } ?: "")
    }
    var targetDateText by remember(initial?.id) {
        mutableStateOf(initial?.targetDate?.toString() ?: "")
    }
    var currency by remember(initial?.id) { mutableStateOf(initial?.currency ?: "MVR") }

    Text(if (initial == null) "Add savings goal" else "Edit savings goal", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(name, { name = it }, label = { Text("Goal name") }, modifier = Modifier.fillMaxWidth())
    ChipRow {
        SavingsGoalType.entries.forEach { option ->
            FilterChip(
                selected = type == option,
                onClick = { type = option },
                label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
    OutlinedTextField(target, { target = it }, label = { Text("Target amount") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(monthly, { monthly = it }, label = { Text("Planned monthly contribution") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(targetDateText, { targetDateText = it }, label = { Text("Target date YYYY-MM-DD (optional)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(currency, { currency = it.uppercase() }, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())

    val parsedTargetDate = targetDateText.takeIf { it.isNotBlank() }
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val dateValid = targetDateText.isBlank() || parsedTargetDate != null
    if (!dateValid) {
        Text(
            "Use the form 2026-03-01.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    Button(
        onClick = { onSave(name, type, target, currency, parsedTargetDate, monthly) },
        enabled = name.isNotBlank() && target.isNotBlank() && dateValid
    ) { Text(saveLabel) }
}

@Composable
private fun LoanForm(
    initial: Loan?,
    saveLabel: String,
    onSave: (String, String, String, String, String, String, String, LoanInterestMethod) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var lender by remember(initial?.id) { mutableStateOf(initial?.lender ?: "") }
    var currency by remember(initial?.id) { mutableStateOf(initial?.currency ?: "MVR") }
    var original by remember(initial?.id) {
        mutableStateOf(initial?.let { minorToEditableMajor(it.originalPrincipalMinor) } ?: "")
    }
    var current by remember(initial?.id) {
        mutableStateOf(initial?.let { minorToEditableMajor(it.trackingStartPrincipalMinor) } ?: "")
    }
    var rate by remember(initial?.id) {
        mutableStateOf(initial?.let { (it.annualInterestRateBps / 100.0).toString() } ?: "")
    }
    var payment by remember(initial?.id) {
        mutableStateOf(initial?.let { minorToEditableMajor(it.minimumPaymentMinor) } ?: "")
    }
    var method by remember(initial?.id) {
        mutableStateOf(initial?.interestMethod ?: LoanInterestMethod.AMORTIZED_MONTHLY)
    }

    Text(if (initial == null) "Add loan" else "Edit loan", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(name, { name = it }, label = { Text("Loan name") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(lender, { lender = it }, label = { Text("Lender") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(original, { original = it }, label = { Text("Original principal") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(current, { current = it }, label = { Text("Current principal") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(rate, { rate = it }, label = { Text("Annual interest %") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(payment, { payment = it }, label = { Text("Minimum monthly payment") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(currency, { currency = it.uppercase() }, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())
    ChipRow {
        LoanInterestMethod.entries.forEach { option ->
            FilterChip(
                selected = method == option,
                onClick = { method = option },
                label = { Text(option.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
    Button(
        onClick = { onSave(name, lender, currency, original, current, rate, payment, method) },
        enabled = name.isNotBlank() && original.isNotBlank() && payment.isNotBlank()
    ) { Text(saveLabel) }
}
