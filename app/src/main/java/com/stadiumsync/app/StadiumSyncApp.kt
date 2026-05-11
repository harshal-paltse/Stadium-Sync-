package com.stadiumsync.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StadiumSyncApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_CRITICAL, "Critical Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "High-priority alerts for crowd surges and match endings" },
            NotificationChannel(
                CHANNEL_WARNING, "Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Transport delays and sync warnings" },
            NotificationChannel(
                CHANNEL_INFO, "Information",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "General status updates" }
        )
        val nm = getSystemService(NotificationManager::class.java)
        channels.forEach { nm.createNotificationChannel(it) }
    }

    companion object {
        const val CHANNEL_CRITICAL = "stadium_sync_critical"
        const val CHANNEL_WARNING = "stadium_sync_warning"
        const val CHANNEL_INFO = "stadium_sync_info"
    }
}
