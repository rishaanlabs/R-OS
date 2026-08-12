package com.rishaanlabs.ros.data.repository

import com.rishaanlabs.ros.data.local.dao.ProjectDao
import com.rishaanlabs.ros.data.local.entity.Project
import com.rishaanlabs.ros.data.local.entity.ProjectStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(private val dao: ProjectDao) {

    fun observeActive(): Flow<List<Project>> = dao.observeByStatus(ProjectStatus.ACTIVE)
    fun observeAll(): Flow<List<Project>> = dao.observeAll()
    fun observeById(id: String): Flow<Project?> = dao.observeById(id)
    fun observeActiveCount(): Flow<Int> = dao.observeActiveCount()

    suspend fun create(title: String, description: String = "", area: String = ""): Project {
        val project = Project(title = title, description = description, area = area)
        dao.insert(project)
        return project
    }

    suspend fun update(project: Project) = dao.update(project.copy(updatedAt = LocalDateTime.now()))
    suspend fun delete(project: Project) = dao.delete(project)
    suspend fun getById(id: String): Project? = dao.getById(id)

    suspend fun setNextAction(projectId: String, taskId: String?) {
        val project = dao.getById(projectId) ?: return
        dao.update(project.copy(nextActionId = taskId, updatedAt = LocalDateTime.now()))
    }
}
