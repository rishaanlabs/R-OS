package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "finance_transactions",
    foreignKeys = [
        ForeignKey(
            entity = FinanceAccount::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = FinanceAccount::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = FinanceCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("accountId"),
        Index("destinationAccountId"),
        Index("categoryId"),
        Index("loanId"),
        Index("occurredAt")
    ]
)
data class FinanceTransaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: FinanceTransactionType,
    /** Always positive. Direction comes from [type] and account fields. */
    val amountMinor: Long,
    val currency: String,
    /** Income destination, expense/debt source, or transfer source. */
    val accountId: String,
    /** Used only for transfers. */
    val destinationAccountId: String? = null,
    val categoryId: String? = null,
    val loanId: String? = null,
    val merchant: String = "",
    val note: String = "",
    val occurredAt: LocalDateTime = LocalDateTime.now(),
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class FinanceTransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    DEBT_PAYMENT
}
