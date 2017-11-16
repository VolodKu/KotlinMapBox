package com.liveroads.secrets

import android.util.Base64
import org.json.JSONObject

/**
 * Decrypt and expose the application's "secret" data.
 *
 * This "secret" data is stored in `secrets.json` and compiled into `SecretData.java` by running
 *
 *     ./gradlew :lib:secrets:generateSecretData
 *
 * The encryption scheme is a very simple one-time pad encryption scheme.  The "encryption key" is actually stored right
 * alongside the encrypted data, and therefore any mildly-sophisticated hacker could easily decrypt it; however, this
 * encryption, albeit easy to crack, at least prevents storing secret data, such as API keys, in plain text in the APK
 * file.  Without this encryption, even an untechnical person could easily extract this secret information with any one
 * of many universally-available APK decompilation tools.
 */
private class Secrets {

    private var initialized: Boolean = false
    lateinit var MAPBOX_API_KEY: String
    lateinit var MAPZEN_API_KEY: String

    fun ensureInitialized(): Secrets {
        if (!initialized) {
            initialize()
            initialized = true
        }
        return this
    }

    fun initialize() {
        val json = decrypt()
        val data = JSONObject(json)
        MAPBOX_API_KEY = data.getString("MAPBOX_API_KEY")
        MAPZEN_API_KEY = data.getString("MAPZEN_API_KEY")
    }

    private fun decrypt(): String {
        val key = base64Decode(ENCRYPTION_KEY)
        val cipherText = base64Decode(DATA)
        val plainText = ByteArray(cipherText.size) { i ->
            (cipherText[i] - key[i]).toByte()
        }
        return java.lang.String(plainText, "utf8").toString()
    }

    private fun base64Decode(s: String): ByteArray {
        return Base64.decode(s, Base64.DEFAULT)
    }

}

private val secrets = Secrets()

val MAPBOX_API_KEY: String
    get() = secrets.ensureInitialized().MAPBOX_API_KEY
val MAPZEN_API_KEY: String
    get() = secrets.ensureInitialized().MAPZEN_API_KEY
