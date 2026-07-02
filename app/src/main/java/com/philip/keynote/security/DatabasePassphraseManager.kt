package com.philip.keynote.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.SecureRandom

class DatabasePassphraseManager(private val context: Context) {

    companion object {
        private const val PREFS_FILE = "keynote_secure_prefs"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val PASSPHRASE_LENGTH_BYTES = 32 // 256 bits
    }

    /**
     * Retrieves the existing database passphrase, or generates and saves a new one.
     * The passphrase is encrypted and stored in Android Keystore-backed SharedPreferences.
     */
    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        val sharedPreferences = try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_FILE,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize EncryptedSharedPreferences securely", e)
        }

        val storedPassphraseBase64 = sharedPreferences.getString(KEY_DB_PASSPHRASE, null)
        return if (storedPassphraseBase64 != null) {
            Base64.decode(storedPassphraseBase64, Base64.DEFAULT)
        } else {
            val newPassphrase = generateRandomPassphrase()
            val newPassphraseBase64 = Base64.encodeToString(newPassphrase, Base64.DEFAULT)
            sharedPreferences.edit()
                .putString(KEY_DB_PASSPHRASE, newPassphraseBase64)
                .apply()
            newPassphrase
        }
    }

    private fun generateRandomPassphrase(): ByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(PASSPHRASE_LENGTH_BYTES)
        random.nextBytes(bytes)
        return bytes
    }
}
