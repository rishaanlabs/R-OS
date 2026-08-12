package com.rishaanlabs.ros.data.repository

import com.rishaanlabs.ros.data.local.dao.TaskDao
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(private val dao: TaskDao) {

    fun observeToday(): Flow<List<Task>> = dao.observeToday()
    fun observeTopPriorities(): Flow<List<Task>> = dao.observeTopPriorities()
    fun observeUpcoming(): Flow<List<Task>> = dao.observeUpcoming()
    fun observeAllOpen(): Flow<List<Task>> = dao.observeAllOpen()
    fun observeSomeday(): Flow<List<Task>> = dao.observeSomeday()
    fun observeCompleted(): Flow<List<Task>> = dao.observeCompleted()
    fun observeByProject(projectId: String): Flow<List<Task>> = dao.observeByProject(projectId)
    fun observeTodayCount(): Flow<Int> = dao.observeTodayCount()

    suspend fun create(
        title: String,
        description: String = "",
        projectId: String? = null,
        priority: com.rishaanlabs.ros.data.local.entity.TaskPriority =
            com.rishaanlabs.ros.data.local.entity.TaskPriority.NONE
    ): Task {
        val task = Task(title = title, description = description, projectId = projectId, priority = priority)
        dao.insert(task)
        return task
    }

    suspend fun complete(task: Task) {
        dao.update(task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = LocalDateTime.now(),
            isTopPriority = false,
            updatedAt = LocalDateTime.now()
        ))
    }

    suspend fun reopen(task: Task) {
        dao.update(task.copy(
            status = TaskStatus.OPEN,
            completedAt = null,
            updatedAt = LocalDateTime.now()
        ))
    }

    suspend fun update(task: Task) = dao.update(task.copy(updatedAt = LocalDateTime.now()))
    suspend fun delete(task: Task) = dao.delete(task)
    suspend fun getById(id: String): Task? = dao.getById(id)
    suspend fun search(query: String): List<Task> = dao.search(query)
}
