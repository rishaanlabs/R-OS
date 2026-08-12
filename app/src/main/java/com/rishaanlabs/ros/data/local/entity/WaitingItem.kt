package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents something the user is waiting on from another person or organization.
 * Waiting is treated as a first-class concept, not merely a task status.
 */
@Entity(
    tableName = "waiting_items",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("projectId")]
)
data class WaitingItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val person: String = "",
    val description: String = "",
    val projectId: String? = null,
    val requestedDate: LocalDateTime = LocalDateTime.now(),
    val followUpDate: LocalDateTime? = null,
    val status: WaitingStatus = WaitingStatus.WAITING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val resolvedAt: LocalDateTime? = null
)

enum class WaitingStatus {
    WAITING, FOLLOW_UP_DUE, RESOLVED, CANCELLED
}
