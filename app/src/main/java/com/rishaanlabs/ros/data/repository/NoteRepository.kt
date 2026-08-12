package com.rishaanlabs.ros.data.repository

import com.rishaanlabs.ros.data.local.dao.NoteDao
import com.rishaanlabs.ros.data.local.entity.Note
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val dao: NoteDao) {

    fun observeAll(): Flow<List<Note>> = dao.observeAll()
    fun observeByProject(projectId: String): Flow<List<Note>> = dao.observeByProject(projectId)
    fun observeById(id: String): Flow<Note?> = dao.observeById(id)

    suspend fun create(title: String, body: String = "", projectId: String? = null): Note {
        val note = Note(title = title, body = body, projectId = projectId)
        dao.insert(note)
        return note
    }

    suspend fun update(note: Note) = dao.update(note.copy(updatedAt = LocalDateTime.now()))
    suspend fun delete(note: Note) = dao.delete(note)
    suspend fun getById(id: String): Note? = dao.getById(id)
    suspend fun search(query: String): List<Note> = dao.search(query)
}
