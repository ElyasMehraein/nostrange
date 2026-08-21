package com.nostrange.app

import com.nostrange.app.ai.schema.PrivacyEnforcer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyEnforcerTest {

    @Test
    fun testForbiddenFieldsDetectionAndRemoval() {
        val dangerousJson = """
        {
            "name": "Ali Ahmadi",
            "phone": "+989123456789",
            "email": "ali@example.com",
            "address": "Tehran, Jordan St",
            "telegram": "@ali_nostr",
            "photo": "https://example.com/avatar.jpg",
            "age": 32,
            "country": "IR",
            "region": "Tehran",
            "gender": "male"
        }
        """.trimIndent()

        assertTrue(PrivacyEnforcer.containsForbiddenFields(dangerousJson))

        val parsed = Json.parseToJsonElement(dangerousJson)
        val sanitized = PrivacyEnforcer.sanitizeJson(parsed).jsonObject

        assertFalse(sanitized.containsKey("name"))
        assertFalse(sanitized.containsKey("phone"))
        assertFalse(sanitized.containsKey("email"))
        assertFalse(sanitized.containsKey("address"))
        assertFalse(sanitized.containsKey("telegram"))
        assertFalse(sanitized.containsKey("photo"))

        assertTrue(sanitized.containsKey("age"))
        assertTrue(sanitized.containsKey("country"))
        assertTrue(sanitized.containsKey("region"))
        assertTrue(sanitized.containsKey("gender"))
    }

    @Test
    fun testTextSanitizationRedactsPii() {
        val rawText = "من با ایمیل user@test.com و شماره 09121234567 و اکانت @myhandle در تلگرام هستم."
        val sanitized = PrivacyEnforcer.sanitizeText(rawText)

        assertFalse(sanitized.contains("user@test.com"))
        assertFalse(sanitized.contains("09121234567"))
        assertFalse(sanitized.contains("@myhandle"))
        assertTrue(sanitized.contains("[REDACTED_EMAIL]"))
        assertTrue(sanitized.contains("[REDACTED_PHONE]"))
        assertTrue(sanitized.contains("[REDACTED_HANDLE]"))
    }
}
