package com.rishaanlabs.ros.data.repository

import com.rishaanlabs.ros.data.local.dao.InboxDao
import com.rishaanlabs.ros.data.local.entity.InboxItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxRepository @Inject constructor(private val dao: InboxDao) {

    fun observeUnprocessed(): Flow<List<InboxItem>> = dao.observeUnprocessed()
    fun observeAll(): Flow<List<InboxItem>> = dao.observeAll()
    fun observeUnprocessedCount(): Flow<Int> = dao.observeUnprocessedCount()

    suspend fun capture(text: String, type: com.rishaanlabs.ros.data.local.entity.InboxItemType =
        com.rishaanlabs.ros.data.local.entity.InboxItemType.UNSPECIFIED) {
        dao.insert(InboxItem(text = text, type = type))
    }

    suspend fun markProcessed(item: InboxItem) {
        dao.update(item.copy(isProcessed = true, processedAt = java.time.LocalDateTime.now()))
    }

    suspend fun update(item: InboxItem) = dao.update(item)
    suspend fun delete(item: InboxItem) = dao.delete(item)
    suspend fun getById(id: String): InboxItem? = dao.getById(id)
}
