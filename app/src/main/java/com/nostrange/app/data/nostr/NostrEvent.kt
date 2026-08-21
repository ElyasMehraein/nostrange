package com.nostrange.app.data.nostr

import com.nostrange.app.security.Bech32
import com.nostrange.app.security.Secp256k1Crypto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

object NostrEventKind {
    const val SET_METADATA = 0
    const val TEXT_NOTE = 1
    const val DIRECT_MESSAGE_NIP04 = 4
    const val SEAL = 13
    const val CHAT_MESSAGE_NIP17 = 14
    const val GIFT_WRAP_NIP59 = 1059
    const val MATCHABLE_PROFILE_KIND = 30078 // Parameterized Replaceable Event for Nostrange Profiles
}

@Serializable
data class NostrEvent(
    val id: String,
    val pubkey: String,
    @SerialName("created_at") val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>> = emptyList(),
    val content: String,
    val sig: String
) {
    fun verify(): Boolean {
        return try {
            val serializedArray = serializeForId(pubkey, createdAt, kind, tags, content)
            val expectedHash = Secp256k1Crypto.sha256(serializedArray.toByteArray(Charsets.UTF_8))
            val expectedId = Bech32.bytesToHex(expectedHash)

            if (id != expectedId) return false

            val pubBytes = Bech32.hexToBytes(pubkey)
            val sigBytes = Bech32.hexToBytes(sig)
            Secp256k1Crypto.verifySchnorr(expectedHash, pubBytes, sigBytes)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun serializeForId(
            pubkey: String,
            createdAt: Long,
            kind: Int,
            tags: List<List<String>>,
            content: String
        ): String {
            val jsonArray = buildJsonArray {
                add(JsonPrimitive(0))
                add(JsonPrimitive(pubkey.lowercase()))
                add(JsonPrimitive(createdAt))
                add(JsonPrimitive(kind))
                add(JsonArray(tags.map { tagList -> JsonArray(tagList.map { JsonPrimitive(it) }) }))
                add(JsonPrimitive(content))
            }
            return Json.encodeToString(jsonArray)
        }
    }
}
