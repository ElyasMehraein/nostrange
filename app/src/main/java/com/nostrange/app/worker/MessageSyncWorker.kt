package com.nostrange.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nostrange.app.NostrangeApp
import kotlinx.coroutines.delay

/**
 * Standard Android WorkManager Worker that checks for incoming encrypted messages
 * on a 15-minute periodic schedule.
 */
class MessageSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = appContext.applicationContext as? NostrangeApp ?: return Result.success()

            // Trigger sync of Nostr encrypted messages
            app.chatRepository.startMessageSync()

            // Allow WebSocket relays time to deliver any pending messages
            delay(5000)

            Result.success()
        } catch (e: Exception) {
            Log.w("MessageSyncWorker", "Periodic background sync encountered error: ${e.message}")
            Result.retry()
        }
    }
}
