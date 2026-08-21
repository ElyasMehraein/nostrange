package com.nostrange.app

import android.app.Application
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
        ChatRepository(database.messageDao(), database.blockedPubkeyDao(), database.candidateDao(), keyStoreManager, nostrClient)
    }

    val relayRepository by lazy {
        RelayRepository(database.relayDao(), nostrClient)
    }

    val appFunctions by lazy {
        NostrangeAppFunctions(profileRepository, candidateRepository, keyStoreManager)
    }

    override fun onCreate() {
        super.onCreate()
        // Ensure hardware-backed Nostr keypair exists
        keyStoreManager.getOrCreateKeypair()

        // Broadcast updated online timestamp to Nostr relays
        CoroutineScope(Dispatchers.IO).launch {
            profileRepository.broadcastOnlineStatus()
        }
    }
}
