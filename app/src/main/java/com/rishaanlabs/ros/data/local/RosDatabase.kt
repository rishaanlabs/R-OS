package com.rishaanlabs.ros.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rishaanlabs.ros.data.local.dao.*
import com.rishaanlabs.ros.data.local.entity.*

/**
 * The single Room database for the application.
 * Version 1 is the initial schema. Bump the version number whenever the schema changes,
 * and provide a Migration implementation to preserve user data.
 */
@Database(
    entities = [
        InboxItem::class,
        Project::class,
        Task::class,
        WaitingItem::class,
        Note::class,
        DailyReview::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RosDatabase : RoomDatabase() {
    abstract fun inboxDao(): InboxDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun waitingDao(): WaitingDao
    abstract fun noteDao(): NoteDao
    abstract fun dailyReviewDao(): DailyReviewDao
}
