package com.rishaanlabs.ros.data.local.dao

import androidx.room.*
import com.rishaanlabs.ros.data.local.entity.DailyReview
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyReviewDao {

    @Query("SELECT * FROM daily_reviews ORDER BY date DESC")
    fun observeAll(): Flow<List<DailyReview>>

    @Query("SELECT * FROM daily_reviews WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: LocalDate): DailyReview?

    @Query("SELECT * FROM daily_reviews WHERE date = :date LIMIT 1")
    fun observeByDate(date: LocalDate): Flow<DailyReview?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: DailyReview)

    @Update
    suspend fun update(review: DailyReview)

    @Delete
    suspend fun delete(review: DailyReview)
}
