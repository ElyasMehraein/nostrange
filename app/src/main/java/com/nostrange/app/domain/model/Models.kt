package com.nostrange.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Personality(
    val independence: Int = 50,          // 0 = بسیار وابسته, 100 = بسیار مستقل
    val sociability: Int = 50,           // 0 = درونگرا/خلوت‌گزین, 100 = برونگرا/بسیار اجتماعی
    val openness: Int = 50,              // 0 = پایبند به سنت/تغییرگریز, 100 = بسیار باز به تجربیات نو
    val emotional_stability: Int = 50,   // 0 = حساس/نوسانی, 100 = بسیار صبور و باثبات
    val agreeableness: Int = 50,         // 0 = رقابتی/رک, 100 = اهل سازش و همدل
    val conscientiousness: Int = 50      // 0 = منعطف/بی‌برنامه, 100 = بسیار دقیق و با برنامه
)

@Serializable
data class Lifestyle(
    val activity_level: Int = 50,        // 0 = کم‌تحرک, 100 = بسیار فعال و ورزشی
    val travel: Int = 50,                // 0 = اهل خانه, 100 = همواره در سفر
    val social_life: Int = 50,           // 0 = جمع‌های کوچک خانوادگی, 100 = مهمانی و رویدادهای مکرر
    val intellectual_curiosity: Int = 50,// 0 = عملگرا, 100 = اهل مطالعه و گفتگوهای عمیق
    val economic_style: Int = 50         // 0 = اهل پس‌انداز و آینده‌نگر, 100 = اهل لذت در لحظه و ولخرج
)

@Serializable
data class Preferences(
    val min_age: Int = 18,
    val max_age: Int = 70,
    val max_distance_km: Int = 500,
    val allow_different_country: Boolean = false
)

/**
 * Clean structured profile model.
 * ABSOLUTE ZERO MEDIA & ZERO CONTACT INFO GUARANTEE:
 * No name, photo, phone, email, telegram, instagram or contact fields.
 */
@Serializable
data class UserProfile(
    val schema_version: Int = 1,
    val pubkey: String,
    val country: String,
    val region: String,
    val age: Int,
    val gender: String,                          // male, female, non_binary, etc.
    val target_genders: List<String>,            // ["female"], ["male"], etc.
    val relationship_goal: String,               // long_term, marriage, casual, friendship, etc.
    val wants_marriage: Boolean = true,
    val wants_children: Boolean = true,
    val personality: Personality = Personality(),
    val lifestyle: Lifestyle = Lifestyle(),
    val interests: List<String> = emptyList(),
    val values: List<String> = emptyList(),
    val preferences: Preferences = Preferences(),
    val deal_breakers: List<String> = emptyList(),
    val created_at: Long = System.currentTimeMillis() / 1000
)

@Serializable
data class CandidateProfile(
    val pubkey: String,
    val schema_version: Int = 1,
    val country: String,
    val region: String,
    val age: Int,
    val gender: String,
    val target_genders: List<String>,
    val relationship_goal: String,
    val wants_marriage: Boolean,
    val wants_children: Boolean,
    val personality: Personality = Personality(),
    val lifestyle: Lifestyle = Lifestyle(),
    val interests: List<String> = emptyList(),
    val values: List<String> = emptyList(),
    val deal_breakers: List<String> = emptyList(),
    val initial_score: Double = 0.0,
    val ai_rank: Int? = null,
    val ai_score: Double? = null,
    val ai_reasons: List<String> = emptyList(),
    val is_intro_sent: Boolean = false,
    val is_blocked: Boolean = false,
    val updated_at: Long = System.currentTimeMillis() / 1000
) {
    val aiScore: Double? get() = ai_score
    val aiRank: Int? get() = ai_rank
    val aiReasons: List<String> get() = ai_reasons
    val initialScore: Double get() = initial_score
}

@Serializable
data class ChatMessage(
    val id: String,
    val conversationPubkey: String,
    val senderPubkey: String,
    val content: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val isRead: Boolean = true
)

data class Conversation(
    val partnerPubkey: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0,
    val matchScore: Double = 0.0,
    val isBlocked: Boolean = false
)

enum class RelayStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class Relay(
    val url: String,
    val read: Boolean = true,
    val write: Boolean = true,
    val status: RelayStatus = RelayStatus.DISCONNECTED,
    val lastError: String? = null
)
