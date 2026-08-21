package com.nostrange.app.ai.prompt

/**
 * Generates structured, privacy-guaranteed prompts for external AI / Agents (ChatGPT, Gemini, Grok, etc.)
 * to transform raw conversational onboarding answers into validated structured Profile JSON.
 */
object ProfilePromptGenerator {

    fun generateProfileCreationPrompt(
        userAnswers: Map<String, String>,
        userPubkey: String
    ): String {
        val answersFormatted = userAnswers.entries.joinToString("\n") { (question, answer) ->
            "- $question: $answer"
        }

        return """
شما یک سیستم هوشمند استخراج ساختار پروفایل سازگاری برای Nostrange هستید.

وظیفه شما:
پاسخ‌های کاربر به سؤالات مصاحبه آشنایی را تجزیه و تحلیل کرده و آن‌ها را دقیقاً به فرمت JSON ساختاریافته زیر تبدیل کنید.

═══════════════════════════════════════════════════════════════
قوانین و دستورات بسیار سخت‌گیرانه حریم خصوصی (Privacy Rules):
═══════════════════════════════════════════════════════════════
۱. اطلاعات موجود در متن کاربر را فقط و فقط برای استخراج ویژگی‌های لازم برای Matching بررسی کن.
۲. فیلدهای country (نام یا کد کشور مثلاً IR) و region (نام شهر یا استان مثلاً Tehran یا Isfahan) الزامی هستند و باید از متن استخراج شوند.
۳. اگر کاربر نام واقعی، نام خانوادگی، شماره تلفن، ایمیل، آدرس دقیق خیابان، شناسه شبکه اجتماعی (Telegram، Instagram و غیره)، لینک وب‌سایت، اطلاعات تماس، اطلاعات هویتی مستقیم یا هر داده‌ای که بتواند به شناسایی مستقیم او منجر شود وارد کرده است، این اطلاعات را از خروجی JSON کاملاً حذف کن.
۴. این اطلاعات هرگز نباید در خروجی JSON قرار گیرند.
۵. هیچ فیلدی مربوط به عکس، تصویر، آواتار، نام، شماره، ایمیل یا آدرس پستی در JSON ایجاد نکن.
۶. هر اطلاعاتی که برای matching لازم نیست نیز نباید وارد JSON شود.
۷. فقط داده‌هایی را استخراج کن که در schema تعریف شده‌اند.
۸. هرگز چیزی را که کاربر نگفته است حدس نزن یا جعل نکن.
۹. خروجی فقط و فقط یک شیء JSON معتبر (بدون توضیحات اضافی، بدون Markdown اضافه‌تر از تگ json) باشد.

═══════════════════════════════════════════════════════════════
راهنمای مقادیر عددی (0 تا 100):
0 = بسیار کم / درونگرا / کم‌تحرک / حداقل
50 = متوسط / متعادل
100 = بسیار زیاد / برونگرا / بسیار پرتحرک / حداکثر
═══════════════════════════════════════════════════════════════

Schema خروجی مورد انتظار:
```json
{
  "schema_version": 1,
  "pubkey": "$userPubkey",
  "country": "IR",
  "region": "Tehran",
  "age": 30,
  "gender": "male",
  "target_genders": ["female"],
  "relationship_goal": "long_term",
  "wants_marriage": true,
  "wants_children": true,
  "personality": {
    "independence": 80,
    "sociability": 45,
    "openness": 75,
    "emotional_stability": 80,
    "agreeableness": 70,
    "conscientiousness": 85
  },
  "lifestyle": {
    "activity_level": 50,
    "travel": 60,
    "social_life": 40,
    "intellectual_curiosity": 85,
    "economic_style": 60
  },
  "interests": [
    "technology",
    "reading",
    "hiking"
  ],
  "values": [
    "honesty",
    "growth",
    "family"
  ],
  "preferences": {
    "min_age": 24,
    "max_age": 36,
    "max_distance_km": 500,
    "allow_different_country": false
  },
  "deal_breakers": [
    "smoking",
    "dishonesty"
  ]
}
```

پاسخ‌های کاربر به سؤالات:
$answersFormatted

لطفاً خروجی JSON نهایی را تولید کنید:
""".trimIndent()
    }
}
