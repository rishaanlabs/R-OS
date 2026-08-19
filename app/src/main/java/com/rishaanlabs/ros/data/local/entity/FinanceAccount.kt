package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "finance_accounts",
    indices = [Index("currency")]
)
data class FinanceAccount(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val institution: String = "",
    val type: FinanceAccountType = FinanceAccountType.BANK,
    val currency: String = "MVR",
    val openingBalanceMinor: Long = 0L,
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class FinanceAccountType {
    BANK,
    CASH,
    SAVINGS,
    WALLET,
    OTHER
}
