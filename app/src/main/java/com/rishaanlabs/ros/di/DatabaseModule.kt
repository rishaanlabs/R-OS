package com.rishaanlabs.ros.di

import android.content.Context
import androidx.room.Room
import com.rishaanlabs.ros.data.local.RosDatabase
import com.rishaanlabs.ros.data.local.dao.*
import com.rishaanlabs.ros.data.local.migration.FINANCE_MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RosDatabase =
        Room.databaseBuilder(context, RosDatabase::class.java, "ros_database")
            .addMigrations(FINANCE_MIGRATION_1_2)
            .build()

    @Provides fun provideInboxDao(db: RosDatabase): InboxDao = db.inboxDao()
    @Provides fun provideProjectDao(db: RosDatabase): ProjectDao = db.projectDao()
    @Provides fun provideTaskDao(db: RosDatabase): TaskDao = db.taskDao()
    @Provides fun provideWaitingDao(db: RosDatabase): WaitingDao = db.waitingDao()
    @Provides fun provideNoteDao(db: RosDatabase): NoteDao = db.noteDao()
    @Provides fun provideDailyReviewDao(db: RosDatabase): DailyReviewDao = db.dailyReviewDao()
    @Provides fun provideFinanceDao(db: RosDatabase): FinanceDao = db.financeDao()
}
