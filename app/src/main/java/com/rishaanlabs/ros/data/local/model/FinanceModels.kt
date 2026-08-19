package com.rishaanlabs.ros.data.local.model

import androidx.room.Embedded
import com.rishaanlabs.ros.data.local.entity.FinanceAccount
import com.rishaanlabs.ros.data.local.entity.Loan
import com.rishaanlabs.ros.data.local.entity.SavingsGoal


data class FinanceAccountBalance(
    @Embedded val account: FinanceAccount,
    val balanceMinor: Long
)

data class MonthlyFinanceTotals(
    val incomeMinor: Long,
    val expenseMinor: Long,
    val debtPaymentMinor: Long
) {
    val netCashFlowMinor: Long
        get() = incomeMinor - expenseMinor - debtPaymentMinor
}

data class CategorySpend(
    val categoryId: String,
    val groupName: String,
    val categoryName: String,
    val isEssential: Boolean,
    val monthlyBudgetMinor: Long?,
    val amountMinor: Long
)

data class GoalProgressRow(
    @Embedded val goal: SavingsGoal,
    val currentMinor: Long
)

data class LoanProgressRow(
    @Embedded val loan: Loan,
    val principalPaidMinor: Long,
    val interestPaidMinor: Long,
    val feesPaidMinor: Long
) {
    val currentPrincipalMinor: Long
        get() = (loan.trackingStartPrincipalMinor - principalPaidMinor).coerceAtLeast(0L)
}
