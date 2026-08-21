package com.nostrange.app

import com.nostrange.app.domain.matching.CandidateRanker
import com.nostrange.app.domain.matching.CompatibilityEngine
import com.nostrange.app.domain.matching.HardFilterEngine
import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.domain.model.Lifestyle
import com.nostrange.app.domain.model.Personality
import com.nostrange.app.domain.model.Preferences
import com.nostrange.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityAndFilterTests {

    private val userProfile = UserProfile(
        pubkey = "user_pubkey_1111",
        country = "IR",
        region = "Tehran",
        age = 30,
        gender = "male",
        target_genders = listOf("female"),
        relationship_goal = "marriage",
        wants_marriage = true,
        wants_children = true,
        personality = Personality(independence = 80, sociability = 50, openness = 70),
        lifestyle = Lifestyle(activity_level = 60, travel = 50),
        values = listOf("honesty", "family", "growth"),
        interests = listOf("books", "tech"),
        preferences = Preferences(min_age = 22, max_age = 35, allow_different_country = false),
        deal_breakers = listOf("smoking")
    )

    @Test
    fun testHardFilterEligibleCandidate() {
        val compatibleCandidate = CandidateProfile(
            pubkey = "candidate_pubkey_2222",
            country = "IR",
            region = "Tehran",
            age = 28,
            gender = "female",
            target_genders = listOf("male"),
            relationship_goal = "marriage",
            wants_marriage = true,
            wants_children = true,
            values = listOf("family", "growth"),
            interests = listOf("books")
        )

        assertTrue(HardFilterEngine.isEligible(userProfile, compatibleCandidate))
    }

    @Test
    fun testHardFilterIneligibleGender() {
        val maleCandidate = CandidateProfile(
            pubkey = "candidate_pubkey_3333",
            country = "IR",
            region = "Tehran",
            age = 29,
            gender = "male", // Incompatible with user's target_genders ("female")
            target_genders = listOf("female"),
            relationship_goal = "marriage",
            wants_marriage = true,
            wants_children = true
        )

        assertFalse(HardFilterEngine.isEligible(userProfile, maleCandidate))
    }

    @Test
    fun testHardFilterIneligibleAge() {
        val candidateTooOld = CandidateProfile(
            pubkey = "candidate_pubkey_4444",
            country = "IR",
            region = "Tehran",
            age = 45, // Out of range (22-35)
            gender = "female",
            target_genders = listOf("male"),
            relationship_goal = "marriage",
            wants_marriage = true,
            wants_children = true
        )

        assertFalse(HardFilterEngine.isEligible(userProfile, candidateTooOld))
    }

    @Test
    fun testHardFilterDealBreakerTriggered() {
        val smokerCandidate = CandidateProfile(
            pubkey = "candidate_pubkey_5555",
            country = "IR",
            region = "Tehran",
            age = 27,
            gender = "female",
            target_genders = listOf("male"),
            relationship_goal = "marriage",
            wants_marriage = true,
            wants_children = true,
            interests = listOf("smoking") // Triggers user deal-breaker!
        )

        assertFalse(HardFilterEngine.isEligible(userProfile, smokerCandidate))
    }

    @Test
    fun testMutualCompatibilityScoring() {
        val candidate = CandidateProfile(
            pubkey = "candidate_pubkey_6666",
            country = "IR",
            region = "Tehran",
            age = 29,
            gender = "female",
            target_genders = listOf("male"),
            relationship_goal = "marriage",
            wants_marriage = true,
            wants_children = true,
            personality = Personality(independence = 75, sociability = 55, openness = 75),
            lifestyle = Lifestyle(activity_level = 65, travel = 55),
            values = listOf("honesty", "family", "growth"),
            interests = listOf("books", "tech")
        )

        val score = CompatibilityEngine.calculateMutualScore(userProfile, candidate)
        assertTrue("Score should be high for very compatible profiles", score > 80.0)
    }

    @Test
    fun testCandidateRankingPipeline() {
        val pool = listOf(
            CandidateProfile(
                pubkey = "cand_1", country = "IR", region = "Tehran", age = 28, gender = "female",
                target_genders = listOf("male"), relationship_goal = "marriage", wants_marriage = true, wants_children = true,
                values = listOf("honesty", "family"), interests = listOf("books")
            ),
            CandidateProfile(
                pubkey = "cand_2_ineligible", country = "US", region = "CA", age = 28, gender = "female",
                target_genders = listOf("male"), relationship_goal = "marriage", wants_marriage = true, wants_children = true
            ),
            CandidateProfile(
                pubkey = "cand_3_smoker", country = "IR", region = "Tehran", age = 28, gender = "female",
                target_genders = listOf("male"), relationship_goal = "marriage", wants_marriage = true, wants_children = true,
                interests = listOf("smoking")
            )
        )

        val topCandidates = CandidateRanker.generateTopCandidates(userProfile, pool, 10)
        assertEquals(1, topCandidates.size)
        assertEquals("cand_1", topCandidates[0].pubkey)
        assertTrue(topCandidates[0].initial_score > 0.0)
    }
}
