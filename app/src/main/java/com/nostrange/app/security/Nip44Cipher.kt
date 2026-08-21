package com.nostrange.app.security

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.ChaCha7539Engine
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64

/**
 * Nostr NIP-44 Version 2 Encryption and Decryption standard.
 * Uses ECDH, HKDF-SHA256, ChaCha20-Poly1305 / HMAC-SHA256 and padding for metadata privacy.
 */
object Nip44Cipher {
    private const val VERSION: Byte = 2
    private val SALT_V2 = "nip44-v2".toByteArray(Charsets.UTF_8)
    private val secureRandom = SecureRandom()

    fun getConversationKey(privateKey: ByteArray, remotePubKey: ByteArray): ByteArray {
        val sharedSecret = Secp256k1Crypto.getSharedSecret(privateKey, remotePubKey)
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(sharedSecret, SALT_V2, null))
        val conversationKey = ByteArray(32)
        hkdf.generateBytes(conversationKey, 0, 32)
        return conversationKey
    }

    /**
     * Encrypts plaintext using NIP-44 v2.
     */
    fun encrypt(plaintext: String, conversationKey: ByteArray): String {
        val plainBytes = plaintext.toByteArray(Charsets.UTF_8)
        val padded = pad(plainBytes)

        val nonce = ByteArray(32)
        secureRandom.nextBytes(nonce)

        val (encKey, authKey) = deriveKeys(conversationKey, nonce)

        // ChaCha20 encryption
        val ciphertext = chacha20(padded, encKey, nonce.copyOfRange(0, 12))

        // HMAC-SHA256 Authentication Tag
        val hmac = HMac(SHA256Digest())
        hmac.init(KeyParameter(authKey))
        val dataToAuth = nonce + ciphertext
        hmac.update(dataToAuth, 0, dataToAuth.size)
        val mac = ByteArray(32)
        hmac.doFinal(mac, 0)

        // Format: version (1 byte) + nonce (32 bytes) + ciphertext + mac (32 bytes)
        val output = ByteArray(1 + 32 + ciphertext.size + 32)
        output[0] = VERSION
        System.arraycopy(nonce, 0, output, 1, 32)
        System.arraycopy(ciphertext, 0, output, 33, ciphertext.size)
        System.arraycopy(mac, 0, output, 33 + ciphertext.size, 32)

        return Base64.getEncoder().encodeToString(output)
    }

    /**
     * Decrypts ciphertext using NIP-44 v2.
     */
    fun decrypt(payloadBase64: String, conversationKey: ByteArray): String {
        val payload = Base64.getDecoder().decode(payloadBase64)
        if (payload.size < 1 + 32 + 32 + 2) { // version + nonce + min_padded + mac
            throw IllegalArgumentException("Payload too short")
        }
        if (payload[0] != VERSION) {
            throw IllegalArgumentException("Unsupported NIP-44 version: ${payload[0]}")
        }

        val nonce = payload.copyOfRange(1, 33)
        val ciphertextSize = payload.size - 1 - 32 - 32
        val ciphertext = payload.copyOfRange(33, 33 + ciphertextSize)
        val receivedMac = payload.copyOfRange(33 + ciphertextSize, payload.size)

        val (encKey, authKey) = deriveKeys(conversationKey, nonce)

        // Verify MAC
        val hmac = HMac(SHA256Digest())
        hmac.init(KeyParameter(authKey))
        val dataToAuth = nonce + ciphertext
        hmac.update(dataToAuth, 0, dataToAuth.size)
        val computedMac = ByteArray(32)
        hmac.doFinal(computedMac, 0)

        if (!computedMac.contentEquals(receivedMac)) {
            throw SecurityException("Invalid MAC in NIP-44 payload")
        }

        val decryptedPadded = chacha20(ciphertext, encKey, nonce.copyOfRange(0, 12))
        val unpadded = unpad(decryptedPadded)
        return String(unpadded, Charsets.UTF_8)
    }

    private fun deriveKeys(conversationKey: ByteArray, nonce: ByteArray): Pair<ByteArray, ByteArray> {
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(conversationKey, nonce, null))
        val keys = ByteArray(64)
        hkdf.generateBytes(keys, 0, 64)
        val encKey = keys.copyOfRange(0, 32)
        val authKey = keys.copyOfRange(32, 64)
        return Pair(encKey, authKey)
    }

    private fun chacha20(data: ByteArray, key: ByteArray, nonce12: ByteArray): ByteArray {
        val engine = ChaCha7539Engine()
        val params = ParametersWithIV(KeyParameter(key), nonce12)
        engine.init(true, params)
        val out = ByteArray(data.size)
        engine.processBytes(data, 0, data.size, out, 0)
        return out
    }

    private fun pad(plaintext: ByteArray): ByteArray {
        val len = plaintext.size
        val paddedLen = calcPaddedLen(len)
        val buffer = ByteBuffer.allocate(2 + paddedLen)
        buffer.putShort(len.toShort())
        buffer.put(plaintext)
        // Zeroes fill the rest
        return buffer.array()
    }

    private fun unpad(padded: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(padded)
        val len = buffer.short.toInt() and 0xffff
        if (len > padded.size - 2) {
            throw IllegalArgumentException("Invalid padded payload length")
        }
        val unpadded = ByteArray(len)
        buffer.get(unpadded)
        return unpadded
    }

    private fun calcPaddedLen(len: Int): Int {
        if (len <= 32) return 32
        val nextPower = 1 shl (32 - Integer.numberOfLeadingZeros(len - 1))
        val chunk = if (nextPower <= 256) 32 else nextPower / 8
        return if (len % chunk == 0) len else len + (chunk - (len % chunk))
    }
}
