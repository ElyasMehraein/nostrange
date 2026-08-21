package com.nostrange.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val pubkey: String,
    val schemaVersion: Int,
    val country: String,
    val region: String,
    val age: Int,
    val gender: String,
    val targetGendersJson: String,
    val relationshipGoal: String,
    val wantsMarriage: Boolean,
    val wantsChildren: Boolean,
    val personalityJson: String,
    val lifestyleJson: String,
    val interestsJson: String,
    val valuesJson: String,
    val preferencesJson: String,
    val dealBreakersJson: String,
    val lastActiveAt: Long,
    val createdAt: Long
)

@Entity(
    tableName = "candidates",
    indices = [
        Index(value = ["pubkey"], unique = true),
        Index(value = ["country"]),
        Index(value = ["region"]),
        Index(value = ["age"]),
        Index(value = ["gender"]),
        Index(value = ["relationshipGoal"]),
        Index(value = ["initialScore"]),
        Index(value = ["aiRank"]),
        Index(value = ["lastActiveAt"])
    ]
)
data class CandidateEntity(
    @PrimaryKey val pubkey: String,
    val schemaVersion: Int,
    val country: String,
    val region: String,
    val age: Int,
    val gender: String,
    val targetGendersJson: String,
    val relationshipGoal: String,
    val wantsMarriage: Boolean,
    val wantsChildren: Boolean,
    val personalityJson: String,
    val lifestyleJson: String,
    val interestsJson: String,
    val valuesJson: String,
    val dealBreakersJson: String,
    val initialScore: Double,
    val aiRank: Int?,
    val aiScore: Double?,
    val aiReasonsJson: String,
    val isIntroSent: Boolean = false,
    val isBlocked: Boolean = false,
    val lastActiveAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationPubkey"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationPubkey: String,
    val senderPubkey: String,
    val content: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val isRead: Boolean = true
)

@Entity(tableName = "blocked_pubkeys")
data class BlockedPubkeyEntity(
    @PrimaryKey val pubkey: String,
    val reason: String? = null,
    val blockedAt: Long = System.currentTimeMillis() / 1000
)

@Entity(tableName = "relays")
data class RelayEntity(
    @PrimaryKey val url: String,
    val read: Boolean = true,
    val write: Boolean = true,
    val isDefault: Boolean = false
)
