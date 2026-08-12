package com.rishaanlabs.ros.data.repository

import com.rishaanlabs.ros.data.local.dao.WaitingDao
import com.rishaanlabs.ros.data.local.entity.WaitingItem
import com.rishaanlabs.ros.data.local.entity.WaitingStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaitingRepository @Inject constructor(private val dao: WaitingDao) {

    fun observeActive(): Flow<List<WaitingItem>> = dao.observeActive()
    fun observeAll(): Flow<List<WaitingItem>> = dao.observeAll()
    fun observeByProject(projectId: String): Flow<List<WaitingItem>> = dao.observeByProject(projectId)
    fun observeActiveCount(): Flow<Int> = dao.observeActiveCount()
    fun observeOverdueCount(): Flow<Int> = dao.observeOverdueCount()

    suspend fun create(
        title: String,
        person: String = "",
        description: String = "",
        projectId: String? = null,
        followUpDate: LocalDateTime? = null
    ): WaitingItem {
        val item = WaitingItem(
            title = title,
            person = person,
            description = description,
            projectId = projectId,
            followUpDate = followUpDate
        )
        dao.insert(item)
        return item
    }

    suspend fun resolve(item: WaitingItem) {
        dao.update(item.copy(status = WaitingStatus.RESOLVED, resolvedAt = LocalDateTime.now()))
    }

    suspend fun update(item: WaitingItem) = dao.update(item)
    suspend fun delete(item: WaitingItem) = dao.delete(item)
    suspend fun getById(id: String): WaitingItem? = dao.getById(id)
    suspend fun search(query: String): List<WaitingItem> = dao.search(query)
}
