package com.nostrange.app

import android.app.Application
import android.util.Log
import com.nostrange.app.ai.appfunctions.NostrangeAppFunctions
import com.nostrange.app.data.local.AppDatabase
import com.nostrange.app.data.nostr.NostrClient
import com.nostrange.app.data.repository.CandidateRepository
import com.nostrange.app.data.repository.ChatRepository
import com.nostrange.app.data.repository.ProfileRepository
import com.nostrange.app.data.repository.RelayRepository
import com.nostrange.app.security.KeyStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nostrange.app.notifications.NotificationHelper
import com.nostrange.app.worker.MessageSyncWorker
import java.util.concurrent.TimeUnit

class NostrangeApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val keyStoreManager by lazy { KeyStoreManager(this) }
    val nostrClient by lazy { NostrClient() }

    val profileRepository by lazy {
        ProfileRepository(database.profileDao(), keyStoreManager, nostrClient)
    }

    val candidateRepository by lazy {
        CandidateRepository(database.candidateDao(), database.blockedPubkeyDao(), nostrClient)
    }

    val chatRepository by lazy {
        ChatRepository(this, database.messageDao(), database.blockedPubkeyDao(), database.candidateDao(), keyStoreManager, nostrClient)
    }

    val relayRepository by lazy {
        RelayRepository(database.relayDao(), nostrClient)
    }

    val appFunctions by lazy {
        NostrangeAppFunctions(profileRepository, candidateRepository, keyStoreManager)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            // 1. Create notification channels for high-priority message alerts
            NotificationHelper.createNotificationChannel(this)

            // 2. Ensure hardware-backed Nostr keypair exists
            keyStoreManager.getOrCreateKeypair()

            // 3. Setup Android WorkManager 15-Minute Periodic Message Sync
            setupPeriodicMessageSyncWorker()

            // 4. Broadcast updated online timestamp to Nostr relays safely in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    profileRepository.broadcastOnlineStatus()
                } catch (e: Exception) {
                    Log.w("NostrangeApp", "Failed to broadcast online status on launch: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NostrangeApp", "Error during app onCreate: ${e.message}", e)
        }
    }

    private fun setupPeriodicMessageSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<MessageSyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "nostrange_15min_message_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}
