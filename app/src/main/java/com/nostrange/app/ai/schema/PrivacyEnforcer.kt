package com.nostrange.app.ai.schema

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.regex.Pattern

/**
 * STRICT PRIVACY ENFORCEMENT ENGINE
 *
 * Core Principles:
 * 1. Absolute ban on media (no photo, avatar, image, video).
 * 2. Absolute ban on direct personal identifiers (name, phone, email, address, social handles, URLs).
 * 3. Both JSON field blacklist stripping and regex content sanitization.
 */
object PrivacyEnforcer {

    private val FORBIDDEN_KEYS = setOf(
        "name", "firstname", "lastname", "first_name", "last_name", "real_name",
        "phone", "phonenumber", "phone_number", "mobile", "tel",
        "email", "mail", "e-mail",
        "address", "location_address", "street", "postal_code", "zip",
        "telegram", "instagram", "twitter", "x_handle", "social_media", "social",
        "url", "website", "link",
        "photo", "picture", "image", "avatar", "video", "media", "attachment", "gallery",
        "contact", "contact_info", "contact_details"
    )

    private val EMAIL_REGEX = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}")
    private val PHONE_REGEX = Pattern.compile("(\\+?[0-9]{1,3}[-.\\s]?)?\\(?[0-9]{2,4}\\)?[-.\\s]?[0-9]{3,4}[-.\\s]?[0-9]{3,4}")
    private val SOCIAL_HANDLE_REGEX = Pattern.compile("@[a-zA-Z0-9_]{3,30}")
    private val URL_REGEX = Pattern.compile("https?://[\\w-]+(\\.[\\w-]+)+[/#?]?.*")

    /**
     * Sanitizes a JSON element recursively:
     * - Drops any key matching forbidden PII / media names.
     * - Recursively sanitizes nested objects and arrays.
     * - Cleans strings containing potential phone numbers, emails, URLs, or handles.
     */
    fun sanitizeJson(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> {
                buildJsonObject {
                    for ((key, value) in element) {
                        val normalizedKey = key.lowercase().replace("_", "").replace("-", "")
                        val isForbidden = FORBIDDEN_KEYS.any { forbidden ->
                            val normalizedForbidden = forbidden.replace("_", "").replace("-", "")
                            normalizedKey == normalizedForbidden || normalizedKey.contains(normalizedForbidden)
                        }

                        if (!isForbidden) {
                            put(key, sanitizeJson(value))
                        }
                    }
                }
            }
            is JsonArray -> {
                JsonArray(element.map { sanitizeJson(it) })
            }
            is JsonPrimitive -> {
                if (element.isString) {
                    val sanitizedStr = sanitizeText(element.content)
                    JsonPrimitive(sanitizedStr)
                } else {
                    element
                }
            }
        }
    }

    /**
     * Scans and sanitizes raw text by redacting accidental PII (emails, phones, URLs).
     */
    fun sanitizeText(input: String): String {
        var text = EMAIL_REGEX.matcher(input).replaceAll("[REDACTED_EMAIL]")
        text = URL_REGEX.matcher(text).replaceAll("[REDACTED_URL]")
        text = PHONE_REGEX.matcher(text).replaceAll("[REDACTED_PHONE]")
        text = SOCIAL_HANDLE_REGEX.matcher(text).replaceAll("[REDACTED_HANDLE]")
        return text
    }

    /**
     * Inspects raw JSON text and returns true if any forbidden field was detected.
     */
    fun containsForbiddenFields(jsonString: String): Boolean {
        return try {
            val element = Json.parseToJsonElement(jsonString)
            scanForForbidden(element)
        } catch (e: Exception) {
            false
        }
    }

    private fun scanForForbidden(element: JsonElement): Boolean {
        return when (element) {
            is JsonObject -> {
                for ((key, value) in element) {
                    val normalizedKey = key.lowercase().replace("_", "").replace("-", "")
                    if (FORBIDDEN_KEYS.any { normalizedKey == it.replace("_", "") }) {
                        return true
                    }
                    if (scanForForbidden(value)) return true
                }
                false
            }
            is JsonArray -> element.any { scanForForbidden(it) }
            else -> false
        }
    }
}
