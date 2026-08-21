package com.nostrange.app.domain.matching

import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.domain.model.UserProfile
import kotlin.math.abs

/**
 * Hard Filtering Engine.
 * Evaluates non-negotiable baseline compatibility requirements.
 * Filters 10,000 candidates down to ~3,000 candidates.
 */
object HardFilterEngine {

    fun isEligible(user: UserProfile, candidate: CandidateProfile): Boolean {
        // Do not match user with oneself
        if (user.pubkey.equals(candidate.pubkey, ignoreCase = true)) {
            return false
        }

        // 1. Gender Compatibility (Mutual)
        val userAcceptsCandidateGender = user.target_genders.any { it.equals(candidate.gender, ignoreCase = true) }
        val candidateAcceptsUserGender = candidate.target_genders.isEmpty() ||
                candidate.target_genders.any { it.equals(user.gender, ignoreCase = true) }

        if (!userAcceptsCandidateGender || !candidateAcceptsUserGender) {
            return false
        }

        // 2. Age Constraints
        if (candidate.age < user.preferences.min_age || candidate.age > user.preferences.max_age) {
            return false
        }

        // 3. Country / Location constraints
        if (!user.preferences.allow_different_country) {
            if (!user.country.equals(candidate.country, ignoreCase = true)) {
                return false
            }
        }

        // 4. Fundamental Marriage / Children deal-breakers (if explicitly specified as non-negotiable)
        if (user.relationship_goal.equals("marriage", ignoreCase = true) && !candidate.wants_marriage) {
            return false
        }
        if (candidate.relationship_goal.equals("marriage", ignoreCase = true) && !user.wants_marriage) {
            return false
        }

        // 5. Deal Breakers check
        if (user.deal_breakers.isNotEmpty()) {
            for (db in user.deal_breakers) {
                val dbNorm = db.trim().lowercase()
                if (candidate.interests.any { it.trim().lowercase() == dbNorm } ||
                    candidate.values.any { it.trim().lowercase() == dbNorm }) {
                    return false
                }
            }
        }

        if (candidate.deal_breakers.isNotEmpty()) {
            for (db in candidate.deal_breakers) {
                val dbNorm = db.trim().lowercase()
                if (user.interests.any { it.trim().lowercase() == dbNorm } ||
                    user.values.any { it.trim().lowercase() == dbNorm }) {
                    return false
                }
            }
        }

        return true
    }

    fun filterCandidates(user: UserProfile, candidates: List<CandidateProfile>): List<CandidateProfile> {
        return candidates.filter { isEligible(user, it) }
    }
}
