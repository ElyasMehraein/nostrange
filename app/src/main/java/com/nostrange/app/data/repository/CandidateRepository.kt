package com.nostrange.app.data.repository

import android.util.Log
import com.nostrange.app.ai.schema.MatchingResultSchema
import com.nostrange.app.ai.schema.ProfileJsonSchema
import com.nostrange.app.data.local.dao.BlockedPubkeyDao
import com.nostrange.app.data.local.dao.CandidateDao
import com.nostrange.app.data.local.entity.CandidateEntity
import com.nostrange.app.data.nostr.NostrClient
import com.nostrange.app.data.nostr.NostrEventKind
import com.nostrange.app.data.nostr.NostrFilter
import com.nostrange.app.domain.matching.CandidateRanker
import com.nostrange.app.domain.matching.CompatibilityEngine
import com.nostrange.app.domain.matching.HardFilterEngine
import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.domain.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CandidateRepository(
    private val candidateDao: CandidateDao,
    private val blockedPubkeyDao: BlockedPubkeyDao,
    private val nostrClient: NostrClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val json = Json { ignoreUnknownKeys = true }

    // 10 Days in seconds (864,000s)
    private fun get10DaysCutoffTimestamp(): Long {
        return (System.currentTimeMillis() / 1000) - (10 * 24 * 60 * 60)
    }

    val allCandidates: Flow<List<CandidateProfile>>
        get() = candidateDao.getActiveCandidates(get10DaysCutoffTimestamp()).map { list ->
            list.map { mapEntityToDomain(it) }
        }

    val topAiMatches: Flow<List<CandidateProfile>>
        get() = candidateDao.getTopAiMatches(get10DaysCutoffTimestamp()).map { list ->
            list.map { mapEntityToDomain(it) }
        }

    val candidateCount: Flow<Int>
        get() = candidateDao.getCandidateCount(get10DaysCutoffTimestamp())

    init {
        // Listen to incoming Nostr matchable profile events from relays
        scope.launch {
            nostrClient.incomingEvents.collect { event ->
                if (event.kind == NostrEventKind.MATCHABLE_PROFILE_KIND) {
                    processIncomingProfileEvent(event.pubkey, event.content, event.createdAt)
                }
            }
        }
    }

    fun startCandidateSync() {
        val filter = NostrFilter(
            kinds = listOf(NostrEventKind.MATCHABLE_PROFILE_KIND),
            dTags = listOf("nostrange-match-profile"),
            since = get10DaysCutoffTimestamp(),
            limit = 1000
        )
        nostrClient.subscribe("nostrange-candidates-sub", listOf(filter))
    }

    /**
     * Executes the local candidate generation pipeline:
     * Room DB profiles (active within 10 days) -> Hard Filters -> Compatibility Scoring -> Top 100
     */
    suspend fun runLocalCandidateGeneration(user: UserProfile, targetLimit: Int = 100): List<CandidateProfile> =
        withContext(Dispatchers.IO) {
            val cutoff = get10DaysCutoffTimestamp()
            val allLocal = candidateDao.getTopCandidatesForAiPrompt(cutoff, 10000).map { mapEntityToDomain(it) }
            val topCandidates = CandidateRanker.generateTopCandidates(user, allLocal, targetLimit)

            // Persist initial scores back to DB
            for (c in topCandidates) {
                candidateDao.getCandidateByPubkey(c.pubkey)?.let { entity ->
                    candidateDao.updateCandidate(entity.copy(initialScore = c.initial_score))
                }
            }

            topCandidates
        }

    /**
     * Imports and validates Top-20 AI Matching Result.
     */
    suspend fun importAiMatchingResult(rawJson: String): Result<Int> = withContext(Dispatchers.IO) {
        val cutoff = get10DaysCutoffTimestamp()
        val topCandidates = candidateDao.getTopCandidatesForAiPrompt(cutoff, 100)
        val validPubkeys = topCandidates.map { it.pubkey }.toSet()

        val parseResult = MatchingResultSchema.parseAndValidateMatchingResult(rawJson, validPubkeys)

        parseResult.mapCatching { aiResult ->
            // Reset previous rankings
            candidateDao.resetAiRankings()

            var count = 0
            for (match in aiResult.matches) {
                val candidateEntity = candidateDao.getCandidateByPubkey(match.pubkey)
                if (candidateEntity != null) {
                    candidateDao.updateAiRank(
                        pubkey = match.pubkey,
                        rank = match.rank,
                        score = match.compatibilityScore,
                        reasonsJson = json.encodeToString(match.reasons)
                    )
                    count++
                }
            }
            count
        }
    }

    suspend fun markIntroSent(pubkey: String) = withContext(Dispatchers.IO) {
        candidateDao.markIntroSent(pubkey)
    }

    suspend fun blockCandidate(pubkey: String) = withContext(Dispatchers.IO) {
        candidateDao.blockCandidate(pubkey)
    }

    suspend fun getCandidateByPubkey(pubkey: String): CandidateProfile? = withContext(Dispatchers.IO) {
        candidateDao.getCandidateByPubkey(pubkey)?.let { mapEntityToDomain(it) }
    }

    suspend fun clearAllCandidates() = withContext(Dispatchers.IO) {
        candidateDao.clearAllCandidates()
    }

    private suspend fun processIncomingProfileEvent(pubkey: String, contentJson: String, eventCreatedAt: Long) {
        if (blockedPubkeyDao.isBlocked(pubkey)) return

        val parseResult = ProfileJsonSchema.parseAndValidateProfileJson(contentJson, pubkey)
        parseResult.onSuccess { profile ->
            val lastActive = if (profile.last_active_at > 0) profile.last_active_at else eventCreatedAt
            val entity = CandidateEntity(
                pubkey = profile.pubkey,
                schemaVersion = profile.schema_version,
                country = profile.country,
                region = profile.region,
                age = profile.age,
                gender = profile.gender,
                targetGendersJson = json.encodeToString(profile.target_genders),
                relationshipGoal = profile.relationship_goal,
                wantsMarriage = profile.wants_marriage,
                wantsChildren = profile.wants_children,
                personalityJson = json.encodeToString(profile.personality),
                lifestyleJson = json.encodeToString(profile.lifestyle),
                interestsJson = json.encodeToString(profile.interests),
                valuesJson = json.encodeToString(profile.values),
                dealBreakersJson = json.encodeToString(profile.deal_breakers),
                initialScore = 0.0,
                aiRank = null,
                aiScore = null,
                aiReasonsJson = "[]",
                isIntroSent = false,
                isBlocked = false,
                lastActiveAt = lastActive,
                updatedAt = System.currentTimeMillis() / 1000
            )
            candidateDao.insertCandidate(entity)
        }
    }

    private fun mapEntityToDomain(entity: CandidateEntity): CandidateProfile {
        return CandidateProfile(
            pubkey = entity.pubkey,
            schema_version = entity.schemaVersion,
            country = entity.country,
            region = entity.region,
            age = entity.age,
            gender = entity.gender,
            target_genders = json.decodeFromString(entity.targetGendersJson),
            relationship_goal = entity.relationshipGoal,
            wants_marriage = entity.wantsMarriage,
            wants_children = entity.wantsChildren,
            personality = json.decodeFromString(entity.personalityJson),
            lifestyle = json.decodeFromString(entity.lifestyleJson),
            interests = json.decodeFromString(entity.interestsJson),
            values = json.decodeFromString(entity.valuesJson),
            deal_breakers = json.decodeFromString(entity.dealBreakersJson),
            initial_score = entity.initialScore,
            ai_rank = entity.aiRank,
            ai_score = entity.aiScore,
            ai_reasons = json.decodeFromString(entity.aiReasonsJson),
            is_intro_sent = entity.isIntroSent,
            is_blocked = entity.isBlocked,
            last_active_at = entity.lastActiveAt,
            updated_at = entity.updatedAt
        )
    }
}
