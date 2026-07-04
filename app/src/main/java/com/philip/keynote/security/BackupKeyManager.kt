package com.philip.keynote.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.SecureRandom

class BackupKeyManager(private val context: Context) {

    companion object {
        private const val PREFS_FILE = "keynote_backup_key_prefs"
        private const val KEY_BACKUP_PASSWORD = "backup_encryption_key"
        private const val KEY_LENGTH = 32
    }

    fun getOrCreateBackupKey(): String {
        val prefs = createEncryptedSharedPreferences()
        val existing = prefs.getString(KEY_BACKUP_PASSWORD, null)
        if (existing != null) return existing

        val newKey = generateRandomKey()
        prefs.edit().putString(KEY_BACKUP_PASSWORD, newKey).apply()
        return newKey
    }

    private fun createEncryptedSharedPreferences(): android.content.SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun generateRandomKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..KEY_LENGTH).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}
