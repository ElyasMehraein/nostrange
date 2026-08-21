package com.nostrange.app.ai.schema

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Resilient utilities for cleaning and parsing JSON returned by external AI / LLMs.
 * Handles mixed Persian/English digits, markdown code block fences, missing/empty fields,
 * and Persian keywords.
 */
object JsonSanitizerUtils {

    /**
     * Extracts pure JSON string from text that might contain markdown fences (```json ... ```)
     * or surrounding conversational text.
     */
    fun extractJson(input: String): String {
        var text = input.trim()

        // Strip markdown code fences if present
        if (text.startsWith("```")) {
            val firstLineEnd = text.indexOf('\n')
            if (firstLineEnd != -1) {
                text = text.substring(firstLineEnd + 1)
            }
            val lastFence = text.lastIndexOf("```")
            if (lastFence != -1) {
                text = text.substring(0, lastFence)
            }
            text = text.trim()
        }

        // If surrounded by other text, locate the outermost { ... } or [ ... ]
        val firstBrace = text.indexOf('{')
        val firstBracket = text.indexOf('[')

        val startIndex = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            firstBracket != -1 -> firstBracket
            else -> 0
        }

        val lastBrace = text.lastIndexOf('}')
        val lastBracket = text.lastIndexOf(']')

        val endIndex = maxOf(lastBrace, lastBracket)

        val jsonSubstring = if (startIndex >= 0 && endIndex > startIndex) {
            text.substring(startIndex, endIndex + 1)
        } else {
            text
        }

        return normalizePersianDigits(jsonSubstring)
    }

    /**
     * Converts Persian and Arabic digits (۰-۹ and ٠-٩) to standard ASCII digits (0-9).
     */
    fun normalizePersianDigits(input: String): String {
        val persian = "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩"
        val english = "01234567890123456789"
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = persian.indexOf(ch)
            if (idx != -1) {
                sb.append(english[idx])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Normalizes gender string from Persian / English to canonical "male" or "female".
     */
    fun normalizeGender(gender: String?): String {
        if (gender.isNullOrBlank()) return "male"
        val g = gender.lowercase().trim()
        return when {
            g in listOf("زن", "دختر", "خانم", "بانو", "مونث", "female", "woman", "girl", "f", "w") -> "female"
            g in listOf("مرد", "پسر", "آقا", "مذکر", "male", "man", "boy", "m") -> "male"
            g.contains("زن") || g.contains("خانم") || g.contains("female") -> "female"
            g.contains("مرد") || g.contains("آقا") || g.contains("male") -> "male"
            else -> "male"
        }
    }

    /**
     * Normalizes target_genders list from Persian / English to canonical ["male"], ["female"], or ["male", "female"].
     */
    fun normalizeTargetGenders(genders: List<String>?, userGender: String): List<String> {
        val normalized = mutableSetOf<String>()
        if (genders != null) {
            for (raw in genders) {
                val g = raw.lowercase().trim()
                when {
                    g in listOf("هر دو", "هردو", "همه", "both", "all", "any") -> {
                        normalized.add("male")
                        normalized.add("female")
                    }
                    g in listOf("زن", "دختر", "خانم", "بانو", "مونث", "female", "woman", "girl", "f", "w") ||
                            g.contains("زن") || g.contains("خانم") || g.contains("female") -> {
                        normalized.add("female")
                    }
                    g in listOf("مرد", "پسر", "آقا", "مذکر", "male", "man", "boy", "m") ||
                            g.contains("مرد") || g.contains("آقا") || g.contains("male") -> {
                        normalized.add("male")
                    }
                }
            }
        }

        if (normalized.isEmpty()) {
            // Default target gender is opposite of user's gender
            val userNorm = normalizeGender(userGender)
            return if (userNorm == "male") listOf("female") else listOf("male")
        }
        return normalized.toList()
    }

    /**
     * Normalizes relationship goal to canonical values.
     */
    fun normalizeRelationshipGoal(goal: String?): String {
        if (goal.isNullOrBlank()) return "marriage"
        val g = goal.lowercase().trim()
        return when {
            g.contains("ازدواج") || g.contains("همسر") || g.contains("marriage") -> "marriage"
            g.contains("بلند") || g.contains("long") || g.contains("پایدار") -> "long_term"
            g.contains("دوستی") || g.contains("آشنایی") || g.contains("friend") || g.contains("short") -> "friendship"
            else -> "long_term"
        }
    }

    /**
     * Robust integer parser from JsonPrimitive (handles Int, Double, String with Persian/English digits).
     */
    fun parseInt(primitive: JsonPrimitive?, defaultVal: Int = 0): Int {
        if (primitive == null) return defaultVal
        primitive.intOrNull?.let { return it }
        val str = normalizePersianDigits(primitive.content.trim())
        return str.toIntOrNull() ?: str.toDoubleOrNull()?.toInt() ?: defaultVal
    }

    /**
     * Robust double parser from JsonPrimitive.
     */
    fun parseDouble(primitive: JsonPrimitive?, defaultVal: Double = 0.0): Double {
        if (primitive == null) return defaultVal
        primitive.doubleOrNull?.let { return it }
        val str = normalizePersianDigits(primitive.content.trim())
        return str.toDoubleOrNull() ?: defaultVal
    }

    /**
     * Normalizes country name/code to canonical uppercase code (e.g., "IR") or clean string.
     */
    fun normalizeCountry(country: String?): String {
        if (country.isNullOrBlank()) return "IR"
        val c = country.lowercase().trim()
        return when {
            c in listOf("ir", "iran", "ایران", "جمهوری اسلامی ایران", "persia") -> "IR"
            c in listOf("us", "usa", "america", "united states", "آمریکا") -> "US"
            c in listOf("ca", "canada", "کانادا") -> "CA"
            c in listOf("de", "germany", "deutschland", "آلمان") -> "DE"
            c in listOf("uk", "gb", "great britain", "united kingdom", "انگلیس", "بریتانیا") -> "GB"
            c in listOf("tr", "turkey", "ترکیه") -> "TR"
            c in listOf("ae", "uae", "امارات", "دبی") -> "AE"
            else -> country.trim().uppercase()
        }
    }

    /**
     * Checks if two country strings match or are compatible.
     */
    fun isCountryCompatible(country1: String?, country2: String?, allowDifferent: Boolean): Boolean {
        if (allowDifferent) return true
        val c1 = normalizeCountry(country1)
        val c2 = normalizeCountry(country2)
        if (c1 == "نامشخص" || c2 == "نامشخص" || c1.isBlank() || c2.isBlank()) return true
        return c1.equals(c2, ignoreCase = true)
    }

    /**
     * Robust boolean parser from JsonPrimitive.
     */
    fun parseBoolean(primitive: JsonPrimitive?, defaultVal: Boolean = true): Boolean {
        if (primitive == null) return defaultVal
        primitive.booleanOrNull?.let { return it }
        val str = primitive.content.trim().lowercase()
        return when (str) {
            "true", "1", "yes", "بله", "آره", "حتما", "حتماً" -> true
            "false", "0", "no", "خیر", "نه", "اصلا", "اصلاً" -> false
            else -> defaultVal
        }
    }
}
