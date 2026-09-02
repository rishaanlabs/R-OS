package com.rishaanlabs.ros.ui.screen.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Finance.
 *
 * Two things about this screen came out of using it for a fortnight rather than from designing it.
 *
 * The tabs were six, and one of them ("Plan") existed only to edit monthly limits, which belong
 * beside the spending they constrain. Folding it into Overview left five sections that each answer
 * a different question, and removed a whole navigation decision from every visit.
 *
 * The add button opened a picker asking which of four kinds of record you meant, on every tab,
 * including the tabs that only contain one kind. It now adds the thing the tab is about, and only
 * asks on Overview where there is no obvious answer.
 */
private enum class FinanceTab(val title: String, val addKind: FinanceAddKind?) {
    OVERVIEW("Overview", null),
    ACCOUNTS("Accounts", FinanceAddKind.ACCOUNT),
    ACTIVITY("Activity", FinanceAddKind.TRANSACTION),
    SAVINGS("Savings", FinanceAddKind.GOAL),
    DEBT("Debt", FinanceAddKind.LOAN)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    /** Null when Finance is shown as a root tab: there is nothing to go back to. */
    onBack: (() -> Unit)? = null,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FinanceTarget?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val tab = FinanceTab.entries[selectedTab]

    // Validation failures used to escape the ViewModel scope and take the process with them.
    // They arrive here as a message instead.
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text("Finance") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        when (tab.addKind) {
                            FinanceAddKind.ACCOUNT -> "Account"
                            FinanceAddKind.TRANSACTION -> "Transaction"
                            FinanceAddKind.GOAL -> "Goal"
                            FinanceAddKind.LOAN -> "Loan"
                            null -> "Add"
                        }
                    )
                }
            )
        }
    ) { padding ->
        val tabPadding = PaddingValues(bottom = padding.calculateBottomPadding())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                FinanceTab.entries.forEachIndexed { index, entry ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(entry.title) }
                    )
                }
            }
            when (tab) {
                FinanceTab.OVERVIEW -> FinanceOverviewTab(
                    state = state,
                    contentPadding = tabPadding,
                    onSetBudget = viewModel::setCategoryBudget
                )
                FinanceTab.ACCOUNTS -> AccountsTab(
                    state = state,
                    contentPadding = tabPadding,
                    onOpen = { editing = it }
                )
                FinanceTab.ACTIVITY -> TransactionsTab(
                    state = state,
                    contentPadding = tabPadding,
                    onOpen = { editing = it }
                )
                FinanceTab.SAVINGS -> GoalsTab(
                    state = state,
                    contentPadding = tabPadding,
                    onAllocate = viewModel::allocateToGoal,
                    onOpen = { editing = it }
                )
                FinanceTab.DEBT -> DebtTab(
                    state = state,
                    contentPadding = tabPadding,
                    onRecordPayment = viewModel::recordLoanPayment,
                    onOpen = { editing = it }
                )
            }
        }
    }

    if (showAddSheet) {
        FinanceAddSheet(
            state = state,
            fixedKind = tab.addKind,
            onDismiss = { showAddSheet = false },
            onAddAccount = viewModel::addAccount,
            onAddTransaction = viewModel::addTransaction,
            onAddGoal = viewModel::addGoal,
            onAddLoan = viewModel::addLoan
        )
    }

    editing?.let { target ->
        FinanceEditSheet(
            state = state,
            target = target,
            onDismiss = { editing = null },
            onSaveAccount = viewModel::editAccount,
            onSaveTransaction = viewModel::editTransaction,
            onSaveGoal = viewModel::editGoal,
            onSaveLoan = viewModel::editLoan,
            onArchiveAccount = viewModel::archiveAccount,
            onDelete = viewModel::requestDelete
        )
    }

    // Deleting always asks, and the question states the consequence rather than a generic warning.
    state.pendingDeletion?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(if (pending.isBlocked) "Cannot delete ${pending.label}" else "Delete ${pending.label}?") },
            text = {
                Text(
                    pending.explanation
                        ?: "This cannot be undone. Nothing else refers to it, so nothing else changes."
                )
            },
            confirmButton = {
                if (pending.isBlocked) {
                    TextButton(onClick = viewModel::cancelDelete) { Text("Close") }
                } else {
                    TextButton(onClick = viewModel::confirmDelete) { Text("Delete") }
                }
            },
            dismissButton = {
                if (!pending.isBlocked) {
                    TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") }
                }
            }
        )
    }
}
