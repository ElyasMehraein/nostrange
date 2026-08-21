package com.nostrange.app.ui.me

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nostrange.app.NostrangeApp
import com.nostrange.app.ai.prompt.ProfilePromptGenerator
import com.nostrange.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingQuestion(
    val id: String,
    val text: String,
    val placeholder: String
)

data class ChatMessageItem(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val questionId: String? = null
)

class MeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NostrangeApp
    private val profileRepo = app.profileRepository
    private val keyStoreManager = app.keyStoreManager

    val userProfileFlow = profileRepo.userProfileFlow

    val questions = listOf(
        OnboardingQuestion("intro", "سلام! برای پیدا کردن افراد سازگار با شما، چند سؤال کوتاه می‌پرسم. ابتدا بفرمایید چند سال دارید؟", "مثلاً 30"),
        OnboardingQuestion("location", "در کدام کشور و شهر یا استان زندگی می‌کنید؟", "مثلاً ایران، تهران"),
        OnboardingQuestion("gender", "جنسیت شما چیست و مایلید با چه جنسیتی آشنا شوید؟", "مثلاً آقا هستم و مایل به آشنایی با خانم‌ها"),
        OnboardingQuestion("goal", "هدف اصلی شما از این ارتباط چیست؟ آیا قصد ازدواج یا رابطه بلندمدت دارید؟", "مثلاً رابطه بلندمدت با هدف ازدواج"),
        OnboardingQuestion("children", "آیا تمایل به داشتن فرزند دارید؟", "مثلاً بله، علاقه‌مند به داشتن فرزند هستم"),
        OnboardingQuestion("lifestyle", "سبک زندگی، میزان ورزش، سفر و ساعات کاری شما چگونه است؟", "مثلاً ورزش هفتگی، اهل مسافرت در تعطیلات، ساعات اداری"),
        OnboardingQuestion("personality", "ویژگی‌های بارز شخصیتی شما چیست؟ (میزان استقلال، درونگرایی یا برونگرایی، میزان خونسردی و برنامه‌ریزی)", "مثلاً مستقل، متعادل بین درونگرا و برونگرا، منظم"),
        OnboardingQuestion("values", "مهم‌ترین ارزش‌ها و خط قرمزهای شما در زندگی و رابطه چیست؟", "مثلاً صداقت، احترام متقابل، رشد فردی، غیرسیگاری"),
        OnboardingQuestion("interests", "به چه سرگرمی‌ها و علایقی بیشتر علاقه دارید؟", "مثلاً کتابخوانی، طبیعت‌گردی، سینما، تکنولوژی"),
        OnboardingQuestion("partner_pref", "طرف مقابل ایده‌آل شما بهتر است چه محدوده سنی یا ویژگی‌هایی داشته باشد؟", "مثلاً بازه سنی ۲۴ تا ۳۴ سال، ساکن تهران یا البرز")
    )

    private val _chatMessages = MutableStateFlow<List<ChatMessageItem>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageItem>> = _chatMessages.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _answers = MutableStateFlow<Map<String, String>>(emptyMap())
    val answers: StateFlow<Map<String, String>> = _answers.asStateFlow()

    private val _generatedPrompt = MutableStateFlow<String?>(null)
    val generatedPrompt: StateFlow<String?> = _generatedPrompt.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    init {
        // Start interview with first question
        if (questions.isNotEmpty()) {
            _chatMessages.value = listOf(
                ChatMessageItem(
                    id = "bot_0",
                    isUser = false,
                    text = questions[0].text,
                    questionId = questions[0].id
                )
            )
        }
    }

    fun submitAnswer(answerText: String) {
        if (answerText.isBlank()) return

        val index = _currentQuestionIndex.value
        if (index < questions.size) {
            val q = questions[index]
            val updatedAnswers = _answers.value.toMutableMap()
            updatedAnswers[q.text] = answerText.trim()
            _answers.value = updatedAnswers

            val currentList = _chatMessages.value.toMutableList()
            currentList.add(
                ChatMessageItem(
                    id = "user_${index}_${System.currentTimeMillis()}",
                    isUser = true,
                    text = answerText.trim(),
                    questionId = q.id
                )
            )

            val nextIndex = index + 1
            if (nextIndex < questions.size) {
                val nextQ = questions[nextIndex]
                currentList.add(
                    ChatMessageItem(
                        id = "bot_$nextIndex",
                        isUser = false,
                        text = nextQ.text,
                        questionId = nextQ.id
                    )
                )
                _currentQuestionIndex.value = nextIndex
            } else {
                currentList.add(
                    ChatMessageItem(
                        id = "bot_done",
                        isUser = false,
                        text = "گفتگو تکمیل شد! متشکرم. اکنون می‌توانید پرامپت اختصاصی هوش مصنوعی را تولید کرده و به هوش مصنوعی انتخابی خود (ChatGPT، Gemini، Grok و غیره) بدهید تا پروفایل استاندارد Nostrange شما را بسازد."
                    )
                )
                _currentQuestionIndex.value = nextIndex
            }
            _chatMessages.value = currentList
        }
    }

    fun restartInterview() {
        _answers.value = emptyMap()
        _currentQuestionIndex.value = 0
        _chatMessages.value = listOf(
            ChatMessageItem(
                id = "bot_0",
                isUser = false,
                text = questions[0].text,
                questionId = questions[0].id
            )
        )
    }

    fun generateAiPrompt() {
        val userPubkey = keyStoreManager.getPublicKeyHex()
        val prompt = ProfilePromptGenerator.generateProfileCreationPrompt(_answers.value, userPubkey)
        _generatedPrompt.value = prompt
    }

    fun clearGeneratedPrompt() {
        _generatedPrompt.value = null
    }

    fun copyPromptToClipboard(context: Context, prompt: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Nostrange Profile Prompt", prompt)
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

    fun importProfileJson(rawJson: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null

            val result = profileRepo.importProfileJson(rawJson)
            result.onSuccess {
                _isImporting.value = false
                onSuccess()
            }.onFailure { ex ->
                _isImporting.value = false
                _importError.value = "خطا در پردازش JSON: ${ex.message}"
            }
        }
    }
}
