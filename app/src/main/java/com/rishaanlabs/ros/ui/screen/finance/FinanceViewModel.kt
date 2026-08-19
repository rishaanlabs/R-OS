package com.rishaanlabs.ros.ui.screen.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.FinanceAccountType
import com.rishaanlabs.ros.data.local.entity.FinanceCategory
import com.rishaanlabs.ros.data.local.entity.FinanceTransaction
import com.rishaanlabs.ros.data.local.entity.FinanceTransactionType
import com.rishaanlabs.ros.data.local.entity.LoanInterestMethod
import com.rishaanlabs.ros.data.local.entity.SavingsGoalType
import com.rishaanlabs.ros.data.local.model.CategorySpend
import com.rishaanlabs.ros.data.local.model.FinanceAccountBalance
import com.rishaanlabs.ros.data.local.model.GoalProgressRow
import com.rishaanlabs.ros.data.local.model.LoanProgressRow
import com.rishaanlabs.ros.data.local.model.MonthlyFinanceTotals
import com.rishaanlabs.ros.data.repository.FinanceRepository
import com.rishaanlabs.ros.domain.finance.FinanceCalculator
import com.rishaanlabs.ros.domain.finance.parseMajorToMinor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject


data class FinanceUiState(
    val selectedCurrency: String = "MVR",
    val accounts: List<FinanceAccountBalance> = emptyList(),
    val categories: List<FinanceCategory> = emptyList(),
    val transactions: List<FinanceTransaction> = emptyList(),
    val goals: List<GoalProgressRow> = emptyList(),
    val loans: List<LoanProgressRow> = emptyList(),
    val monthlyTotals: MonthlyFinanceTotals = MonthlyFinanceTotals(0, 0, 0),
    val categorySpend: List<CategorySpend> = emptyList(),
    val previousCategorySpend: List<CategorySpend> = emptyList(),
    val averageEssentialMonthlyMinor: Long = 0L
) {
    val currencyAccounts: List<FinanceAccountBalance>
        get() = accounts.filter { it.account.currency.equals(selectedCurrency, ignoreCase = true) }
    val totalCashMinor: Long get() = currencyAccounts.sumOf { it.balanceMinor }
    val assignedToGoalsMinor: Long get() = goals
        .filter { it.goal.currency.equals(selectedCurrency, ignoreCase = true) }
        .sumOf { it.currentMinor.coerceAtLeast(0L) }
    val freeToAllocateMinor: Long get() = totalCashMinor - assignedToGoalsMinor
    val plannedGoalContributionsMinor: Long get() = goals
        .filter { it.goal.currency.equals(selectedCurrency, ignoreCase = true) }
        .sumOf { it.goal.monthlyPlannedMinor }
    val emergencyGoal: GoalProgressRow? get() = goals.firstOrNull {
        it.goal.currency.equals(selectedCurrency, ignoreCase = true) && it.goal.type == SavingsGoalType.EMERGENCY
    }
    val emergencyRunwayMonths: Double? get() = FinanceCalculator.emergencyRunwayMonths(
        emergencyGoal?.currentMinor ?: 0L,
        averageEssentialMonthlyMinor
    )
}

private data class CoreFinanceState(
    val accounts: List<FinanceAccountBalance>,
    val categories: List<FinanceCategory>,
    val transactions: List<FinanceTransaction>,
    val goals: List<GoalProgressRow>,
    val loans: List<LoanProgressRow>
)

private data class FinanceMetricsState(
    val totals: MonthlyFinanceTotals,
    val spend: List<CategorySpend>,
    val previousSpend: List<CategorySpend>,
    val essentialTrailingTotal: Long,
    val currency: String
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val selectedCurrency = MutableStateFlow("MVR")
    private val monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay()
    private val nextMonthStart = monthStart.plusMonths(1)
    private val previousMonthStart = monthStart.minusMonths(1)
    private val trailingThreeMonthsStart = LocalDate.now().minusMonths(3).atStartOfDay()
    private val nowExclusive = LocalDate.now().plusDays(1).atStartOfDay()

    private val monthlyTotals = selectedCurrency.flatMapLatest { currency ->
        repository.observeMonthlyTotals(monthStart, nextMonthStart, currency)
    }
    private val currentCategorySpend = selectedCurrency.flatMapLatest { currency ->
        repository.observeCategorySpend(monthStart, nextMonthStart, currency)
    }
    private val previousCategorySpend = selectedCurrency.flatMapLatest { currency ->
        repository.observeCategorySpend(previousMonthStart, monthStart, currency)
    }
    private val essentialTrailing = selectedCurrency.flatMapLatest { currency ->
        repository.observeEssentialExpenseTotal(trailingThreeMonthsStart, nowExclusive, currency)
    }

    private val core = combine(
        repository.observeAccountBalances(),
        repository.observeCategories(),
        repository.observeRecentTransactions(),
        repository.observeGoalProgress(),
        repository.observeLoanProgress()
    ) { accounts, categories, transactions, goals, loans ->
        CoreFinanceState(accounts, categories, transactions, goals, loans)
    }

    private val metrics = combine(
        monthlyTotals,
        currentCategorySpend,
        previousCategorySpend,
        essentialTrailing,
        selectedCurrency
    ) { totals, spend, previousSpend, essentialTotal, currency ->
        FinanceMetricsState(totals, spend, previousSpend, essentialTotal, currency)
    }

    val state: StateFlow<FinanceUiState> = combine(core, metrics) { coreState, metricState ->
        FinanceUiState(
            selectedCurrency = metricState.currency,
            accounts = coreState.accounts,
            categories = coreState.categories,
            transactions = coreState.transactions,
            goals = coreState.goals,
            loans = coreState.loans,
            monthlyTotals = metricState.totals,
            categorySpend = metricState.spend,
            previousCategorySpend = metricState.previousSpend,
            averageEssentialMonthlyMinor = metricState.essentialTrailingTotal / 3L
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState())

    init {
        viewModelScope.launch { repository.ensureDefaultCategories() }
    }

    fun setCurrency(currency: String) {
        selectedCurrency.value = currency.uppercase()
    }

    fun addAccount(name: String, institution: String, type: FinanceAccountType, currency: String, opening: String) =
        launchAction {
            repository.createAccount(name, institution, type, currency, parseMajorToMinor(opening.ifBlank { "0" }))
        }

    fun addTransaction(
        type: FinanceTransactionType,
        amount: String,
        accountId: String,
        destinationAccountId: String?,
        categoryId: String?,
        merchant: String,
        note: String
    ) = launchAction {
        repository.recordTransaction(
            type = type,
            amountMinor = parseMajorToMinor(amount),
            accountId = accountId,
            destinationAccountId = destinationAccountId,
            categoryId = categoryId,
            merchant = merchant,
            note = note
        )
    }

    fun addGoal(
        name: String,
        type: SavingsGoalType,
        target: String,
        currency: String,
        targetDate: LocalDate?,
        monthlyPlanned: String
    ) = launchAction {
        repository.createGoal(
            name = name,
            type = type,
            targetMinor = parseMajorToMinor(target),
            currency = currency,
            targetDate = targetDate,
            monthlyPlannedMinor = parseMajorToMinor(monthlyPlanned.ifBlank { "0" }),
            emergencyTargetMonths = if (type == SavingsGoalType.EMERGENCY) 6 else null
        )
    }

    fun allocateToGoal(goalId: String, amount: String, accountId: String?, note: String = "") = launchAction {
        repository.allocateToGoal(goalId, parseMajorToMinor(amount), accountId, note = note)
    }

    fun setCategoryBudget(category: FinanceCategory, amount: String) = launchAction {
        val budget = if (amount.isBlank()) null else parseMajorToMinor(amount)
        repository.upsertCategory(category.copy(monthlyBudgetMinor = budget))
    }

    fun addLoan(
        name: String,
        lender: String,
        currency: String,
        originalPrincipal: String,
        currentPrincipal: String,
        annualRatePercent: String,
        minimumPayment: String,
        interestMethod: LoanInterestMethod
    ) = launchAction {
        val bps = ((annualRatePercent.toDoubleOrNull() ?: 0.0) * 100.0).toInt()
        repository.createLoan(
            name = name,
            lender = lender,
            currency = currency,
            originalPrincipalMinor = parseMajorToMinor(originalPrincipal),
            trackingStartPrincipalMinor = parseMajorToMinor(currentPrincipal.ifBlank { originalPrincipal }),
            annualInterestRateBps = bps,
            aprBps = null,
            minimumPaymentMinor = parseMajorToMinor(minimumPayment),
            nextPaymentDate = null,
            remainingTermMonths = null,
            interestMethod = interestMethod
        )
    }

    fun recordLoanPayment(
        loanId: String,
        accountId: String,
        total: String,
        principal: String,
        interest: String,
        fees: String,
        note: String
    ) = launchAction {
        repository.recordLoanPayment(
            loanId = loanId,
            accountId = accountId,
            totalMinor = parseMajorToMinor(total),
            principalMinor = parseMajorToMinor(principal.ifBlank { "0" }),
            interestMinor = parseMajorToMinor(interest.ifBlank { "0" }),
            feesMinor = parseMajorToMinor(fees.ifBlank { "0" }),
            note = note
        )
    }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
