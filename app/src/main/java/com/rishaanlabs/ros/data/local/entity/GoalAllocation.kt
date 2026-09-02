package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "finance_goal_allocations",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FinanceAccount::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = FinanceTransaction::class,
            parentColumns = ["id"],
            childColumns = ["sourceTransactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("goalId"), Index("accountId"), Index("sourceTransactionId"), Index("allocatedAt")]
)
data class GoalAllocation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    /** Positive reserves money for a goal, negative releases it. */
    val amountMinor: Long,
    val accountId: String? = null,
    val sourceTransactionId: String? = null,
    val note: String = "",
    val allocatedAt: LocalDateTime = LocalDateTime.now(),
    val createdAt: LocalDateTime = LocalDateTime.now()
)
