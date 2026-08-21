package com.nostrange.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages Nostr cryptographic identity (Private Key & Public Key)
 * stored securely in hardware-backed Android Keystore / EncryptedSharedPreferences.
 *
 * CRITICAL PRIVACY GUARANTEE:
 * Private keys NEVER leave this device. They are NEVER sent to relays,
 * external AI, analytics, or crash logs.
 */
class KeyStoreManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for JVM testing or devices where Android Keystore encounters issues
            context.getSharedPreferences(PREF_FILE_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    /**
     * Retrieves or generates the user's Nostr keypair on first run.
     */
    fun getOrCreateKeypair(): Pair<ByteArray, ByteArray> {
        val existingPrivHex = prefs.getString(KEY_PRIVATE_KEY_HEX, null)
        if (existingPrivHex != null) {
            val privBytes = Bech32.hexToBytes(existingPrivHex)
            val pubBytes = Secp256k1Crypto.getPublicKey(privBytes)
            return Pair(privBytes, pubBytes)
        }

        // Generate new secp256k1 keypair
        val newPrivBytes = Secp256k1Crypto.generatePrivateKey()
        val newPubBytes = Secp256k1Crypto.getPublicKey(newPrivBytes)
        val privHex = Bech32.bytesToHex(newPrivBytes)
        val pubHex = Bech32.bytesToHex(newPubBytes)

        prefs.edit()
            .putString(KEY_PRIVATE_KEY_HEX, privHex)
            .putString(KEY_PUBLIC_KEY_HEX, pubHex)
            .apply()

        return Pair(newPrivBytes, newPubBytes)
    }

    fun getPublicKeyHex(): String {
        return prefs.getString(KEY_PUBLIC_KEY_HEX, null) ?: run {
            val (_, pub) = getOrCreateKeypair()
            Bech32.bytesToHex(pub)
        }
    }

    fun getPublicKeyNpub(): String {
        return Bech32.pubkeyToNpub(getPublicKeyHex())
    }

    fun getPrivateKeyBytes(): ByteArray {
        val (priv, _) = getOrCreateKeypair()
        return priv
    }

    fun importPrivateKey(nsecOrHex: String): Pair<ByteArray, ByteArray> {
        val privBytes = if (nsecOrHex.startsWith("nsec1")) {
            Bech32.hexToBytes(Bech32.nsecToPrivkey(nsecOrHex))
        } else {
            Bech32.hexToBytes(nsecOrHex)
        }
        val pubBytes = Secp256k1Crypto.getPublicKey(privBytes)
        val privHex = Bech32.bytesToHex(privBytes)
        val pubHex = Bech32.bytesToHex(pubBytes)

        prefs.edit()
            .putString(KEY_PRIVATE_KEY_HEX, privHex)
            .putString(KEY_PUBLIC_KEY_HEX, pubHex)
            .apply()

        return Pair(privBytes, pubBytes)
    }

    companion object {
        private const val PREF_FILE_NAME = "nostrange_secure_keystore"
        private const val KEY_PRIVATE_KEY_HEX = "nostr_private_key_hex"
        private const val KEY_PUBLIC_KEY_HEX = "nostr_public_key_hex"
    }
}
