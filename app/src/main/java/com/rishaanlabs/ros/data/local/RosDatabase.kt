package com.rishaanlabs.ros.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rishaanlabs.ros.data.local.dao.*
import com.rishaanlabs.ros.data.local.entity.*

/**
 * The single Room database for the application.
 *
 * Version 1 was the initial schema. Version 2 adds the Finance tables and is reached through
 * FINANCE_MIGRATION_1_2, which only creates new tables — no existing table is touched, so
 * Inbox, Project, Task, Waiting, Note and Daily Review data survives the upgrade untouched.
 *
 * Bump the version whenever the schema changes and always supply a Migration. Never use
 * fallbackToDestructiveMigration here: this database holds the only copy of the user's data.
 */
@Database(
    entities = [
        InboxItem::class,
        Project::class,
        Task::class,
        WaitingItem::class,
        Note::class,
        DailyReview::class,
        FinanceAccount::class,
        FinanceCategory::class,
        Loan::class,
        FinanceTransaction::class,
        SavingsGoal::class,
        GoalAllocation::class,
        LoanPayment::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RosDatabase : RoomDatabase() {
    abstract fun inboxDao(): InboxDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun waitingDao(): WaitingDao
    abstract fun noteDao(): NoteDao
    abstract fun dailyReviewDao(): DailyReviewDao
    abstract fun financeDao(): FinanceDao
}
