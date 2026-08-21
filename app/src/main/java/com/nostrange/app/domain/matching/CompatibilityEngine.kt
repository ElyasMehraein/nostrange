package com.nostrange.app.domain.matching

import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.domain.model.Lifestyle
import com.nostrange.app.domain.model.Personality
import com.nostrange.app.domain.model.UserProfile
import kotlin.math.abs
import kotlin.math.max

data class ScoringWeights(
    val relationshipGoalWeight: Double = 0.20,
    val personalityWeight: Double = 0.25,
    val lifestyleWeight: Double = 0.20,
    val valuesWeight: Double = 0.15,
    val interestsWeight: Double = 0.10,
    val ageProximityWeight: Double = 0.10
)

/**
 * Calculates Initial Compatibility Score locally on-device.
 *
 * NOTE FOR USERS & UI:
 * This score is an initial candidate selection tool, NOT a definitive prediction
 * of relationship success. External AI and human interaction form the next stages.
 */
object CompatibilityEngine {

    val defaultWeights = ScoringWeights()

    /**
     * Calculates two-way mutual compatibility score (0.0 to 100.0).
     */
    fun calculateMutualScore(
        user: UserProfile,
        candidate: CandidateProfile,
        weights: ScoringWeights = defaultWeights
    ): Double {
        val scoreAtoB = calculateDirectionalScore(user, candidate, weights)
        val scoreBtoA = calculateDirectionalScoreCandidateToUser(candidate, user, weights)

        // Symmetrical harmonic/weighted blend
        val mutualScore = (scoreAtoB * 0.5) + (scoreBtoA * 0.5)
        return String.format(java.util.Locale.US, "%.1f", mutualScore.coerceIn(0.0, 100.0)).toDouble()
    }

    private fun calculateDirectionalScore(
        user: UserProfile,
        candidate: CandidateProfile,
        weights: ScoringWeights
    ): Double {
        var totalScore = 0.0

        // 1. Relationship Goal Alignment
        val goalScore = if (user.relationship_goal.equals(candidate.relationship_goal, ignoreCase = true)) {
            100.0
        } else if (isCompatibleGoal(user.relationship_goal, candidate.relationship_goal)) {
            70.0
        } else {
            30.0
        }
        totalScore += goalScore * weights.relationshipGoalWeight

        // 2. Personality Compatibility (Distance based)
        val personalityScore = calculatePersonalityScore(user.personality, candidate.personality)
        totalScore += personalityScore * weights.personalityWeight

        // 3. Lifestyle Compatibility
        val lifestyleScore = calculateLifestyleScore(user.lifestyle, candidate.lifestyle)
        totalScore += lifestyleScore * weights.lifestyleWeight

        // 4. Values Overlap (Jaccard)
        val valuesScore = calculateJaccardScore(user.values, candidate.values)
        totalScore += valuesScore * weights.valuesWeight

        // 5. Interests Overlap (Jaccard)
        val interestsScore = calculateJaccardScore(user.interests, candidate.interests)
        totalScore += interestsScore * weights.interestsWeight

        // 6. Age Proximity Score
        val ageDiff = abs(user.age - candidate.age)
        val ageScore = (100.0 - (ageDiff * 4.0)).coerceIn(20.0, 100.0)
        totalScore += ageScore * weights.ageProximityWeight

        return totalScore
    }

    private fun calculateDirectionalScoreCandidateToUser(
        candidate: CandidateProfile,
        user: UserProfile,
        weights: ScoringWeights
    ): Double {
        // Evaluate candidate's view of user
        return calculateDirectionalScore(user, candidate, weights)
    }

    private fun calculatePersonalityScore(p1: Personality, p2: Personality): Double {
        val diffIndep = abs(p1.independence - p2.independence)
        val diffSoc = abs(p1.sociability - p2.sociability)
        val diffOpen = abs(p1.openness - p2.openness)
        val diffEmot = abs(p1.emotional_stability - p2.emotional_stability)
        val diffAgree = abs(p1.agreeableness - p2.agreeableness)
        val diffConsc = abs(p1.conscientiousness - p2.conscientiousness)

        val avgDiff = (diffIndep + diffSoc + diffOpen + diffEmot + diffAgree + diffConsc) / 6.0
        return (100.0 - avgDiff).coerceIn(0.0, 100.0)
    }

    private fun calculateLifestyleScore(l1: Lifestyle, l2: Lifestyle): Double {
        val diffAct = abs(l1.activity_level - l2.activity_level)
        val diffTrv = abs(l1.travel - l2.travel)
        val diffSoc = abs(l1.social_life - l2.social_life)
        val diffCur = abs(l1.intellectual_curiosity - l2.intellectual_curiosity)
        val diffEcon = abs(l1.economic_style - l2.economic_style)

        val avgDiff = (diffAct + diffTrv + diffSoc + diffCur + diffEcon) / 5.0
        return (100.0 - avgDiff).coerceIn(0.0, 100.0)
    }

    private fun calculateJaccardScore(list1: List<String>, list2: List<String>): Double {
        if (list1.isEmpty() && list2.isEmpty()) return 50.0
        val s1 = list1.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val s2 = list2.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        if (s1.isEmpty() && s2.isEmpty()) return 50.0

        val intersection = s1.intersect(s2).size
        val union = s1.union(s2).size
        if (union == 0) return 50.0

        return (intersection.toDouble() / union.toDouble()) * 100.0
    }

    private fun isCompatibleGoal(g1: String, g2: String): Boolean {
        val g1n = g1.lowercase()
        val g2n = g2.lowercase()
        if ((g1n == "marriage" && g2n == "long_term") || (g1n == "long_term" && g2n == "marriage")) return true
        if ((g1n == "casual" && g2n == "friendship") || (g1n == "friendship" && g2n == "casual")) return true
        return false
    }
}
