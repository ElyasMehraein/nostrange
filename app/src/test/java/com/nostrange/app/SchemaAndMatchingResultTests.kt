package com.nostrange.app

import com.nostrange.app.ai.schema.MatchingResultSchema
import com.nostrange.app.ai.schema.ProfileJsonSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileJsonSchemaTest {

    @Test
    fun testValidProfileParsing() {
        val validJson = """
        {
          "schema_version": 1,
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
          "interests": ["tech", "books"],
          "values": ["honesty", "family"]
        }
        """.trimIndent()

        val result = ProfileJsonSchema.parseAndValidateProfileJson(validJson, "test_pubkey_123")
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals("IR", profile?.country)
        assertEquals("Tehran", profile?.region)
        assertEquals(30, profile?.age)
        assertEquals("male", profile?.gender)
        assertEquals(80, profile?.personality?.independence)
        assertEquals(50, profile?.lifestyle?.activity_level)
    }

    @Test
    fun testInvalidAgeRejection() {
        val invalidAgeJson = """
        {
          "country": "IR",
          "region": "Tehran",
          "age": 15,
          "gender": "male",
          "target_genders": ["female"]
        }
        """.trimIndent()

        val result = ProfileJsonSchema.parseAndValidateProfileJson(invalidAgeJson, "test_pubkey")
        assertTrue(result.isFailure)
    }
}

class MatchingResultValidatorTest {

    private val validCandidatePubkeys = setOf(
        "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90",
        "11223344556677889900aabbccddeeff11223344556677889900aabbccddeeff"
    )

    @Test
    fun testValidAiMatchingResultParsing() {
        val json = """
        {
          "schema_version": 1,
          "matches": [
            {
              "rank": 1,
              "pubkey": "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90",
              "compatibility_score": 93.5,
              "reasons": ["ارزش‌های مشترک", "اهداف ازدواج"]
            },
            {
              "rank": 2,
              "pubkey": "11223344556677889900aabbccddeeff11223344556677889900aabbccddeeff",
              "compatibility_score": 88.0,
              "reasons": ["سبک زندگی سازگار"]
            }
          ]
        }
        """.trimIndent()

        val result = MatchingResultSchema.parseAndValidateMatchingResult(json, validCandidatePubkeys)
        assertTrue(result.isSuccess)
        val aiResult = result.getOrNull()
        assertNotNull(aiResult)
        assertEquals(2, aiResult?.matches?.size)
        assertEquals(1, aiResult?.matches?.get(0)?.rank)
        assertEquals(93.5, aiResult?.matches?.get(0)?.compatibilityScore ?: 0.0, 0.01)
    }

    @Test
    fun testRejectionOfHallucinatedPubkeys() {
        val hallucinatedJson = """
        {
          "schema_version": 1,
          "matches": [
            {
              "rank": 1,
              "pubkey": "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
              "compatibility_score": 99.0,
              "reasons": ["نامعتبر"]
            }
          ]
        }
        """.trimIndent()

        val result = MatchingResultSchema.parseAndValidateMatchingResult(hallucinatedJson, validCandidatePubkeys)
        assertTrue(result.isFailure) // All items were filtered out because pubkey was not in local candidate pool
    }
}
