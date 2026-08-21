package com.nostrange.app.data.repository

import android.util.Log
import com.nostrange.app.data.local.dao.BlockedPubkeyDao
import com.nostrange.app.data.local.dao.CandidateDao
import com.nostrange.app.data.local.dao.MessageDao
import com.nostrange.app.data.local.entity.BlockedPubkeyEntity
import com.nostrange.app.data.local.entity.MessageEntity
import com.nostrange.app.data.nostr.NostrClient
import com.nostrange.app.data.nostr.NostrEventKind
import com.nostrange.app.data.nostr.NostrFilter
import com.nostrange.app.data.nostr.NostrSigner
import com.nostrange.app.domain.model.ChatMessage
import com.nostrange.app.domain.model.Conversation
import com.nostrange.app.security.Bech32
import com.nostrange.app.security.KeyStoreManager
import com.nostrange.app.security.Nip44Cipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val messageDao: MessageDao,
    private val blockedPubkeyDao: BlockedPubkeyDao,
    private val candidateDao: CandidateDao,
    private val keyStoreManager: KeyStoreManager,
    private val nostrClient: NostrClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    val conversationsFlow: Flow<List<Conversation>> = messageDao.getAllConversationPubkeys()
        .combine(blockedPubkeyDao.getBlockedPubkeys()) { pubkeys, blockedList ->
            val blockedSet = blockedList.map { it.pubkey }.toSet()
            pubkeys.filterNot { blockedSet.contains(it) }.map { pubkey ->
                val latestMsg = messageDao.getLatestMessage(pubkey)
                val candidate = candidateDao.getCandidateByPubkey(pubkey)
                val score = candidate?.aiScore ?: candidate?.initialScore ?: 0.0

                Conversation(
                    partnerPubkey = pubkey,
                    lastMessage = latestMsg?.content ?: "",
                    lastTimestamp = latestMsg?.timestamp ?: 0L,
                    matchScore = score,
                    isBlocked = false
                )
            }.sortedByDescending { it.lastTimestamp }
        }

    init {
        // Listen to incoming Nostr encrypted direct messages (Kind 4 / Kind 14 / Kind 1059)
        scope.launch {
            nostrClient.incomingEvents.collect { event ->
                if (event.kind == NostrEventKind.DIRECT_MESSAGE_NIP04 || event.kind == NostrEventKind.CHAT_MESSAGE_NIP17) {
                    processIncomingMessageEvent(event.id, event.pubkey, event.tags, event.content, event.createdAt)
                }
            }
        }
    }

    fun startMessageSync() {
        val userPubkey = keyStoreManager.getPublicKeyHex()
        val filter = NostrFilter(
            kinds = listOf(NostrEventKind.DIRECT_MESSAGE_NIP04, NostrEventKind.CHAT_MESSAGE_NIP17),
            pTags = listOf(userPubkey),
            limit = 200
        )
        nostrClient.subscribe("nostrange-messages-sub", listOf(filter))
    }

    fun getMessagesForConversation(conversationPubkey: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationPubkey).map { list ->
            list.map {
                ChatMessage(
                    id = it.id,
                    conversationPubkey = it.conversationPubkey,
                    senderPubkey = it.senderPubkey,
                    content = it.content,
                    timestamp = it.timestamp,
                    isOutgoing = it.isOutgoing,
                    isRead = it.isRead
                )
            }
        }
    }

    /**
     * Sends an encrypted private message to recipient using NIP-44 v2 encryption.
     */
    suspend fun sendMessage(recipientPubkey: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (blockedPubkeyDao.isBlocked(recipientPubkey)) {
                throw IllegalStateException("Cannot send message to blocked pubkey")
            }

            val privKey = keyStoreManager.getPrivateKeyBytes()
            val userPubkey = keyStoreManager.getPublicKeyHex()
            val recipientPubBytes = Bech32.hexToBytes(recipientPubkey)

            // Derive conversation key & encrypt using NIP-44 v2
            val conversationKey = Nip44Cipher.getConversationKey(privKey, recipientPubBytes)
            val encryptedPayload = Nip44Cipher.encrypt(text, conversationKey)

            val tags = listOf(
                listOf("p", recipientPubkey)
            )

            val event = NostrSigner.createAndSignEvent(
                privateKey = privKey,
                kind = NostrEventKind.DIRECT_MESSAGE_NIP04,
                tags = tags,
                content = encryptedPayload
            )

            // Save locally
            val localMessage = MessageEntity(
                id = event.id,
                conversationPubkey = recipientPubkey,
                senderPubkey = userPubkey,
                content = text,
                timestamp = event.createdAt,
                isOutgoing = true,
                isRead = true
            )
            messageDao.insertMessage(localMessage)

            // Publish to Nostr relays
            nostrClient.publishEvent(event)
        }
    }

    /**
     * Introduction Protocol:
     * When user taps "Request Introduction", sends an initial introduction message
     * containing match info without revealing real name or contact info.
     */
    suspend fun sendIntroductionRequest(
        recipientPubkey: String,
        matchScore: Double,
        introMessage: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val result = sendMessage(recipientPubkey, introMessage)
        if (result.isSuccess) {
            candidateDao.markIntroSent(recipientPubkey)
        }
        result
    }

    suspend fun blockPubkey(pubkey: String, reason: String? = null) = withContext(Dispatchers.IO) {
        blockedPubkeyDao.blockPubkey(BlockedPubkeyEntity(pubkey, reason))
        candidateDao.blockCandidate(pubkey)
    }

    suspend fun unblockPubkey(pubkey: String) = withContext(Dispatchers.IO) {
        blockedPubkeyDao.unblockPubkey(pubkey)
    }

    suspend fun isPubkeyBlocked(pubkey: String): Boolean = withContext(Dispatchers.IO) {
        blockedPubkeyDao.isBlocked(pubkey)
    }

    private suspend fun processIncomingMessageEvent(
        eventId: String,
        senderPubkey: String,
        tags: List<List<String>>,
        encryptedContent: String,
        createdAt: Long
    ) {
        val userPubkey = keyStoreManager.getPublicKeyHex()
        if (senderPubkey.equals(userPubkey, ignoreCase = true)) return // Ignore own echoes
        if (blockedPubkeyDao.isBlocked(senderPubkey)) return // Spam protection

        val isTargetedToMe = tags.any { it.size >= 2 && it[0] == "p" && it[1].equals(userPubkey, ignoreCase = true) }
        if (!isTargetedToMe) return

        try {
            val privKey = keyStoreManager.getPrivateKeyBytes()
            val senderPubBytes = Bech32.hexToBytes(senderPubkey)
            val conversationKey = Nip44Cipher.getConversationKey(privKey, senderPubBytes)
            val decryptedText = Nip44Cipher.decrypt(encryptedContent, conversationKey)

            val msg = MessageEntity(
                id = eventId,
                conversationPubkey = senderPubkey,
                senderPubkey = senderPubkey,
                content = decryptedText,
                timestamp = createdAt,
                isOutgoing = false,
                isRead = false
            )
            messageDao.insertMessage(msg)
        } catch (e: Exception) {
            Log.w("ChatRepo", "Failed to decrypt incoming message from $senderPubkey: ${e.message}")
        }
    }
}
