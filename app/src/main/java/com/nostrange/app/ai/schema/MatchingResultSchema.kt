package com.nostrange.app.ai.schema

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class AiMatchItem(
    val rank: Int,
    val pubkey: String,
    val compatibilityScore: Double,
    val reasons: List<String>
)

@Serializable
data class AiMatchingResult(
    val schemaVersion: Int = 1,
    val matches: List<AiMatchItem>
)

/**
 * Validates and parses external AI / Agent ranking output.
 * Ensures pubkeys exist in local candidate pool, no hallucinations, and valid score ranges.
 */
object MatchingResultSchema {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseAndValidateMatchingResult(
        rawJson: String,
        validCandidatePubkeys: Set<String>
    ): Result<AiMatchingResult> {
        return runCatching {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val schemaVersion = root["schema_version"]?.jsonPrimitive?.intOrNull ?: 1

            val matchesArray = root["matches"]?.jsonArray
                ?: throw IllegalArgumentException("Missing 'matches' array in AI response")

            val parsedMatches = mutableListOf<AiMatchItem>()
            val seenPubkeys = mutableSetOf<String>()

            for (matchElem in matchesArray) {
                val matchObj = matchElem.jsonObject
                val rank = matchObj["rank"]?.jsonPrimitive?.intOrNull ?: (parsedMatches.size + 1)
                val pubkey = matchObj["pubkey"]?.jsonPrimitive?.content?.trim()?.lowercase()
                    ?: throw IllegalArgumentException("Missing pubkey in match item")

                // Ensure pubkey is a 64-character hex string
                if (pubkey.length != 64 || !pubkey.all { it in "0123456789abcdef" }) {
                    continue // Skip invalid pubkeys
                }

                // AI is forbidden from hallucinating or inventing new pubkeys
                if (!validCandidatePubkeys.contains(pubkey)) {
                    continue // Skip pubkeys not in the provided prompt candidate pool
                }

                // Prevent duplicates
                if (seenPubkeys.contains(pubkey)) {
                    continue
                }
                seenPubkeys.add(pubkey)

                val score = (matchObj["compatibility_score"]?.jsonPrimitive?.doubleOrNull ?: 50.0).coerceIn(0.0, 100.0)

                val reasons = matchObj["reasons"]?.jsonArray?.mapNotNull {
                    val text = it.jsonPrimitive.content.trim()
                    if (text.isNotBlank()) PrivacyEnforcer.sanitizeText(text) else null
                } ?: emptyList()

                parsedMatches.add(
                    AiMatchItem(
                        rank = rank,
                        pubkey = pubkey,
                        compatibilityScore = score,
                        reasons = reasons
                    )
                )
            }

            if (parsedMatches.isEmpty()) {
                throw IllegalArgumentException("No valid candidate matches found in AI response")
            }

            // Sort by rank
            val sortedMatches = parsedMatches.sortedBy { it.rank }

            AiMatchingResult(
                schemaVersion = schemaVersion,
                matches = sortedMatches
            )
        }
    }
}
