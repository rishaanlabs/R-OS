package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "finance_goals",
    indices = [Index("currency"), Index("status")]
)
data class SavingsGoal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: SavingsGoalType = SavingsGoalType.OTHER,
    val targetMinor: Long,
    val currency: String = "MVR",
    val targetDate: LocalDate? = null,
    val monthlyPlannedMinor: Long = 0L,
    /** For emergency goals, optional desired runway target such as 3 or 6 months. */
    val emergencyTargetMonths: Int? = null,
    val status: SavingsGoalStatus = SavingsGoalStatus.ACTIVE,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class SavingsGoalType {
    EMERGENCY,
    MEDICAL,
    TRAVEL,
    STUDY,
    PURCHASE,
    HOME,
    BUSINESS,
    OTHER
}

enum class SavingsGoalStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED
}
