package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a single actionable item.
 * Tasks are the core unit of work. They optionally belong to a project.
 * A task can exist independently of any project.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            // SET NULL so deleting a project does not silently destroy tasks.
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("projectId")]
)
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.OPEN,
    val priority: TaskPriority = TaskPriority.NONE,
    val projectId: String? = null,
    val dueDate: LocalDateTime? = null,
    val scheduledDate: LocalDateTime? = null,
    val isSomeday: Boolean = false,
    val isTopPriority: Boolean = false,
    val reminderAt: LocalDateTime? = null,
    val parentTaskId: String? = null,
    val sortOrder: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class TaskStatus {
    OPEN, COMPLETED, CANCELLED
}

enum class TaskPriority {
    NONE, LOW, MEDIUM, HIGH
}
