package com.nostrange.app.ai.schema

import com.nostrange.app.domain.model.Lifestyle
import com.nostrange.app.domain.model.Personality
import com.nostrange.app.domain.model.Preferences
import com.nostrange.app.domain.model.UserProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Validates and parses User Profile JSON from external AI or Nostr events.
 * Enforces strict schema, value ranges (0-100), and privacy sanitization.
 */
object ProfileJsonSchema {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseAndValidateProfileJson(rawJson: String, targetPubkey: String): Result<UserProfile> {
        return runCatching {
            val cleanedJson = JsonSanitizerUtils.extractJson(rawJson)
            val parsedElement = json.parseToJsonElement(cleanedJson)
            val sanitized = PrivacyEnforcer.sanitizeJson(parsedElement)
            val rootObj = sanitized.jsonObject

            val schemaVersion = JsonSanitizerUtils.parseInt(rootObj["schema_version"]?.jsonPrimitive, 1)
            val rawCountry = rootObj["country"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() } ?: "IR"
            val country = JsonSanitizerUtils.normalizeCountry(rawCountry)
            val region = rootObj["region"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
                ?: rootObj["city"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
                ?: rootObj["state"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
                ?: "نامشخص"

            val age = JsonSanitizerUtils.parseInt(rootObj["age"]?.jsonPrimitive, 0)
            if (age < 18 || age > 120) {
                throw IllegalArgumentException("سن باید عددی بین ۱۸ تا ۱۲۰ باشد (مقدار دریافتی: $age)")
            }

            val rawGender = rootObj["gender"]?.jsonPrimitive?.content
            val gender = JsonSanitizerUtils.normalizeGender(rawGender)

            val rawTargetGenders = rootObj["target_genders"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.content.trim().takeIf { s -> s.isNotBlank() }
            }
            val targetGenders = JsonSanitizerUtils.normalizeTargetGenders(rawTargetGenders, gender)

            val rawRelationshipGoal = rootObj["relationship_goal"]?.jsonPrimitive?.content
            val relationshipGoal = JsonSanitizerUtils.normalizeRelationshipGoal(rawRelationshipGoal)

            val wantsMarriage = JsonSanitizerUtils.parseBoolean(rootObj["wants_marriage"]?.jsonPrimitive, true)
            val wantsChildren = JsonSanitizerUtils.parseBoolean(rootObj["wants_children"]?.jsonPrimitive, true)

            val personalityObj = rootObj["personality"]?.jsonObject
            val personality = Personality(
                independence = clamp(JsonSanitizerUtils.parseInt(personalityObj?.get("independence")?.jsonPrimitive, 50)),
                sociability = clamp(JsonSanitizerUtils.parseInt(personalityObj?.get("sociability")?.jsonPrimitive, 50)),
                openness = clamp(JsonSanitizerUtils.parseInt(personalityObj?.get("openness")?.jsonPrimitive, 50)),
                emotional_stability = clamp(JsonSanitizerUtils.parseInt(personalityObj?.get("emotional_stability")?.jsonPrimitive, 50)),
                agreeableness = clamp(JsonSanitizerUtils.parseInt(personalityObj?.get("agreeableness")?.jsonPrimitive, 50)),
                conscientiousness = clamp(JsonSanitizerUtils.parseInt(personalityObj?.get("conscientiousness")?.jsonPrimitive, 50))
            )

            val lifestyleObj = rootObj["lifestyle"]?.jsonObject
            val lifestyle = Lifestyle(
                activity_level = clamp(JsonSanitizerUtils.parseInt(lifestyleObj?.get("activity_level")?.jsonPrimitive, 50)),
                travel = clamp(JsonSanitizerUtils.parseInt(lifestyleObj?.get("travel")?.jsonPrimitive, 50)),
                social_life = clamp(JsonSanitizerUtils.parseInt(lifestyleObj?.get("social_life")?.jsonPrimitive, 50)),
                intellectual_curiosity = clamp(JsonSanitizerUtils.parseInt(lifestyleObj?.get("intellectual_curiosity")?.jsonPrimitive, 50)),
                economic_style = clamp(JsonSanitizerUtils.parseInt(lifestyleObj?.get("economic_style")?.jsonPrimitive, 50))
            )

            val rawInterests = rootObj["interests"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.content.trim().takeIf { s -> s.isNotBlank() }
            } ?: emptyList()
            val interests = if (rawInterests.isNotEmpty()) rawInterests else listOf("گفتگو", "رشد شخصی")

            val rawValues = rootObj["values"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.content.trim().takeIf { s -> s.isNotBlank() }
            } ?: emptyList()
            val values = if (rawValues.isNotEmpty()) rawValues else listOf("صداقت", "احترام")

            val dealBreakers = rootObj["deal_breakers"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.content.trim().takeIf { s -> s.isNotBlank() }
            } ?: emptyList()

            val prefObj = rootObj["preferences"]?.jsonObject
            val preferences = Preferences(
                min_age = JsonSanitizerUtils.parseInt(prefObj?.get("min_age")?.jsonPrimitive, 18),
                max_age = JsonSanitizerUtils.parseInt(prefObj?.get("max_age")?.jsonPrimitive, 70),
                max_distance_km = JsonSanitizerUtils.parseInt(prefObj?.get("max_distance_km")?.jsonPrimitive, 500),
                allow_different_country = JsonSanitizerUtils.parseBoolean(prefObj?.get("allow_different_country")?.jsonPrimitive, false)
            )

            UserProfile(
                schema_version = schemaVersion,
                pubkey = targetPubkey,
                country = country,
                region = region,
                age = age,
                gender = gender,
                target_genders = targetGenders,
                relationship_goal = relationshipGoal,
                wants_marriage = wantsMarriage,
                wants_children = wantsChildren,
                personality = personality,
                lifestyle = lifestyle,
                interests = interests,
                values = values,
                preferences = preferences,
                deal_breakers = dealBreakers,
                created_at = System.currentTimeMillis() / 1000
            )
        }
    }

    private fun clamp(value: Int): Int {
        return value.coerceIn(0, 100)
    }
}
