package com.rishaanlabs.ros.data.repository

import androidx.room.withTransaction
import com.rishaanlabs.ros.data.local.RosDatabase
import com.rishaanlabs.ros.data.local.dao.FinanceDao
import com.rishaanlabs.ros.data.local.entity.FinanceAccount
import com.rishaanlabs.ros.data.local.entity.FinanceAccountType
import com.rishaanlabs.ros.data.local.entity.FinanceCategory
import com.rishaanlabs.ros.data.local.entity.FinanceTransaction
import com.rishaanlabs.ros.data.local.entity.FinanceTransactionType
import com.rishaanlabs.ros.data.local.entity.GoalAllocation
import com.rishaanlabs.ros.data.local.entity.Loan
import com.rishaanlabs.ros.data.local.entity.LoanInterestMethod
import com.rishaanlabs.ros.data.local.entity.LoanPayment
import com.rishaanlabs.ros.data.local.entity.SavingsGoal
import com.rishaanlabs.ros.data.local.entity.SavingsGoalType
import com.rishaanlabs.ros.data.local.entity.defaultFinanceCategories
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val dao: FinanceDao,
    private val database: RosDatabase
) {
    fun observeAccountBalances() = dao.observeAccountBalances()
    fun observeAccounts() = dao.observeAccounts()
    fun observeCategories() = dao.observeCategories()
    fun observeRecentTransactions(limit: Int = 100) = dao.observeRecentTransactions(limit)
    fun observeGoalProgress() = dao.observeGoalProgress()
    fun observeLoanProgress() = dao.observeLoanProgress()
    fun observeMonthlyTotals(from: LocalDateTime, to: LocalDateTime, currency: String) =
        dao.observeMonthlyTotals(from, to, currency)
    fun observeCategorySpend(from: LocalDateTime, to: LocalDateTime, currency: String) =
        dao.observeCategorySpend(from, to, currency)
    fun observeEssentialExpenseTotal(from: LocalDateTime, to: LocalDateTime, currency: String) =
        dao.observeEssentialExpenseTotal(from, to, currency)
    fun observeGoalAllocations(goalId: String) = dao.observeGoalAllocations(goalId)
    fun observeLoanPayments(loanId: String) = dao.observeLoanPayments(loanId)

    suspend fun ensureDefaultCategories() {
        if (dao.categoryCount() == 0) dao.insertCategories(defaultFinanceCategories())
    }

    suspend fun createAccount(
        name: String,
        institution: String,
        type: FinanceAccountType,
        currency: String,
        openingBalanceMinor: Long
    ) {
        require(name.isNotBlank()) { "Account name is required." }
        dao.insertAccount(
            FinanceAccount(
                name = name.trim(),
                institution = institution.trim(),
                type = type,
                currency = currency.trim().uppercase().ifBlank { "MVR" },
                openingBalanceMinor = openingBalanceMinor
            )
        )
    }

    suspend fun archiveAccount(account: FinanceAccount) = dao.updateAccount(account.copy(isArchived = true))

    suspend fun upsertCategory(category: FinanceCategory) = dao.insertCategory(category)

    suspend fun recordTransaction(
        type: FinanceTransactionType,
        amountMinor: Long,
        accountId: String,
        destinationAccountId: String? = null,
        categoryId: String? = null,
        merchant: String = "",
        note: String = "",
        occurredAt: LocalDateTime = LocalDateTime.now()
    ): FinanceTransaction {
        require(amountMinor > 0L) { "Amount must be greater than zero." }
        val source = requireNotNull(dao.getAccountById(accountId)) { "Account not found." }
        val destination = destinationAccountId?.let { requireNotNull(dao.getAccountById(it)) { "Destination account not found." } }

        if (type == FinanceTransactionType.TRANSFER) {
            require(destination != null) { "A transfer needs a destination account." }
            require(destination.id != source.id) { "Transfer accounts must be different." }
            require(destination.currency.equals(source.currency, ignoreCase = true)) {
                "Cross-currency transfers are not supported in this Finance version."
            }
        } else {
            require(destination == null) { "Destination account is only valid for transfers." }
        }

        val transaction = FinanceTransaction(
            type = type,
            amountMinor = amountMinor,
            currency = source.currency,
            accountId = source.id,
            destinationAccountId = destination?.id,
            categoryId = if (type == FinanceTransactionType.EXPENSE) categoryId else null,
            merchant = merchant.trim(),
            note = note.trim(),
            occurredAt = occurredAt
        )
        dao.insertTransaction(transaction)
        return transaction
    }

    suspend fun createGoal(
        name: String,
        type: SavingsGoalType,
        targetMinor: Long,
        currency: String,
        targetDate: LocalDate?,
        monthlyPlannedMinor: Long,
        emergencyTargetMonths: Int? = null
    ) {
        require(name.isNotBlank()) { "Goal name is required." }
        require(targetMinor > 0L) { "Goal target must be greater than zero." }
        require(monthlyPlannedMinor >= 0L) { "Monthly contribution cannot be negative." }
        dao.insertGoal(
            SavingsGoal(
                name = name.trim(),
                type = type,
                targetMinor = targetMinor,
                currency = currency.trim().uppercase().ifBlank { "MVR" },
                targetDate = targetDate,
                monthlyPlannedMinor = monthlyPlannedMinor,
                emergencyTargetMonths = emergencyTargetMonths
            )
        )
    }

    suspend fun allocateToGoal(
        goalId: String,
        amountMinor: Long,
        accountId: String? = null,
        sourceTransactionId: String? = null,
        note: String = ""
    ) {
        require(amountMinor != 0L) { "Allocation cannot be zero." }
        val goal = requireNotNull(dao.getGoalById(goalId)) { "Goal not found." }
        accountId?.let { id ->
            val account = requireNotNull(dao.getAccountById(id)) { "Account not found." }
            require(account.currency.equals(goal.currency, ignoreCase = true)) {
                "Goal and account currencies must match."
            }
        }
        dao.insertGoalAllocation(
            GoalAllocation(
                goalId = goal.id,
                amountMinor = amountMinor,
                accountId = accountId,
                sourceTransactionId = sourceTransactionId,
                note = note.trim()
            )
        )
    }

    suspend fun createLoan(
        name: String,
        lender: String,
        currency: String,
        originalPrincipalMinor: Long,
        trackingStartPrincipalMinor: Long,
        annualInterestRateBps: Int,
        aprBps: Int?,
        minimumPaymentMinor: Long,
        nextPaymentDate: LocalDate?,
        remainingTermMonths: Int?,
        interestMethod: LoanInterestMethod
    ) {
        require(name.isNotBlank()) { "Loan name is required." }
        require(originalPrincipalMinor > 0L) { "Original principal must be greater than zero." }
        require(trackingStartPrincipalMinor >= 0L && trackingStartPrincipalMinor <= originalPrincipalMinor) {
            "Current tracked principal must be between zero and original principal."
        }
        require(annualInterestRateBps >= 0) { "Interest rate cannot be negative." }
        require(minimumPaymentMinor > 0L) { "Payment must be greater than zero." }
        dao.insertLoan(
            Loan(
                name = name.trim(),
                lender = lender.trim(),
                currency = currency.trim().uppercase().ifBlank { "MVR" },
                originalPrincipalMinor = originalPrincipalMinor,
                trackingStartPrincipalMinor = trackingStartPrincipalMinor,
                annualInterestRateBps = annualInterestRateBps,
                aprBps = aprBps,
                minimumPaymentMinor = minimumPaymentMinor,
                nextPaymentDate = nextPaymentDate,
                remainingTermMonths = remainingTermMonths,
                interestMethod = interestMethod
            )
        )
    }

    suspend fun recordLoanPayment(
        loanId: String,
        accountId: String,
        totalMinor: Long,
        principalMinor: Long,
        interestMinor: Long,
        feesMinor: Long = 0L,
        paidAt: LocalDateTime = LocalDateTime.now(),
        note: String = ""
    ) {
        require(totalMinor > 0L) { "Payment must be greater than zero." }
        require(principalMinor >= 0L && interestMinor >= 0L && feesMinor >= 0L) { "Payment components cannot be negative." }
        require(principalMinor + interestMinor + feesMinor == totalMinor) {
            "Principal + interest + fees must equal the payment total."
        }

        database.withTransaction {
            val loan = requireNotNull(dao.getLoanById(loanId)) { "Loan not found." }
            val account = requireNotNull(dao.getAccountById(accountId)) { "Account not found." }
            require(account.currency.equals(loan.currency, ignoreCase = true)) {
                "Loan and payment account currencies must match."
            }
            val transactionId = UUID.randomUUID().toString()
            dao.insertTransaction(
                FinanceTransaction(
                    id = transactionId,
                    type = FinanceTransactionType.DEBT_PAYMENT,
                    amountMinor = totalMinor,
                    currency = account.currency,
                    accountId = account.id,
                    loanId = loan.id,
                    note = note.trim(),
                    occurredAt = paidAt
                )
            )
            dao.insertLoanPayment(
                LoanPayment(
                    loanId = loan.id,
                    accountId = account.id,
                    financeTransactionId = transactionId,
                    totalMinor = totalMinor,
                    principalMinor = principalMinor,
                    interestMinor = interestMinor,
                    feesMinor = feesMinor,
                    paidAt = paidAt,
                    note = note.trim()
                )
            )
        }
    }
}
