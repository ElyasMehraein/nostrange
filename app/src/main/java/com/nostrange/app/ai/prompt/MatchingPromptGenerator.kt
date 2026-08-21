package com.nostrange.app.ai.prompt

import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.domain.model.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Generates an AI prompt for ranking top filtered candidates.
 */
object MatchingPromptGenerator {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    fun generateRankingPrompt(
        userProfile: UserProfile,
        candidates: List<CandidateProfile>,
        topCount: Int = 20
    ): String {
        val userJson = json.encodeToString(userProfile)

        val candidatesCompactList = candidates.map { c ->
            buildJsonObject {
                put("pubkey", c.pubkey)
                put("age", c.age)
                put("country", c.country)
                put("region", c.region)
                put("gender", c.gender)
                put("relationship_goal", c.relationship_goal)
                put("wants_marriage", c.wants_marriage)
                put("wants_children", c.wants_children)
                putJsonObject("personality") {
                    put("indep", c.personality.independence)
                    put("soc", c.personality.sociability)
                    put("open", c.personality.openness)
                    put("emot", c.personality.emotional_stability)
                    put("agree", c.personality.agreeableness)
                    put("consc", c.personality.conscientiousness)
                }
                putJsonObject("lifestyle") {
                    put("act", c.lifestyle.activity_level)
                    put("trv", c.lifestyle.travel)
                    put("soc_l", c.lifestyle.social_life)
                    put("cur", c.lifestyle.intellectual_curiosity)
                    put("econ", c.lifestyle.economic_style)
                }
                putJsonArray("interests") {
                    c.interests.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                }
                putJsonArray("values") {
                    c.values.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                }
                putJsonArray("deal_breakers") {
                    c.deal_breakers.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                }
                put("initial_score", c.initial_score)
            }
        }

        val candidatesJson = json.encodeToString(candidatesCompactList)

        return """
شما یک هوش مصنوعی تحلیل سازگاری و Matchmaking پیشرفته برای Nostrange هستید.

وظیفه شما:
پروفایل کاربر اصلی را با لیست ${candidates.size} کاندیدای موجود مقایسه کرده و بهترین $topCount مورد سازگار را به ترتیب اولویت و رتبه‌بندی انتخاب کنید.

═══════════════════════════════════════════════════════════════
قوانین اکید:
═══════════════════════════════════════════════════════════════
۱. فقط و فقط از pubkeyهای موجود در لیست کاندیداها استفاده کن. به هیچ وجه pubkey جدید تولید یا جعل نکن.
۲. هیچ اطلاعات جدیدی که در داده‌ها نیست اختراع نکن.
۳. compatibility_score باید عددی بین 0 تا 100 باشد.
۴. برای هر کاندیدا، بین ۲ تا ۳ دلیل کلیدی و دقیق به زبان فارسی برای سازگاری (reasons) بنویس.
۵. خروجی باید کاملاً معتبر و مطابق فرمت JSON زیر باشد (بدون هرگونه متن اضافی):

```json
{
  "schema_version": 1,
  "matches": [
    {
      "rank": 1,
      "pubkey": "64-char hex pubkey",
      "compatibility_score": 94.5,
      "reasons": [
        "اهداف رابطه مشابه و تمایل مشترک به تشکیل خانواده",
        "ارزش‌های بنیادین یکسان در رشد فکری و خانواده",
        "سازگاری سبک زندگی و علایق مشترک در مطالعه و سفر"
      ]
    }
  ]
}
```

═══════════════════════════════════════════════════════════════
پروفایل کاربر اصلی:
$userJson

═══════════════════════════════════════════════════════════════
لیست کاندیداها (${candidates.size} نفر):
$candidatesJson

لطفاً خروجی JSON رتبه‌بندی $topCount مورد برتر را ارسال کنید:
""".trimIndent()
    }
}
