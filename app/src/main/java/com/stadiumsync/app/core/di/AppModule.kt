package com.stadiumsync.app.core.di

import android.content.Context
import androidx.room.Room
import com.stadiumsync.app.data.local.StadiumSyncDatabase
import com.stadiumsync.app.data.local.dao.*
import com.stadiumsync.app.data.repository.*
import com.stadiumsync.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): StadiumSyncDatabase =
        Room.databaseBuilder(ctx, StadiumSyncDatabase::class.java, "stadium_sync.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun matchDao(db: StadiumSyncDatabase): MatchDao = db.matchDao()
    @Provides fun crowdDao(db: StadiumSyncDatabase): CrowdDataDao = db.crowdDataDao()
    @Provides fun transitRouteDao(db: StadiumSyncDatabase): TransitRouteDao = db.transitRouteDao()
    @Provides fun transitActionDao(db: StadiumSyncDatabase): TransitActionDao = db.transitActionDao()
    @Provides fun notificationDao(db: StadiumSyncDatabase): NotificationDao = db.notificationDao()
    @Provides fun syncQueueDao(db: StadiumSyncDatabase): SyncQueueDao = db.syncQueueDao()
    @Provides fun auditLogDao(db: StadiumSyncDatabase): AuditLogDao = db.auditLogDao()
    @Provides fun predictionDao(db: StadiumSyncDatabase): PredictionDao = db.predictionDao()
    @Provides fun ticketDao(db: StadiumSyncDatabase): TicketDao = db.ticketDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun matchRepo(impl: MatchRepositoryImpl): MatchRepository
    @Binds abstract fun crowdRepo(impl: CrowdRepositoryImpl): CrowdRepository
    @Binds abstract fun transitRepo(impl: TransitRepositoryImpl): TransitRepository
    @Binds abstract fun authRepo(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun notificationRepo(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds abstract fun syncRepo(impl: SyncRepositoryImpl): SyncRepository
    @Binds abstract fun analyticsRepo(impl: AnalyticsRepositoryImpl): AnalyticsRepository
    @Binds abstract fun settingsRepo(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun ticketRepo(impl: TicketRepositoryImpl): TicketRepository
}
