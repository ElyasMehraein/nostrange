package com.nostrange.app.domain.matching

import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.domain.model.UserProfile

/**
 * Orchestrates the full on-device matching pipeline:
 * Profiles -> Hard Filters -> Initial Scoring -> Top 100
 */
object CandidateRanker {

    fun generateTopCandidates(
        user: UserProfile,
        rawPool: List<CandidateProfile>,
        targetLimit: Int = 100
    ): List<CandidateProfile> {
        // Step 1: Hard Filter (Drops non-compliant profiles)
        val filtered = HardFilterEngine.filterCandidates(user, rawPool)

        // Step 2: Calculate Initial Compatibility Scores locally
        val scored = filtered.map { candidate ->
            val score = CompatibilityEngine.calculateMutualScore(user, candidate)
            candidate.copy(initial_score = score)
        }

        // Step 3: Sort descending and take top N (e.g. 100)
        return scored.sortedByDescending { it.initial_score }.take(targetLimit)
    }
}
