package com.rishaanlabs.ros.data.repository

import com.rishaanlabs.ros.data.local.dao.DailyReviewDao
import com.rishaanlabs.ros.data.local.entity.DailyReview
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyReviewRepository @Inject constructor(private val dao: DailyReviewDao) {

    fun observeAll(): Flow<List<DailyReview>> = dao.observeAll()
    fun observeByDate(date: LocalDate): Flow<DailyReview?> = dao.observeByDate(date)
    suspend fun getByDate(date: LocalDate): DailyReview? = dao.getByDate(date)

    suspend fun save(review: DailyReview) {
        val existing = dao.getByDate(review.date)
        if (existing == null) {
            dao.insert(review)
        } else {
            dao.update(review.copy(id = existing.id))
        }
    }

    suspend fun delete(review: DailyReview) = dao.delete(review)
}
