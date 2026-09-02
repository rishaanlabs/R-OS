package com.rishaanlabs.ros.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rishaanlabs.ros.data.local.entity.FinanceAccount
import com.rishaanlabs.ros.data.local.entity.FinanceCategory
import com.rishaanlabs.ros.data.local.entity.FinanceTransaction
import com.rishaanlabs.ros.data.local.entity.GoalAllocation
import com.rishaanlabs.ros.data.local.entity.Loan
import com.rishaanlabs.ros.data.local.entity.LoanPayment
import com.rishaanlabs.ros.data.local.entity.SavingsGoal
import com.rishaanlabs.ros.data.local.model.CategorySpend
import com.rishaanlabs.ros.data.local.model.FinanceAccountBalance
import com.rishaanlabs.ros.data.local.model.GoalProgressRow
import com.rishaanlabs.ros.data.local.model.LoanProgressRow
import com.rishaanlabs.ros.data.local.model.MonthlyFinanceTotals
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface FinanceDao {

    @Query("""
        SELECT a.*,
            a.openingBalanceMinor + COALESCE(SUM(
                CASE
                    WHEN t.type = 'INCOME' AND t.accountId = a.id THEN t.amountMinor
                    WHEN t.type IN ('EXPENSE', 'DEBT_PAYMENT') AND t.accountId = a.id THEN -t.amountMinor
                    WHEN t.type = 'TRANSFER' AND t.accountId = a.id THEN -t.amountMinor
                    WHEN t.type = 'TRANSFER' AND t.destinationAccountId = a.id THEN t.amountMinor
                    ELSE 0
                END
            ), 0) AS balanceMinor
        FROM finance_accounts a
        LEFT JOIN finance_transactions t
            ON t.accountId = a.id OR t.destinationAccountId = a.id
        WHERE a.isArchived = 0
        GROUP BY a.id
        ORDER BY a.institution COLLATE NOCASE, a.name COLLATE NOCASE
    """)
    fun observeAccountBalances(): Flow<List<FinanceAccountBalance>>

    @Query("SELECT * FROM finance_accounts WHERE isArchived = 0 ORDER BY institution COLLATE NOCASE, name COLLATE NOCASE")
    fun observeAccounts(): Flow<List<FinanceAccount>>

    @Query("SELECT * FROM finance_accounts WHERE id = :id")
    suspend fun getAccountById(id: String): FinanceAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: FinanceAccount)

    @Update
    suspend fun updateAccount(account: FinanceAccount)

    @Query("DELETE FROM finance_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: String)

    /**
     * Transactions referencing this account either way round. The accountId foreign key is
     * NO_ACTION, so deleting an account with transactions would be refused by SQLite; this lets
     * the repository say why instead of surfacing a constraint failure.
     */
    @Query("SELECT COUNT(*) FROM finance_transactions WHERE accountId = :id OR destinationAccountId = :id")
    suspend fun countTransactionsForAccount(id: String): Int

    @Query("SELECT COUNT(*) FROM finance_categories")
    suspend fun categoryCount(): Int

    @Query("SELECT * FROM finance_categories WHERE isArchived = 0 ORDER BY sortOrder, groupName COLLATE NOCASE, name COLLATE NOCASE")
    fun observeCategories(): Flow<List<FinanceCategory>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<FinanceCategory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: FinanceCategory)

    @Update
    suspend fun updateCategory(category: FinanceCategory)

    @Query("SELECT * FROM finance_transactions ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int = 100): Flow<List<FinanceTransaction>>

    @Query("SELECT * FROM finance_transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): FinanceTransaction?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: FinanceTransaction)

    @Update
    suspend fun updateTransaction(transaction: FinanceTransaction)

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    /**
     * The loan payment recorded alongside this transaction, if any. A debt payment writes both
     * rows together, so removing one without the other would leave the loan and the account
     * disagreeing about whether the money was paid.
     */
    @Query("SELECT * FROM finance_loan_payments WHERE financeTransactionId = :transactionId LIMIT 1")
    suspend fun getLoanPaymentByTransactionId(transactionId: String): LoanPayment?

    @Query("DELETE FROM finance_loan_payments WHERE id = :id")
    suspend fun deleteLoanPaymentById(id: String)

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountMinor ELSE 0 END), 0) AS incomeMinor,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS expenseMinor,
            COALESCE(SUM(CASE WHEN type = 'DEBT_PAYMENT' THEN amountMinor ELSE 0 END), 0) AS debtPaymentMinor
        FROM finance_transactions
        WHERE currency = :currency
          AND occurredAt >= :from
          AND occurredAt < :to
    """)
    fun observeMonthlyTotals(from: LocalDateTime, to: LocalDateTime, currency: String): Flow<MonthlyFinanceTotals>

    @Query("""
        SELECT
            c.id AS categoryId,
            c.groupName AS groupName,
            c.name AS categoryName,
            c.isEssential AS isEssential,
            c.monthlyBudgetMinor AS monthlyBudgetMinor,
            COALESCE(SUM(t.amountMinor), 0) AS amountMinor
        FROM finance_categories c
        LEFT JOIN finance_transactions t
            ON t.categoryId = c.id
            AND t.type = 'EXPENSE'
            AND t.currency = :currency
            AND t.occurredAt >= :from
            AND t.occurredAt < :to
        WHERE c.isArchived = 0
        GROUP BY c.id
        HAVING amountMinor > 0 OR monthlyBudgetMinor IS NOT NULL
        ORDER BY amountMinor DESC, c.sortOrder ASC
    """)
    fun observeCategorySpend(from: LocalDateTime, to: LocalDateTime, currency: String): Flow<List<CategorySpend>>

    @Query("""
        SELECT COALESCE(SUM(t.amountMinor), 0)
        FROM finance_transactions t
        INNER JOIN finance_categories c ON c.id = t.categoryId
        WHERE t.type = 'EXPENSE'
          AND c.isEssential = 1
          AND t.currency = :currency
          AND t.occurredAt >= :from
          AND t.occurredAt < :to
    """)
    fun observeEssentialExpenseTotal(from: LocalDateTime, to: LocalDateTime, currency: String): Flow<Long>

    @Query("""
        SELECT g.*, COALESCE(SUM(a.amountMinor), 0) AS currentMinor
        FROM finance_goals g
        LEFT JOIN finance_goal_allocations a ON a.goalId = g.id
        WHERE g.status != 'ARCHIVED'
        GROUP BY g.id
        ORDER BY CASE g.status WHEN 'ACTIVE' THEN 0 WHEN 'PAUSED' THEN 1 WHEN 'COMPLETED' THEN 2 ELSE 3 END,
                 g.targetDate IS NULL,
                 g.targetDate ASC,
                 g.createdAt DESC
    """)
    fun observeGoalProgress(): Flow<List<GoalProgressRow>>

    @Query("SELECT * FROM finance_goals WHERE id = :id")
    suspend fun getGoalById(id: String): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal)

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Query("DELETE FROM finance_goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Query("SELECT COUNT(*) FROM finance_goal_allocations WHERE goalId = :id")
    suspend fun countAllocationsForGoal(id: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoalAllocation(allocation: GoalAllocation)

    @Query("SELECT * FROM finance_goal_allocations WHERE goalId = :goalId ORDER BY allocatedAt DESC")
    fun observeGoalAllocations(goalId: String): Flow<List<GoalAllocation>>

    @Query("""
        SELECT l.*,
            COALESCE(SUM(p.principalMinor), 0) AS principalPaidMinor,
            COALESCE(SUM(p.interestMinor), 0) AS interestPaidMinor,
            COALESCE(SUM(p.feesMinor), 0) AS feesPaidMinor
        FROM finance_loans l
        LEFT JOIN finance_loan_payments p ON p.loanId = l.id
        WHERE l.status != 'ARCHIVED'
        GROUP BY l.id
        ORDER BY CASE l.status WHEN 'ACTIVE' THEN 0 WHEN 'PAUSED' THEN 1 WHEN 'PAID_OFF' THEN 2 ELSE 3 END,
                 l.nextPaymentDate IS NULL,
                 l.nextPaymentDate ASC,
                 l.createdAt DESC
    """)
    fun observeLoanProgress(): Flow<List<LoanProgressRow>>

    @Query("SELECT * FROM finance_loans WHERE id = :id")
    suspend fun getLoanById(id: String): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan)

    @Update
    suspend fun updateLoan(loan: Loan)

    @Query("DELETE FROM finance_loans WHERE id = :id")
    suspend fun deleteLoanById(id: String)

    @Query("SELECT COUNT(*) FROM finance_loan_payments WHERE loanId = :id")
    suspend fun countPaymentsForLoan(id: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLoanPayment(payment: LoanPayment)

    @Query("SELECT * FROM finance_loan_payments WHERE loanId = :loanId ORDER BY paidAt DESC")
    fun observeLoanPayments(loanId: String): Flow<List<LoanPayment>>
}
