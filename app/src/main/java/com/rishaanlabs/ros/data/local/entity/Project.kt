package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a project — an outcome that requires multiple actions.
 * Projects provide context for tasks, notes, and waiting items.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val area: String = "",
    val nextActionId: String? = null,
    val targetDate: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class ProjectStatus {
    ACTIVE, PAUSED, COMPLETED, ARCHIVED
}
