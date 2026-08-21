package com.nostrange.app.security

import java.io.ByteArrayOutputStream

/**
 * BIP-173 Bech32 encoding and decoding implementation for Nostr npub / nsec keys.
 */
object Bech32 {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    fun encode(hrp: String, data: ByteArray): String {
        val converted = convertBits(data, 8, 5, true)
        val checksum = createChecksum(hrp, converted)
        val combined = converted + checksum
        val sb = StringBuilder(hrp).append("1")
        for (b in combined) {
            sb.append(CHARSET[b.toInt() and 0xff])
        }
        return sb.toString()
    }

    fun decode(bech32: String): Pair<String, ByteArray> {
        val pos = bech32.lastIndexOf('1')
        if (pos < 1 || pos + 7 > bech32.length) {
            throw IllegalArgumentException("Invalid bech32 string")
        }
        val hrp = bech32.substring(0, pos).lowercase()
        val data = ByteArray(bech32.length - pos - 1)
        for (i in 0 until data.size) {
            val c = bech32[pos + 1 + i].lowercaseChar()
            val idx = CHARSET.indexOf(c)
            if (idx == -1) throw IllegalArgumentException("Invalid bech32 character: $c")
            data[i] = idx.toByte()
        }
        if (!verifyChecksum(hrp, data)) {
            throw IllegalArgumentException("Invalid checksum")
        }
        val converted = convertBits(data.copyOfRange(0, data.size - 6), 5, 8, false)
        return Pair(hrp, converted)
    }

    fun pubkeyToNpub(pubkeyHex: String): String {
        return encode("npub", hexToBytes(pubkeyHex))
    }

    fun npubToPubkey(npub: String): String {
        val (hrp, bytes) = decode(npub)
        if (hrp != "npub") throw IllegalArgumentException("Expected hrp npub but got $hrp")
        return bytesToHex(bytes)
    }

    fun privkeyToNsec(privkeyHex: String): String {
        return encode("nsec", hexToBytes(privkeyHex))
    }

    fun nsecToPrivkey(nsec: String): String {
        val (hrp, bytes) = decode(nsec)
        if (hrp != "nsec") throw IllegalArgumentException("Expected hrp nsec but got $hrp")
        return bytesToHex(bytes)
    }

    private fun polymod(values: ByteArray): Int {
        var chk = 1
        for (b in values) {
            val top = chk ushr 25
            chk = (chk and 0x1ffffff shl 5) xor (b.toInt() and 0xff)
            if ((top and 1) != 0) chk = chk xor 0x3b2d387f
            if ((top and 2) != 0) chk = chk xor 0x165667b1
            if ((top and 4) != 0) chk = chk xor 0x39da942a
            if ((top and 8) != 0) chk = chk xor 0x0fbc0f6b
            if ((top and 16) != 0) chk = chk xor 0x240e1a09
        }
        return chk
    }

    private fun hrpExpand(hrp: String): ByteArray {
        val result = ByteArray(hrp.length * 2 + 1)
        for (i in hrp.indices) {
            result[i] = (hrp[i].code ushr 5).toByte()
            result[i + hrp.length + 1] = (hrp[i].code and 31).toByte()
        }
        result[hrp.length] = 0
        return result
    }

    private fun verifyChecksum(hrp: String, values: ByteArray): Boolean {
        val expanded = hrpExpand(hrp)
        val combined = expanded + values
        return polymod(combined) == 1
    }

    private fun createChecksum(hrp: String, values: ByteArray): ByteArray {
        val expanded = hrpExpand(hrp)
        val zeroPad = ByteArray(6)
        val combined = expanded + values + zeroPad
        val poly = polymod(combined) xor 1
        val result = ByteArray(6)
        for (i in 0 until 6) {
            result[i] = ((poly ushr (5 * (5 - i))) and 31).toByte()
        }
        return result
    }

    private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
        var acc = 0
        var bits = 0
        val out = ByteArrayOutputStream()
        val maxv = (1 shl toBits) - 1
        val maxAcc = (1 shl (fromBits + toBits - 1)) - 1
        for (b in data) {
            val value = b.toInt() and 0xff
            acc = ((acc shl fromBits) or value) and maxAcc
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                out.write((acc ushr bits) and maxv)
            }
        }
        if (pad) {
            if (bits > 0) {
                out.write((acc shl (toBits - bits)) and maxv)
            }
        } else if (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0) {
            throw IllegalArgumentException("Could not convert bits without padding")
        }
        return out.toByteArray()
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xff
            result.append(hexChars[i ushr 4])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = if (hex.length % 2 != 0) "0$hex" else hex
        val len = cleanHex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4) + Character.digit(cleanHex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
