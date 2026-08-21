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
        deal_breakers = listOf("smoking"),
        last_active_at = System.currentTimeMillis() / 1000
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
            interests = listOf("books"),
            last_active_at = System.currentTimeMillis() / 1000
        )

        assertTrue(HardFilterEngine.isEligible(userProfile, compatibleCandidate))
    }

    @Test
    fun testHardFilterIgnoreUsersInactiveMoreThan10Days() {
        val oldInactiveCandidate = CandidateProfile(
            pubkey = "candidate_pubkey_inactive",
            country = "IR",
            region = "Tehran",
            age = 28,
            gender = "female",
            target_genders = listOf("male"),
            relationship_goal = "marriage",
            wants_marriage = true,
            wants_children = true,
            values = listOf("family", "growth"),
            interests = listOf("books"),
            last_active_at = (System.currentTimeMillis() / 1000) - (15 * 24 * 60 * 60) // 15 days ago
        )

        assertFalse("Candidates inactive for more than 10 days must be filtered out", HardFilterEngine.isEligible(userProfile, oldInactiveCandidate))
    }

    @Test
    fun testHardFilterIneligibleGender() {
        val maleCandidate = CandidateProfile(
            pubkey = "candidate_pubkey_3333",
            country = "IR",
            region = "Tehran",
            age = 29,
            gender = "male",
            target_genders = listOf("female"),
            relationship_goal = "marriage",
            wants_marriage = true,
            wants_children = true,
            last_active_at = System.currentTimeMillis() / 1000
        )

        assertFalse(HardFilterEngine.isEligible(userProfile, maleCandidate))
    }

    @Test
    fun testHardFilterIneligibleAge() {
        val candidateTooOld = CandidateProfile(
            pubkey = "candidate_pubkey_4444",
            country = "IR",
            region = "Tehran",
            age = 45,
            gender = "female",
            target_genders = listOf("male"),
            relationship_goal = "marriage",
            wants_marriage = true,
            wants_children = true,
            last_active_at = System.currentTimeMillis() / 1000
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
            interests = listOf("smoking"),
            last_active_at = System.currentTimeMillis() / 1000
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
            interests = listOf("books", "tech"),
            last_active_at = System.currentTimeMillis() / 1000
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
                values = listOf("honesty", "family"), interests = listOf("books"),
                last_active_at = System.currentTimeMillis() / 1000
            ),
            CandidateProfile(
                pubkey = "cand_2_ineligible", country = "US", region = "CA", age = 28, gender = "female",
                target_genders = listOf("male"), relationship_goal = "marriage", wants_marriage = true, wants_children = true,
                last_active_at = System.currentTimeMillis() / 1000
            ),
            CandidateProfile(
                pubkey = "cand_3_smoker", country = "IR", region = "Tehran", age = 28, gender = "female",
                target_genders = listOf("male"), relationship_goal = "marriage", wants_marriage = true, wants_children = true,
                interests = listOf("smoking"),
                last_active_at = System.currentTimeMillis() / 1000
            )
        )

        val topCandidates = CandidateRanker.generateTopCandidates(userProfile, pool, 10)
        assertEquals(1, topCandidates.size)
        assertEquals("cand_1", topCandidates[0].pubkey)
        assertTrue(topCandidates[0].initial_score > 0.0)
    }
}
