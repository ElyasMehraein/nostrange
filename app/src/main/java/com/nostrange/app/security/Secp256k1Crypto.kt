package com.nostrange.app.security

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Pure cryptographic implementation of Secp256k1, BIP-340 Schnorr Signatures, and ECDH
 * for Nostr identity and encrypted communication.
 */
object Secp256k1Crypto {
    private val ecParams = SECNamedCurves.getByName("secp256k1")
    val curve = ECDomainParameters(ecParams.curve, ecParams.g, ecParams.n, ecParams.h)
    private val secureRandom = SecureRandom()

    fun generatePrivateKey(): ByteArray {
        val key = ByteArray(32)
        do {
            secureRandom.nextBytes(key)
        } while (BigInteger(1, key) >= curve.n || BigInteger(1, key) == BigInteger.ZERO)
        return key
    }

    fun getPublicKey(privateKey: ByteArray): ByteArray {
        val privInt = BigInteger(1, privateKey)
        val point: ECPoint = curve.g.multiply(privInt).normalize()
        val xBytes = point.affineXCoord.toBigInteger().toByteArray()
        return fix32Bytes(xBytes)
    }

    /**
     * BIP-340 Schnorr Signature generation
     */
    fun signSchnorr(messageHash: ByteArray, privateKey: ByteArray): ByteArray {
        val d0 = BigInteger(1, privateKey)
        if (d0 < BigInteger.ONE || d0 >= curve.n) {
            throw IllegalArgumentException("Private key out of range")
        }

        val p = curve.g.multiply(d0).normalize()
        val d = if (p.affineYCoord.toBigInteger().testBit(0)) {
            curve.n.subtract(d0)
        } else {
            d0
        }

        val px = fix32Bytes(p.affineXCoord.toBigInteger().toByteArray())
        val aux = ByteArray(32)
        secureRandom.nextBytes(aux)

        val t = taggedHash("BIP0340/aux", aux)
        val dBytes = fix32Bytes(d.toByteArray())
        val xorVal = ByteArray(32)
        for (i in 0 until 32) {
            xorVal[i] = (dBytes[i].toInt() xor t[i].toInt()).toByte()
        }

        val nonceBytes = taggedHash("BIP0340/nonce", xorVal + px + messageHash)
        val k0 = BigInteger(1, nonceBytes).mod(curve.n)
        if (k0 == BigInteger.ZERO) {
            throw IllegalStateException("k0 is zero")
        }

        val r = curve.g.multiply(k0).normalize()
        val k = if (r.affineYCoord.toBigInteger().testBit(0)) {
            curve.n.subtract(k0)
        } else {
            k0
        }

        val rx = fix32Bytes(r.affineXCoord.toBigInteger().toByteArray())
        val eBytes = taggedHash("BIP0340/challenge", rx + px + messageHash)
        val e = BigInteger(1, eBytes).mod(curve.n)

        val s = k.add(e.multiply(d)).mod(curve.n)
        val sBytes = fix32Bytes(s.toByteArray())

        return rx + sBytes
    }

    /**
     * BIP-340 Schnorr Signature verification
     */
    fun verifySchnorr(messageHash: ByteArray, publicKey: ByteArray, signature: ByteArray): Boolean {
        if (publicKey.size != 32 || signature.size != 64) return false
        val px = BigInteger(1, publicKey)
        if (px >= (curve.curve.field.characteristic)) return false

        val pointP = try {
            liftX(publicKey) ?: return false
        } catch (e: Exception) {
            return false
        }

        val rx = signature.copyOfRange(0, 32)
        val sBytes = signature.copyOfRange(32, 64)
        val s = BigInteger(1, sBytes)
        if (s >= curve.n) return false

        val eBytes = taggedHash("BIP0340/challenge", rx + publicKey + messageHash)
        val e = BigInteger(1, eBytes).mod(curve.n)

        val r = curve.g.multiply(s).subtract(pointP.multiply(e)).normalize()
        if (r.isInfinity) return false
        if (r.affineYCoord.toBigInteger().testBit(0)) return false

        val computedRx = fix32Bytes(r.affineXCoord.toBigInteger().toByteArray())
        return computedRx.contentEquals(rx)
    }

    /**
     * Derives ECDH shared secret (32 bytes) between a local private key and a remote x-only public key.
     */
    fun getSharedSecret(privateKey: ByteArray, remotePublicKey: ByteArray): ByteArray {
        val privInt = BigInteger(1, privateKey)
        val remotePoint = liftX(remotePublicKey) ?: throw IllegalArgumentException("Invalid remote public key")
        val sharedPoint = remotePoint.multiply(privInt).normalize()
        val xBytes = sharedPoint.affineXCoord.toBigInteger().toByteArray()
        return fix32Bytes(xBytes)
    }

    private fun liftX(xBytes: ByteArray): ECPoint? {
        val x = BigInteger(1, xBytes)
        val p = curve.curve.field.characteristic
        if (x >= p) return null
        val ySq = (x.modPow(BigInteger.valueOf(3), p).add(BigInteger.valueOf(7))).mod(p)
        var y = ySq.modPow(p.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), p)
        if (y.modPow(BigInteger.valueOf(2), p) != ySq) return null
        if (y.testBit(0)) {
            y = p.subtract(y)
        }
        return curve.curve.createPoint(x, y)
    }

    fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    fun taggedHash(tag: String, msg: ByteArray): ByteArray {
        val tagHash = sha256(tag.toByteArray(Charsets.UTF_8))
        return sha256(tagHash + tagHash + msg)
    }

    private fun fix32Bytes(bytes: ByteArray): ByteArray {
        return when {
            bytes.size == 32 -> bytes
            bytes.size > 32 -> bytes.copyOfRange(bytes.size - 32, bytes.size)
            else -> {
                val res = ByteArray(32)
                System.arraycopy(bytes, 0, res, 32 - bytes.size, bytes.size)
                res
            }
        }
    }
}
