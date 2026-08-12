package com.rishaanlabs.ros.data.local.dao

import androidx.room.*
import com.rishaanlabs.ros.data.local.entity.InboxItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxDao {

    @Query("SELECT * FROM inbox_items WHERE isProcessed = 0 ORDER BY createdAt DESC")
    fun observeUnprocessed(): Flow<List<InboxItem>>

    @Query("SELECT * FROM inbox_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InboxItem>>

    @Query("SELECT COUNT(*) FROM inbox_items WHERE isProcessed = 0")
    fun observeUnprocessedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InboxItem)

    @Update
    suspend fun update(item: InboxItem)

    @Delete
    suspend fun delete(item: InboxItem)

    @Query("SELECT * FROM inbox_items WHERE id = :id")
    suspend fun getById(id: String): InboxItem?
}
