package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "finance_loan_payments",
    foreignKeys = [
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
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
            childColumns = ["financeTransactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("loanId"), Index("accountId"), Index("financeTransactionId"), Index("paidAt")]
)
data class LoanPayment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val loanId: String,
    val accountId: String? = null,
    val financeTransactionId: String? = null,
    val totalMinor: Long,
    val principalMinor: Long,
    val interestMinor: Long,
    val feesMinor: Long = 0L,
    val paidAt: LocalDateTime = LocalDateTime.now(),
    val note: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)
