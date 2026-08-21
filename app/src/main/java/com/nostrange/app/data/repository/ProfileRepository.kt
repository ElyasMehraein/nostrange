package com.nostrange.app.data.repository

import com.nostrange.app.ai.schema.PrivacyEnforcer
import com.nostrange.app.ai.schema.ProfileJsonSchema
import com.nostrange.app.data.local.dao.ProfileDao
import com.nostrange.app.data.local.entity.UserProfileEntity
import com.nostrange.app.data.nostr.NostrClient
import com.nostrange.app.data.nostr.NostrEventKind
import com.nostrange.app.data.nostr.NostrSigner
import com.nostrange.app.domain.model.Lifestyle
import com.nostrange.app.domain.model.Personality
import com.nostrange.app.domain.model.Preferences
import com.nostrange.app.domain.model.UserProfile
import com.nostrange.app.security.KeyStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val keyStoreManager: KeyStoreManager,
    private val nostrClient: NostrClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    val userProfileFlow: Flow<UserProfile?> = profileDao.getUserProfile().map { entity ->
        entity?.let { mapEntityToDomain(it) }
    }

    suspend fun getUserProfileOnce(): UserProfile? = withContext(Dispatchers.IO) {
        profileDao.getUserProfileOnce()?.let { mapEntityToDomain(it) }
    }

    /**
     * Imports structured Profile JSON received from external AI/Agent.
     * Enforces privacy checks, saves locally in Room, and signs & publishes to Nostr.
     */
    suspend fun importProfileJson(rawJson: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val userPubkey = keyStoreManager.getPublicKeyHex()
        val parseResult = ProfileJsonSchema.parseAndValidateProfileJson(rawJson, userPubkey)

        parseResult.onSuccess { profile ->
            saveProfileLocally(profile)
            publishProfileToNostr(profile)
        }

        parseResult
    }

    suspend fun saveProfileLocally(profile: UserProfile) = withContext(Dispatchers.IO) {
        val entity = UserProfileEntity(
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
            preferencesJson = json.encodeToString(profile.preferences),
            dealBreakersJson = json.encodeToString(profile.deal_breakers),
            lastActiveAt = profile.last_active_at,
            createdAt = profile.created_at
        )
        profileDao.insertOrUpdateProfile(entity)
    }

    /**
     * Broadcasts current online status / updated matchable profile to Nostr network.
     * Triggered every time the user opens the application.
     */
    suspend fun broadcastOnlineStatus() = withContext(Dispatchers.IO) {
        val existingProfile = getUserProfileOnce() ?: return@withContext
        val updatedProfile = existingProfile.copy(last_active_at = System.currentTimeMillis() / 1000)
        saveProfileLocally(updatedProfile)
        publishProfileToNostr(updatedProfile)
    }

    /**
     * Publishes public matchable profile to Nostr network as a replaceable event.
     * GUARANTEE: NEVER includes real name, phone, email, media, or contact info.
     */
    suspend fun publishProfileToNostr(profile: UserProfile) = withContext(Dispatchers.IO) {
        val cleanProfileJson = json.encodeToString(profile)
        val privateKey = keyStoreManager.getPrivateKeyBytes()

        val tags = listOf(
            listOf("d", "nostrange-match-profile"),
            listOf("c", profile.country),
            listOf("r", profile.region),
            listOf("g", profile.gender),
            listOf("goal", profile.relationship_goal),
            listOf("v", "1")
        )

        val event = NostrSigner.createAndSignEvent(
            privateKey = privateKey,
            kind = NostrEventKind.MATCHABLE_PROFILE_KIND,
            tags = tags,
            content = cleanProfileJson,
            createdAt = profile.last_active_at
        )

        nostrClient.publishEvent(event)
    }

    private fun mapEntityToDomain(entity: UserProfileEntity): UserProfile {
        return UserProfile(
            schema_version = entity.schemaVersion,
            pubkey = entity.pubkey,
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
            preferences = json.decodeFromString(entity.preferencesJson),
            deal_breakers = json.decodeFromString(entity.dealBreakersJson),
            last_active_at = entity.lastActiveAt,
            created_at = entity.createdAt
        )
    }
}
