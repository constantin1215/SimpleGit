package simplegit.hash

import java.security.MessageDigest

class HashGenerator {
    companion object {
        fun generateSHA1(seed : String) : String {
            return MessageDigest.getInstance("SHA-1")
                .digest(seed.toByteArray(Charsets.UTF_8))
                .joinToString("") {
                    byte -> "%02x".format(byte)
                }
        }
    }
}