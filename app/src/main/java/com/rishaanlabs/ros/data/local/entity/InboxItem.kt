package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents an unprocessed capture from the user.
 * The inbox is the entry point for all new information.
 * Items can be converted into tasks, notes, or waiting items later.
 */
@Entity(tableName = "inbox_items")
data class InboxItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: InboxItemType = InboxItemType.UNSPECIFIED,
    val isProcessed: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val processedAt: LocalDateTime? = null
)

enum class InboxItemType {
    UNSPECIFIED, TASK, NOTE, IDEA, WAITING
}
