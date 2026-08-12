package com.rishaanlabs.ros.data.local.dao

import androidx.room.*
import com.rishaanlabs.ros.data.local.entity.WaitingItem
import com.rishaanlabs.ros.data.local.entity.WaitingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WaitingDao {

    // "followUpDate IS NULL" sorts 0 before 1, which puts dated items first and undated ones
    // last. NULLS LAST would say this more directly, but SQLite only understands it from 3.30
    // (Android 11), and this app supports Android 8.
    @Query("SELECT * FROM waiting_items WHERE status IN ('WAITING', 'FOLLOW_UP_DUE') ORDER BY followUpDate IS NULL, followUpDate ASC")
    fun observeActive(): Flow<List<WaitingItem>>

    @Query("SELECT * FROM waiting_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WaitingItem>>

    @Query("SELECT * FROM waiting_items WHERE projectId = :projectId AND status != 'CANCELLED' ORDER BY createdAt DESC")
    fun observeByProject(projectId: String): Flow<List<WaitingItem>>

    @Query("SELECT * FROM waiting_items WHERE id = :id")
    suspend fun getById(id: String): WaitingItem?

    @Query("SELECT COUNT(*) FROM waiting_items WHERE status IN ('WAITING', 'FOLLOW_UP_DUE')")
    fun observeActiveCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM waiting_items 
        WHERE status = 'WAITING' 
        AND followUpDate IS NOT NULL 
        AND date(followUpDate) <= date('now')
    """)
    fun observeOverdueCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WaitingItem)

    @Update
    suspend fun update(item: WaitingItem)

    @Delete
    suspend fun delete(item: WaitingItem)

    @Query("""
        SELECT * FROM waiting_items 
        WHERE (title LIKE '%' || :query || '%' OR person LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        AND status != 'CANCELLED'
        ORDER BY createdAt DESC
    """)
    suspend fun search(query: String): List<WaitingItem>
}
