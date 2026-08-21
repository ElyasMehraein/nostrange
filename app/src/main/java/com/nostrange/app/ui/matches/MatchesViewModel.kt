package com.nostrange.app.ui.matches

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nostrange.app.NostrangeApp
import com.nostrange.app.ai.prompt.MatchingPromptGenerator
import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MatchesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NostrangeApp
    private val candidateRepo = app.candidateRepository
    private val profileRepo = app.profileRepository
    private val chatRepo = app.chatRepository

    val topAiMatches = candidateRepo.topAiMatches
    val allCandidates = candidateRepo.allCandidates
    val candidateCount = candidateRepo.candidateCount

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatedPrompt = MutableStateFlow<String?>(null)
    val generatedPrompt: StateFlow<String?> = _generatedPrompt.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    init {
        candidateRepo.startCandidateSync()
    }

    fun generateMatchingPrompt() {
        viewModelScope.launch {
            _isGenerating.value = true
            val user = profileRepo.getUserProfileOnce()
            if (user == null) {
                _isGenerating.value = false
                _importError.value = "ابتدا در تب 'Me' پروفایل خود را بسازید."
                return@launch
            }

            val top500 = candidateRepo.runLocalCandidateGeneration(user, 500)
            if (top500.isEmpty()) {
                _isGenerating.value = false
                _importError.value = "هیچ کاندیدایی مطابق با فیلترهای اولیه شما یافت نشد."
                return@launch
            }

            val prompt = MatchingPromptGenerator.generateRankingPrompt(user, top500, 50)
            _generatedPrompt.value = prompt
            _isGenerating.value = false
        }
    }

    fun clearGeneratedPrompt() {
        _generatedPrompt.value = null
    }

    fun copyPromptToClipboard(context: Context, prompt: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Nostrange Matching Prompt", prompt)
        clipboard.setPrimaryClip(clip)
    }

    fun sharePrompt(context: Context, prompt: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, "ارسال به هوش مصنوعی")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }

    fun importMatchingResult(rawJson: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null

            val result = candidateRepo.importAiMatchingResult(rawJson)
            result.onSuccess { count ->
                _isImporting.value = false
                onSuccess()
            }.onFailure { ex ->
                _isImporting.value = false
                _importError.value = "خطا در اعتبارسنجی رتبه‌بندی: ${ex.message}"
            }
        }
    }

    fun requestIntroduction(candidate: CandidateProfile, onIntroSent: () -> Unit) {
        viewModelScope.launch {
            val user = profileRepo.getUserProfileOnce()
            val score = candidate.aiScore ?: candidate.initial_score
            val introMsg = "سلام! یک کاربر با امتیاز سازگاری ${String.format(java.util.Locale.US, "%.0f", score)}% در Nostrange با مشخصات شما منطبق شده است. در صورت تمایل می‌توانید گفتگو را ادامه دهید."

            val result = chatRepo.sendIntroductionRequest(candidate.pubkey, score, introMsg)
            if (result.isSuccess) {
                candidateRepo.markIntroSent(candidate.pubkey)
                onIntroSent()
            }
        }
    }
}
