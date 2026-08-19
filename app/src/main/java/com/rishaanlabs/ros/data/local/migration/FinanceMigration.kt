package com.rishaanlabs.ros.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the Finance module without touching any V0.1/V0.1.1 data. */
val FINANCE_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `finance_accounts` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `institution` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `currency` TEXT NOT NULL,
                `openingBalanceMinor` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL,
                `createdAt` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_accounts_currency` ON `finance_accounts` (`currency`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `finance_categories` (
                `id` TEXT NOT NULL,
                `groupName` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `isEssential` INTEGER NOT NULL,
                `monthlyBudgetMinor` INTEGER,
                `isArchived` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_finance_categories_groupName_name` ON `finance_categories` (`groupName`, `name`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `finance_loans` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `lender` TEXT NOT NULL,
                `currency` TEXT NOT NULL,
                `originalPrincipalMinor` INTEGER NOT NULL,
                `trackingStartPrincipalMinor` INTEGER NOT NULL,
                `annualInterestRateBps` INTEGER NOT NULL,
                `aprBps` INTEGER,
                `minimumPaymentMinor` INTEGER NOT NULL,
                `startedOn` TEXT,
                `nextPaymentDate` TEXT,
                `remainingTermMonths` INTEGER,
                `interestMethod` TEXT NOT NULL,
                `manualAccruedInterestMinor` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_loans_status` ON `finance_loans` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_loans_currency` ON `finance_loans` (`currency`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `finance_transactions` (
                `id` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `accountId` TEXT NOT NULL,
                `destinationAccountId` TEXT,
                `categoryId` TEXT,
                `loanId` TEXT,
                `merchant` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `occurredAt` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`accountId`) REFERENCES `finance_accounts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`destinationAccountId`) REFERENCES `finance_accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `finance_categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`loanId`) REFERENCES `finance_loans`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transactions_accountId` ON `finance_transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transactions_destinationAccountId` ON `finance_transactions` (`destinationAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transactions_categoryId` ON `finance_transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transactions_loanId` ON `finance_transactions` (`loanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transactions_occurredAt` ON `finance_transactions` (`occurredAt`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `finance_goals` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `targetMinor` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `targetDate` TEXT,
                `monthlyPlannedMinor` INTEGER NOT NULL,
                `emergencyTargetMonths` INTEGER,
                `status` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_goals_currency` ON `finance_goals` (`currency`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_goals_status` ON `finance_goals` (`status`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `finance_goal_allocations` (
                `id` TEXT NOT NULL,
                `goalId` TEXT NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `accountId` TEXT,
                `sourceTransactionId` TEXT,
                `note` TEXT NOT NULL,
                `allocatedAt` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`goalId`) REFERENCES `finance_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`accountId`) REFERENCES `finance_accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`sourceTransactionId`) REFERENCES `finance_transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_goal_allocations_goalId` ON `finance_goal_allocations` (`goalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_goal_allocations_accountId` ON `finance_goal_allocations` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_goal_allocations_sourceTransactionId` ON `finance_goal_allocations` (`sourceTransactionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_goal_allocations_allocatedAt` ON `finance_goal_allocations` (`allocatedAt`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `finance_loan_payments` (
                `id` TEXT NOT NULL,
                `loanId` TEXT NOT NULL,
                `accountId` TEXT,
                `financeTransactionId` TEXT,
                `totalMinor` INTEGER NOT NULL,
                `principalMinor` INTEGER NOT NULL,
                `interestMinor` INTEGER NOT NULL,
                `feesMinor` INTEGER NOT NULL,
                `paidAt` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`loanId`) REFERENCES `finance_loans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`accountId`) REFERENCES `finance_accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`financeTransactionId`) REFERENCES `finance_transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_loan_payments_loanId` ON `finance_loan_payments` (`loanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_loan_payments_accountId` ON `finance_loan_payments` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_loan_payments_financeTransactionId` ON `finance_loan_payments` (`financeTransactionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_loan_payments_paidAt` ON `finance_loan_payments` (`paidAt`)")
    }
}
