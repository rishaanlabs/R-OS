package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

/**
 * A lightweight daily review entry. Captures context around each day.
 * All fields except the date are optional — the review should take 1-3 minutes.
 */
@Entity(tableName = "daily_reviews")
data class DailyReview(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: LocalDate = LocalDate.now(),
    val energy: Int? = null, // 1-5
    val mainFocus: String = "",
    val biggestWin: String = "",
    val unfinished: String = "",
    val toRemember: String = "",
    val tomorrowPriority: String = ""
)
