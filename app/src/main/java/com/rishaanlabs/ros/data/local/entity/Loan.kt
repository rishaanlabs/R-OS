package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "finance_loans",
    indices = [Index("status"), Index("currency")]
)
data class Loan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lender: String = "",
    val currency: String = "MVR",
    val originalPrincipalMinor: Long,
    val trackingStartPrincipalMinor: Long = originalPrincipalMinor,
    /** Basis points: 900 = 9.00%. */
    val annualInterestRateBps: Int = 0,
    /** APR basis points, when the lender provides it. */
    val aprBps: Int? = null,
    val minimumPaymentMinor: Long,
    val startedOn: LocalDate? = null,
    val nextPaymentDate: LocalDate? = null,
    val remainingTermMonths: Int? = null,
    val interestMethod: LoanInterestMethod = LoanInterestMethod.AMORTIZED_MONTHLY,
    /** Manual statement value for interest accrued but not yet paid. */
    val manualAccruedInterestMinor: Long = 0L,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class LoanInterestMethod {
    AMORTIZED_MONTHLY,
    SIMPLE_DAILY,
    PRECOMPUTED,
    MANUAL
}

enum class LoanStatus {
    ACTIVE,
    PAID_OFF,
    PAUSED,
    ARCHIVED
}
