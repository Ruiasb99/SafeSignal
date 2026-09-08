package com.example.emergencybutton

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinManager {
    private const val PREFERENCES_NAME = "emergency_button_preferences"
    private const val PIN_HASH_KEY = "cancellation_pin_hash"
    private const val PIN_SALT_KEY = "cancellation_pin_salt"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun hasPin(context: Context): Boolean =
        preferences(context).contains(PIN_HASH_KEY)

    fun savePin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        preferences(context)
            .edit()
            .putString(PIN_SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(PIN_HASH_KEY, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = preferences(context)
        val saltText = prefs.getString(PIN_SALT_KEY, null) ?: return false
        val expectedText = prefs.getString(PIN_HASH_KEY, null) ?: return false
        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        val expected = Base64.decode(expectedText, Base64.NO_WRAP)
        val actual = hashPin(pin, salt)
        return java.security.MessageDigest.isEqual(expected, actual)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val specification = PBEKeySpec(
            pin.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH_BITS
        )
        return SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(specification)
            .encoded
            .also { specification.clearPassword() }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
