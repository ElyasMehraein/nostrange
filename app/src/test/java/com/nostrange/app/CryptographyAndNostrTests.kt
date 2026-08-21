package com.nostrange.app

import com.nostrange.app.data.nostr.NostrEvent
import com.nostrange.app.data.nostr.NostrEventKind
import com.nostrange.app.data.nostr.NostrSigner
import com.nostrange.app.security.Bech32
import com.nostrange.app.security.Nip44Cipher
import com.nostrange.app.security.Secp256k1Crypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptographyAndNostrTests {

    @Test
    fun testSecp256k1KeyGenerationAndSchnorrSignature() {
        val privateKey = Secp256k1Crypto.generatePrivateKey()
        assertEquals(32, privateKey.size)

        val publicKey = Secp256k1Crypto.getPublicKey(privateKey)
        assertEquals(32, publicKey.size)

        val messageHash = Secp256k1Crypto.sha256("Hello Nostrange!".toByteArray(Charsets.UTF_8))
        val signature = Secp256k1Crypto.signSchnorr(messageHash, privateKey)
        assertEquals(64, signature.size)

        val isValid = Secp256k1Crypto.verifySchnorr(messageHash, publicKey, signature)
        assertTrue("Schnorr signature must verify with public key", isValid)
    }

    @Test
    fun testBech32Encoding() {
        val privKey = Secp256k1Crypto.generatePrivateKey()
        val pubKey = Secp256k1Crypto.getPublicKey(privKey)

        val pubHex = Bech32.bytesToHex(pubKey)
        val npub = Bech32.pubkeyToNpub(pubHex)
        assertTrue(npub.startsWith("npub1"))

        val decodedPubHex = Bech32.npubToPubkey(npub)
        assertEquals(pubHex, decodedPubHex)

        val privHex = Bech32.bytesToHex(privKey)
        val nsec = Bech32.privkeyToNsec(privHex)
        assertTrue(nsec.startsWith("nsec1"))

        val decodedPrivHex = Bech32.nsecToPrivkey(nsec)
        assertEquals(privHex, decodedPrivHex)
    }

    @Test
    fun testNip44EncryptionAndDecryptionRoundtrip() {
        // Alice generates keypair
        val alicePriv = Secp256k1Crypto.generatePrivateKey()
        val alicePub = Secp256k1Crypto.getPublicKey(alicePriv)

        // Bob generates keypair
        val bobPriv = Secp256k1Crypto.generatePrivateKey()
        val bobPub = Secp256k1Crypto.getPublicKey(bobPriv)

        // Alice derives conversation key with Bob
        val aliceConversationKey = Nip44Cipher.getConversationKey(alicePriv, bobPub)

        // Bob derives conversation key with Alice
        val bobConversationKey = Nip44Cipher.getConversationKey(bobPriv, alicePub)

        // Conversation keys must match exactly (ECDH symmetry)
        assertEquals(
            Bech32.bytesToHex(aliceConversationKey),
            Bech32.bytesToHex(bobConversationKey)
        )

        val secretMessage = "سلام! این یک پیام محرمانه رمزنگاری شده با NIP-44 در Nostrange است."
        val encryptedPayload = Nip44Cipher.encrypt(secretMessage, aliceConversationKey)

        // Bob decrypts payload
        val decryptedText = Nip44Cipher.decrypt(encryptedPayload, bobConversationKey)
        assertEquals(secretMessage, decryptedText)
    }

    @Test
    fun testNostrEventSigningAndVerification() {
        val privKey = Secp256k1Crypto.generatePrivateKey()
        val tags = listOf(
            listOf("d", "nostrange-match-profile"),
            listOf("c", "IR")
        )
        val content = "{\"schema_version\":1,\"country\":\"IR\"}"

        val event = NostrSigner.createAndSignEvent(
            privateKey = privKey,
            kind = NostrEventKind.MATCHABLE_PROFILE_KIND,
            tags = tags,
            content = content
        )

        assertEquals(64, event.id.length)
        assertEquals(128, event.sig.length)
        assertTrue("Signed Nostr event must verify correctly", event.verify())
    }
}
