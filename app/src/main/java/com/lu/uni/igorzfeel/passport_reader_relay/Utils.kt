package com.lu.uni.igorzfeel.passport_reader_relay

class Utils {
    companion object {
        private val hexArray = "0123456789ABCDEF".toCharArray()

        fun toHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2) // Each byte has two hex characters (nibbles)
            var v: Int
            for (j in bytes.indices) {
                v = bytes[j].toInt()  and 0xFF // Cast bytes[j] to int, treating as unsigned value
                hexChars[j * 2] = hexArray[v ushr 4] // Select hex character from upper nibble
                hexChars[j * 2 + 1] =
                    hexArray[v and 0x0F] // Select hex character from lower nibble
            }
            return String(hexChars)
        }

        @Throws(IllegalArgumentException::class)
        fun hexStringToByteArray(s: String): ByteArray {
            val len = s.length
            require(len % 2 != 1) { "Hex string must have even number of characters" }
            val data = ByteArray(len / 2) // Allocate 1 byte per 2 hex characters
            var i = 0
            while (i < len) {
                // Convert each character into a integer (base-16), then bit-shift into place
                data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                        + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }
    }
}