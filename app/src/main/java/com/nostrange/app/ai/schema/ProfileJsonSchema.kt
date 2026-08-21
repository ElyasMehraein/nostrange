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
            val parsedElement = json.parseToJsonElement(rawJson)
            val sanitized = PrivacyEnforcer.sanitizeJson(parsedElement)
            val rootObj = sanitized.jsonObject

            val schemaVersion = rootObj["schema_version"]?.jsonPrimitive?.intOrNull ?: 1
            val country = rootObj["country"]?.jsonPrimitive?.content?.trim()
                ?: throw IllegalArgumentException("Missing required field: country")
            val region = rootObj["region"]?.jsonPrimitive?.content?.trim()
                ?: throw IllegalArgumentException("Missing required field: region")
            val age = rootObj["age"]?.jsonPrimitive?.intOrNull
                ?: throw IllegalArgumentException("Missing required field: age")

            if (age < 18 || age > 120) {
                throw IllegalArgumentException("Age must be between 18 and 120 (got $age)")
            }

            val gender = rootObj["gender"]?.jsonPrimitive?.content?.lowercase()?.trim()
                ?: throw IllegalArgumentException("Missing required field: gender")

            val targetGenders = rootObj["target_genders"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.content.lowercase().trim().takeIf { s -> s.isNotBlank() }
            } ?: emptyList()

            if (targetGenders.isEmpty()) {
                throw IllegalArgumentException("target_genders must not be empty")
            }

            val relationshipGoal = rootObj["relationship_goal"]?.jsonPrimitive?.content?.lowercase()?.trim()
                ?: "long_term"

            val wantsMarriage = rootObj["wants_marriage"]?.jsonPrimitive?.booleanOrNull ?: true
            val wantsChildren = rootObj["wants_children"]?.jsonPrimitive?.booleanOrNull ?: true

            val personalityObj = rootObj["personality"]?.jsonObject
            val personality = Personality(
                independence = clamp(personalityObj?.get("independence")?.jsonPrimitive?.intOrNull ?: 50),
                sociability = clamp(personalityObj?.get("sociability")?.jsonPrimitive?.intOrNull ?: 50),
                openness = clamp(personalityObj?.get("openness")?.jsonPrimitive?.intOrNull ?: 50),
                emotional_stability = clamp(personalityObj?.get("emotional_stability")?.jsonPrimitive?.intOrNull ?: 50),
                agreeableness = clamp(personalityObj?.get("agreeableness")?.jsonPrimitive?.intOrNull ?: 50),
                conscientiousness = clamp(personalityObj?.get("conscientiousness")?.jsonPrimitive?.intOrNull ?: 50)
            )

            val lifestyleObj = rootObj["lifestyle"]?.jsonObject
            val lifestyle = Lifestyle(
                activity_level = clamp(lifestyleObj?.get("activity_level")?.jsonPrimitive?.intOrNull ?: 50),
                travel = clamp(lifestyleObj?.get("travel")?.jsonPrimitive?.intOrNull ?: 50),
                social_life = clamp(lifestyleObj?.get("social_life")?.jsonPrimitive?.intOrNull ?: 50),
                intellectual_curiosity = clamp(lifestyleObj?.get("intellectual_curiosity")?.jsonPrimitive?.intOrNull ?: 50),
                economic_style = clamp(lifestyleObj?.get("economic_style")?.jsonPrimitive?.intOrNull ?: 50)
            )

            val interests = rootObj["interests"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.content.trim().takeIf { s -> s.isNotBlank() }
            } ?: emptyList()

            val values = rootObj["values"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.content.trim().takeIf { s -> s.isNotBlank() }
            } ?: emptyList()

            val dealBreakers = rootObj["deal_breakers"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.content.trim().takeIf { s -> s.isNotBlank() }
            } ?: emptyList()

            val prefObj = rootObj["preferences"]?.jsonObject
            val preferences = Preferences(
                min_age = prefObj?.get("min_age")?.jsonPrimitive?.intOrNull ?: 18,
                max_age = prefObj?.get("max_age")?.jsonPrimitive?.intOrNull ?: 70,
                max_distance_km = prefObj?.get("max_distance_km")?.jsonPrimitive?.intOrNull ?: 500,
                allow_different_country = prefObj?.get("allow_different_country")?.jsonPrimitive?.booleanOrNull ?: false
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
