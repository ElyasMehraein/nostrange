package com.nostrange.app.ui.chats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nostrange.app.NostrangeApp
import com.nostrange.app.domain.model.ChatMessage
import com.nostrange.app.domain.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NostrangeApp
    private val chatRepo = app.chatRepository

    val conversationsFlow: Flow<List<Conversation>> = chatRepo.conversationsFlow

    init {
        chatRepo.startMessageSync()
    }
}

class ChatDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NostrangeApp
    private val chatRepo = app.chatRepository
    private val candidateRepo = app.candidateRepository

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _partnerScore = MutableStateFlow(0.0)
    val partnerScore: StateFlow<Double> = _partnerScore.asStateFlow()

    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked.asStateFlow()

    fun loadChat(partnerPubkey: String) {
        viewModelScope.launch {
            _isBlocked.value = chatRepo.isPubkeyBlocked(partnerPubkey)
            val candidate = candidateRepo.getCandidateByPubkey(partnerPubkey)
            _partnerScore.value = candidate?.aiScore ?: candidate?.initial_score ?: 0.0

            chatRepo.getMessagesForConversation(partnerPubkey).collect { list ->
                _messages.value = list
            }
        }
    }

    fun sendMessage(partnerPubkey: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepo.sendMessage(partnerPubkey, text.trim())
        }
    }

    fun blockUser(partnerPubkey: String, onBlocked: () -> Unit) {
        viewModelScope.launch {
            chatRepo.blockPubkey(partnerPubkey, "Spam blocked by user")
            _isBlocked.value = true
            onBlocked()
        }
    }
}
