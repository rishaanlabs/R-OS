package com.rishaanlabs.ros.data.local.dao

import androidx.room.*
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TaskDao {

    @Query("""
        SELECT * FROM tasks 
        WHERE status = 'OPEN' 
        AND (date(scheduledDate) <= date('now') OR date(dueDate) <= date('now') OR isTopPriority = 1)
        ORDER BY isTopPriority DESC, priority DESC, dueDate ASC
    """)
    fun observeToday(): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE status = 'OPEN' 
        AND isTopPriority = 1
        ORDER BY sortOrder ASC
    """)
    fun observeTopPriorities(): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE status = 'OPEN' 
        AND isSomeday = 0
        AND (date(dueDate) > date('now') OR date(scheduledDate) > date('now'))
        ORDER BY dueDate ASC, scheduledDate ASC
    """)
    fun observeUpcoming(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = 'OPEN' AND isSomeday = 0 ORDER BY priority DESC, createdAt DESC")
    fun observeAllOpen(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = 'OPEN' AND isSomeday = 1 ORDER BY createdAt DESC")
    fun observeSomeday(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND status != 'CANCELLED' ORDER BY sortOrder ASC, createdAt ASC")
    fun observeByProject(projectId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'OPEN' AND (date(scheduledDate) <= date('now') OR date(dueDate) <= date('now') OR isTopPriority = 1)")
    fun observeTodayCount(): Flow<Int>

    @Query("""
        SELECT * FROM tasks 
        WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        AND status != 'CANCELLED'
        ORDER BY createdAt DESC
    """)
    suspend fun search(query: String): List<Task>
}
