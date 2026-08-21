package com.nostrange.app.data.repository

import com.nostrange.app.data.local.dao.RelayDao
import com.nostrange.app.data.local.entity.RelayEntity
import com.nostrange.app.data.nostr.NostrClient
import com.nostrange.app.domain.model.Relay
import com.nostrange.app.domain.model.RelayStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RelayRepository(
    private val relayDao: RelayDao,
    private val nostrClient: NostrClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    val relaysFlow: Flow<List<Relay>> = relayDao.getRelays().combine(nostrClient.relaysStatus) { entities, statusMap ->
        entities.map { entity ->
            Relay(
                url = entity.url,
                read = entity.read,
                write = entity.write,
                status = statusMap[entity.url] ?: RelayStatus.DISCONNECTED
            )
        }
    }

    init {
        scope.launch {
            val savedRelays = relayDao.getRelaysOnce()
            val urls = if (savedRelays.isNotEmpty()) {
                savedRelays.map { it.url }
            } else {
                listOf(
                    "wss://relay.damus.io",
                    "wss://nos.lol",
                    "wss://relay.primal.net"
                )
            }
            nostrClient.connectRelays(urls)
        }
    }

    suspend fun addRelay(url: String) = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (cleanUrl.startsWith("wss://") || cleanUrl.startsWith("ws://")) {
            relayDao.insertRelay(RelayEntity(cleanUrl, read = true, write = true))
            nostrClient.connectRelay(cleanUrl)
        }
    }

    suspend fun removeRelay(url: String) = withContext(Dispatchers.IO) {
        relayDao.deleteRelay(url)
        nostrClient.disconnectRelay(url)
    }

    suspend fun reconnectRelay(url: String) = withContext(Dispatchers.IO) {
        nostrClient.connectRelay(url)
    }
}
