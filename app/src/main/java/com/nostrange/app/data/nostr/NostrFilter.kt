package com.nostrange.app.data.nostr

import com.nostrange.app.security.Bech32
import com.nostrange.app.security.Secp256k1Crypto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

@Serializable
data class NostrFilter(
    val ids: List<String>? = null,
    val authors: List<String>? = null,
    val kinds: List<Int>? = null,
    @SerialName("#d") val dTags: List<String>? = null,
    @SerialName("#p") val pTags: List<String>? = null,
    val since: Long? = null,
    val until: Long? = null,
    val limit: Int? = null
) {
    fun toJson(): String {
        return buildJsonObject {
            ids?.let { putJsonArray("ids") { it.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } } }
            authors?.let { putJsonArray("authors") { it.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } } }
            kinds?.let { putJsonArray("kinds") { it.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } } }
            dTags?.let { putJsonArray("#d") { it.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } } }
            pTags?.let { putJsonArray("#p") { it.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } } }
            since?.let { put("since", it) }
            until?.let { put("until", it) }
            limit?.let { put("limit", it) }
        }.toString()
    }
}

object NostrSigner {

    fun createAndSignEvent(
        privateKey: ByteArray,
        kind: Int,
        tags: List<List<String>>,
        content: String,
        createdAt: Long = System.currentTimeMillis() / 1000
    ): NostrEvent {
        val pubBytes = Secp256k1Crypto.getPublicKey(privateKey)
        val pubkeyHex = Bech32.bytesToHex(pubBytes)

        val serialized = NostrEvent.serializeForId(pubkeyHex, createdAt, kind, tags, content)
        val idHash = Secp256k1Crypto.sha256(serialized.toByteArray(Charsets.UTF_8))
        val idHex = Bech32.bytesToHex(idHash)

        val sigBytes = Secp256k1Crypto.signSchnorr(idHash, privateKey)
        val sigHex = Bech32.bytesToHex(sigBytes)

        return NostrEvent(
            id = idHex,
            pubkey = pubkeyHex,
            createdAt = createdAt,
            kind = kind,
            tags = tags,
            content = content,
            sig = sigHex
        )
    }
}
