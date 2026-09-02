package com.rishaanlabs.ros.ui.screen.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val financeTabs = listOf("Overview", "Accounts", "Plan", "Goals", "Activity", "Debt")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    onBack: () -> Unit,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add finance item")
            }
        }
    ) { padding ->
        val tabPadding = PaddingValues(bottom = padding.calculateBottomPadding())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                financeTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            when (selectedTab) {
                0 -> FinanceOverviewTab(state = state, contentPadding = tabPadding)
                1 -> AccountsTab(state = state, contentPadding = tabPadding)
                2 -> PlanTab(
                    state = state,
                    contentPadding = tabPadding,
                    onSetBudget = viewModel::setCategoryBudget
                )
                3 -> GoalsTab(
                    state = state,
                    contentPadding = tabPadding,
                    onAllocate = viewModel::allocateToGoal
                )
                4 -> TransactionsTab(state = state, contentPadding = tabPadding)
                5 -> DebtTab(
                    state = state,
                    contentPadding = tabPadding,
                    onRecordPayment = viewModel::recordLoanPayment
                )
            }
        }
    }

    if (showAddSheet) {
        FinanceAddSheet(
            state = state,
            onDismiss = { showAddSheet = false },
            onAddAccount = viewModel::addAccount,
            onAddTransaction = viewModel::addTransaction,
            onAddGoal = viewModel::addGoal,
            onAddLoan = viewModel::addLoan
        )
    }
}
