package com.nostrange.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nostrange.app.data.local.entity.BlockedPubkeyEntity
import com.nostrange.app.data.local.entity.CandidateEntity
import com.nostrange.app.data.local.entity.MessageEntity
import com.nostrange.app.data.local.entity.RelayEntity
import com.nostrange.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun deleteProfile()
}

@Dao
interface CandidateDao {
    @Query("SELECT * FROM candidates WHERE isBlocked = 0 ORDER BY initialScore DESC")
    fun getAllCandidates(): Flow<List<CandidateEntity>>

    @Query("SELECT * FROM candidates WHERE isBlocked = 0 AND aiRank IS NOT NULL ORDER BY aiRank ASC")
    fun getTopAiMatches(): Flow<List<CandidateEntity>>

    @Query("SELECT * FROM candidates WHERE pubkey = :pubkey LIMIT 1")
    suspend fun getCandidateByPubkey(pubkey: String): CandidateEntity?

    @Query("SELECT * FROM candidates WHERE isBlocked = 0 ORDER BY initialScore DESC LIMIT :limit")
    suspend fun getTopCandidatesForAiPrompt(limit: Int = 500): List<CandidateEntity>

    @Query("SELECT COUNT(*) FROM candidates")
    fun getCandidateCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidates(candidates: List<CandidateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidate(candidate: CandidateEntity)

    @Update
    suspend fun updateCandidate(candidate: CandidateEntity)

    @Query("UPDATE candidates SET isIntroSent = 1 WHERE pubkey = :pubkey")
    suspend fun markIntroSent(pubkey: String)

    @Query("UPDATE candidates SET isBlocked = 1 WHERE pubkey = :pubkey")
    suspend fun blockCandidate(pubkey: String)

    @Query("UPDATE candidates SET aiRank = :rank, aiScore = :score, aiReasonsJson = :reasonsJson WHERE pubkey = :pubkey")
    suspend fun updateAiRank(pubkey: String, rank: Int, score: Double, reasonsJson: String)

    @Query("UPDATE candidates SET aiRank = NULL, aiScore = NULL, aiReasonsJson = '[]'")
    suspend fun resetAiRankings()

    @Query("DELETE FROM candidates")
    suspend fun clearAllCandidates()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationPubkey = :conversationPubkey ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationPubkey: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationPubkey = :conversationPubkey ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessage(conversationPubkey: String): MessageEntity?

    @Query("SELECT DISTINCT conversationPubkey FROM messages ORDER BY timestamp DESC")
    fun getAllConversationPubkeys(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationPubkey = :conversationPubkey")
    suspend fun deleteConversationMessages(conversationPubkey: String)
}

@Dao
interface BlockedPubkeyDao {
    @Query("SELECT * FROM blocked_pubkeys")
    fun getBlockedPubkeys(): Flow<List<BlockedPubkeyEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_pubkeys WHERE pubkey = :pubkey)")
    suspend fun isBlocked(pubkey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockPubkey(blocked: BlockedPubkeyEntity)

    @Query("DELETE FROM blocked_pubkeys WHERE pubkey = :pubkey")
    suspend fun unblockPubkey(pubkey: String)
}

@Dao
interface RelayDao {
    @Query("SELECT * FROM relays")
    fun getRelays(): Flow<List<RelayEntity>>

    @Query("SELECT * FROM relays")
    suspend fun getRelaysOnce(): List<RelayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelays(relays: List<RelayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelay(relay: RelayEntity)

    @Query("DELETE FROM relays WHERE url = :url")
    suspend fun deleteRelay(url: String)
}
