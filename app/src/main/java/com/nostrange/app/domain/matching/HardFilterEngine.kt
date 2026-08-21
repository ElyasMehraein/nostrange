package com.nostrange.app.domain.matching

import com.nostrange.app.ai.schema.JsonSanitizerUtils
import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.domain.model.UserProfile

/**
 * Detailed diagnostics on why candidates were filtered out by HardFilterEngine.
 */
data class FilterRejectionDetails(
    var totalCandidates: Int = 0,
    var selfCount: Int = 0,
    var inactiveCount: Int = 0,
    var genderCount: Int = 0,
    var ageCount: Int = 0,
    var countryCount: Int = 0,
    var goalMarriageCount: Int = 0,
    var dealBreakerCount: Int = 0
) {
    fun toUserFriendlySummary(): String {
        val reasons = mutableListOf<String>()
        if (selfCount > 0) reasons.add("• $selfCount مورد: پروفایل حساب کاربری خود شماست.")
        if (genderCount > 0) reasons.add("• $genderCount مورد: عدم تطابق جنسیت دوطرفه (جنسیت مورد نظر شما با کاندیدا همخوانی ندارد).")
        if (ageCount > 0) reasons.add("• $ageCount مورد: خارج از محدوده سنی تعیین‌شده در فیلترها.")
        if (countryCount > 0) reasons.add("• $countryCount مورد: عدم تطابق کشور محل سکونت.")
        if (goalMarriageCount > 0) reasons.add("• $goalMarriageCount مورد: تفاوت اساسی در قصد ازدواج / هدف رابطه.")
        if (dealBreakerCount > 0) reasons.add("• $dealBreakerCount مورد: برخورد با خطوط قرمز (Deal Breakers).")
        if (inactiveCount > 0) reasons.add("• $inactiveCount مورد: کاندیدا بیش از ۱۰ روز غیرفعال بوده است.")

        return if (reasons.isNotEmpty()) {
            "از بین $totalCandidates کاندیدای موجود در دستگاه، هیچ موردی با فیلترهای پایه شما همخوانی نداشت:\n\n" +
                    reasons.joinToString("\n") +
                    "\n\n💡 پیشنهاد: در بخش Me (پروفایل)، بازه سنی یا ترجیحات خود را گسترده‌تر کنید."
        } else {
            "هیچ کاندیدایی در پایگاه داده دستگاه یافت نشد. لطفاً منتظر دریافت کاندیداها از رله‌ها باشید."
        }
    }
}

/**
 * Hard Filtering Engine.
 * Evaluates non-negotiable baseline compatibility requirements.
 * Filters 10,000 candidates down to compliant candidates.
 */
object HardFilterEngine {

    private const val TEN_DAYS_SECONDS = 10 * 24 * 60 * 60

    fun checkEligibility(user: UserProfile, candidate: CandidateProfile, diagnostics: FilterRejectionDetails? = null): Boolean {
        // Do not match user with oneself
        if (user.pubkey.equals(candidate.pubkey, ignoreCase = true)) {
            diagnostics?.selfCount = (diagnostics?.selfCount ?: 0) + 1
            return false
        }

        // 0. Active Status Check: Ignore users inactive for more than 10 days
        val now = System.currentTimeMillis() / 1000
        if (candidate.last_active_at > 0 && (now - candidate.last_active_at) > TEN_DAYS_SECONDS) {
            diagnostics?.inactiveCount = (diagnostics?.inactiveCount ?: 0) + 1
            return false
        }

        // 1. Gender Compatibility (Mutual)
        val userAcceptsCandidateGender = user.target_genders.any { it.equals(candidate.gender, ignoreCase = true) }
        val candidateAcceptsUserGender = candidate.target_genders.isEmpty() ||
                candidate.target_genders.any { it.equals(user.gender, ignoreCase = true) }

        if (!userAcceptsCandidateGender || !candidateAcceptsUserGender) {
            diagnostics?.genderCount = (diagnostics?.genderCount ?: 0) + 1
            return false
        }

        // 2. Age Constraints
        if (candidate.age < user.preferences.min_age || candidate.age > user.preferences.max_age) {
            diagnostics?.ageCount = (diagnostics?.ageCount ?: 0) + 1
            return false
        }

        // 3. Country / Location constraints (resilient and normalized)
        val isCountryOk = JsonSanitizerUtils.isCountryCompatible(
            user.country,
            candidate.country,
            user.preferences.allow_different_country
        )
        if (!isCountryOk) {
            diagnostics?.countryCount = (diagnostics?.countryCount ?: 0) + 1
            return false
        }

        // 4. Fundamental Marriage / Children deal-breakers (if explicitly specified as non-negotiable)
        if (user.relationship_goal.equals("marriage", ignoreCase = true) && !candidate.wants_marriage) {
            diagnostics?.goalMarriageCount = (diagnostics?.goalMarriageCount ?: 0) + 1
            return false
        }
        if (candidate.relationship_goal.equals("marriage", ignoreCase = true) && !user.wants_marriage) {
            diagnostics?.goalMarriageCount = (diagnostics?.goalMarriageCount ?: 0) + 1
            return false
        }

        // 5. Deal Breakers check
        if (user.deal_breakers.isNotEmpty()) {
            for (db in user.deal_breakers) {
                val dbNorm = db.trim().lowercase()
                if (candidate.interests.any { it.trim().lowercase() == dbNorm } ||
                    candidate.values.any { it.trim().lowercase() == dbNorm }) {
                    diagnostics?.dealBreakerCount = (diagnostics?.dealBreakerCount ?: 0) + 1
                    return false
                }
            }
        }

        if (candidate.deal_breakers.isNotEmpty()) {
            for (db in candidate.deal_breakers) {
                val dbNorm = db.trim().lowercase()
                if (user.interests.any { it.trim().lowercase() == dbNorm } ||
                    user.values.any { it.trim().lowercase() == dbNorm }) {
                    diagnostics?.dealBreakerCount = (diagnostics?.dealBreakerCount ?: 0) + 1
                    return false
                }
            }
        }

        return true
    }

    fun isEligible(user: UserProfile, candidate: CandidateProfile): Boolean {
        return checkEligibility(user, candidate, null)
    }

    fun filterCandidates(user: UserProfile, candidates: List<CandidateProfile>): List<CandidateProfile> {
        return candidates.filter { checkEligibility(user, it, null) }
    }

    fun filterCandidatesWithDiagnostics(
        user: UserProfile,
        candidates: List<CandidateProfile>
    ): Pair<List<CandidateProfile>, FilterRejectionDetails> {
        val diagnostics = FilterRejectionDetails(totalCandidates = candidates.size)
        val passed = candidates.filter { checkEligibility(user, it, diagnostics) }
        return Pair(passed, diagnostics)
    }
}
