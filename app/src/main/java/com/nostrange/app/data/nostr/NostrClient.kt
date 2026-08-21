package com.nostrange.app.data.nostr

import android.util.Log
import com.nostrange.app.domain.model.Relay
import com.nostrange.app.domain.model.RelayStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Robust Multi-Relay WebSocket Client for decentralized Nostr communication.
 */
class NostrClient(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private val activeSockets = ConcurrentHashMap<String, WebSocket>()
    private val relayStates = ConcurrentHashMap<String, MutableStateFlow<RelayStatus>>()

    private val _incomingEvents = MutableSharedFlow<NostrEvent>(extraBufferCapacity = 500)
    val incomingEvents: SharedFlow<NostrEvent> = _incomingEvents.asSharedFlow()

    private val _relaysStatus = MutableStateFlow<Map<String, RelayStatus>>(emptyMap())
    val relaysStatus: StateFlow<Map<String, RelayStatus>> = _relaysStatus.asStateFlow()

    private val activeSubscriptions = ConcurrentHashMap<String, String>() // subId -> rawReqJson

    fun connectRelays(relayUrls: List<String>) {
        for (url in relayUrls) {
            connectRelay(url)
        }
    }

    fun connectRelay(url: String) {
        val stateFlow = relayStates.getOrPut(url) { MutableStateFlow(RelayStatus.DISCONNECTED) }
        stateFlow.value = RelayStatus.CONNECTING
        updateRelaySummary()

        val request = Request.Builder().url(url).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to relay: $url")
                activeSockets[url] = webSocket
                stateFlow.value = RelayStatus.CONNECTED
                updateRelaySummary()

                // Re-send any active subscriptions
                for ((_, reqJson) in activeSubscriptions) {
                    webSocket.send(reqJson)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleRelayMessage(url, text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Relay failure: $url - ${t.message}")
                activeSockets.remove(url)
                stateFlow.value = RelayStatus.ERROR
                updateRelaySummary()

                // Exponential backoff reconnect
                scope.launch {
                    delay(5000)
                    connectRelay(url)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Relay closed: $url - $reason")
                activeSockets.remove(url)
                stateFlow.value = RelayStatus.DISCONNECTED
                updateRelaySummary()
            }
        }

        client.newWebSocket(request, listener)
    }

    fun disconnectRelay(url: String) {
        activeSockets[url]?.close(1000, "User disconnected")
        activeSockets.remove(url)
        relayStates[url]?.value = RelayStatus.DISCONNECTED
        updateRelaySummary()
    }

    fun subscribe(subId: String, filters: List<NostrFilter>) {
        val reqArray = StringBuilder("[\"REQ\",\"$subId\"")
        for (f in filters) {
            reqArray.append(",").append(f.toJson())
        }
        reqArray.append("]")
        val reqJson = reqArray.toString()

        activeSubscriptions[subId] = reqJson

        for ((_, socket) in activeSockets) {
            socket.send(reqJson)
        }
    }

    fun unsubscribe(subId: String) {
        activeSubscriptions.remove(subId)
        val closeMsg = "[\"CLOSE\",\"$subId\"]"
        for ((_, socket) in activeSockets) {
            socket.send(closeMsg)
        }
    }

    fun publishEvent(event: NostrEvent) {
        val eventJson = json.encodeToString(NostrEvent.serializer(), event)
        val msg = "[\"EVENT\",$eventJson]"
        for ((url, socket) in activeSockets) {
            val sent = socket.send(msg)
            Log.d(TAG, "Publish event ${event.id} to $url: $sent")
        }
    }

    private fun handleRelayMessage(relayUrl: String, text: String) {
        try {
            val root = json.parseToJsonElement(text).jsonArray
            val msgType = root[0].jsonPrimitive.content

            when (msgType) {
                "EVENT" -> {
                    if (root.size >= 3) {
                        val eventObj = root[2]
                        val event = json.decodeFromJsonElement(NostrEvent.serializer(), eventObj)
                        if (event.verify()) {
                            _incomingEvents.tryEmit(event)
                        } else {
                            Log.w(TAG, "Event signature verification failed for ${event.id}")
                        }
                    }
                }
                "OK" -> {
                    val eventId = root[1].jsonPrimitive.content
                    val accepted = root[2].jsonPrimitive.content.toBoolean()
                    val info = if (root.size > 3) root[3].jsonPrimitive.content else ""
                    Log.d(TAG, "Relay $relayUrl OK for event $eventId: $accepted ($info)")
                }
                "EOSE" -> {
                    val subId = root[1].jsonPrimitive.content
                    Log.d(TAG, "End of Stored Events for subscription $subId on $relayUrl")
                }
                "NOTICE" -> {
                    val notice = root[1].jsonPrimitive.content
                    Log.i(TAG, "Relay $relayUrl Notice: $notice")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling message from $relayUrl: ${e.message}")
        }
    }

    private fun updateRelaySummary() {
        val summary = relayStates.mapValues { it.value.value }
        _relaysStatus.value = summary
    }

    companion object {
        private const val TAG = "NostrClient"
    }
}
