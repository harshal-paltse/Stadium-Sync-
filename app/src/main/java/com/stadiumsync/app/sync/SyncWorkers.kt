package com.stadiumsync.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.stadiumsync.app.core.datastore.UserPreferences
import com.stadiumsync.app.data.local.dao.SyncQueueDao
import com.stadiumsync.app.domain.repository.SyncRepository
import com.stadiumsync.app.domain.repository.TransitRepository
import com.stadiumsync.app.domain.repository.CrowdRepository
import com.stadiumsync.app.domain.model.MatchPhase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepo: SyncRepository,
    private val prefs: UserPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = syncRepo.processPendingSync()
            result.fold(
                onSuccess = {
                    prefs.setLastSync(System.currentTimeMillis())
                    Result.success()
                },
                onFailure = {
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            )
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

@HiltWorker
class DataRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transitRepo: TransitRepository,
    private val crowdRepo: CrowdRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            transitRepo.refreshRoutes()
            crowdRepo.refreshCrowdData(MatchPhase.MIDDLE_OVERS)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

object SyncScheduler {
    fun schedulePeriodicSync(context: Context) {
        val syncRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("stadium_sync_periodic")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "stadium_sync_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        val refreshRequest = PeriodicWorkRequestBuilder<DataRefreshWorker>(5, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("stadium_data_refresh")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "stadium_data_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            refreshRequest
        )
    }

    fun triggerImmediateSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
