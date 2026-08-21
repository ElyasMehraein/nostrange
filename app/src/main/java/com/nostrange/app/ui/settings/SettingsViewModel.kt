package com.nostrange.app.ui.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nostrange.app.NostrangeApp
import com.nostrange.app.domain.model.Relay
import com.nostrange.app.domain.model.RelayStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NostrangeApp
    private val relayRepo = app.relayRepository
    private val candidateRepo = app.candidateRepository
    private val keyStoreManager = app.keyStoreManager

    val relaysFlow: Flow<List<Relay>> = relayRepo.relaysFlow
    val candidateCount: Flow<Int> = candidateRepo.candidateCount

    val userPubkeyHex = keyStoreManager.getPublicKeyHex()
    val userPubkeyNpub = keyStoreManager.getPublicKeyNpub()

    fun addRelay(url: String) {
        viewModelScope.launch {
            relayRepo.addRelay(url)
        }
    }

    fun removeRelay(url: String) {
        viewModelScope.launch {
            relayRepo.removeRelay(url)
        }
    }

    fun reconnectRelay(url: String) {
        viewModelScope.launch {
            relayRepo.reconnectRelay(url)
        }
    }

    fun clearCandidateCache() {
        viewModelScope.launch {
            candidateRepo.clearAllCandidates()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
