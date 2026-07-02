package com.philip.keynote.data.backup

import android.content.Context
import com.google.gson.Gson
import com.philip.keynote.data.local.KeyNoteDatabase
import com.philip.keynote.data.local.entity.CategoryEntity
import com.philip.keynote.data.local.entity.NoteEntity
import com.philip.keynote.data.local.entity.PasswordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupManager(private val context: Context) {

    private val db = KeyNoteDatabase.getDatabase(context)
    private val gson = Gson()

    data class BackupData(
        val notes: List<NoteEntity>,
        val categories: List<CategoryEntity>,
        val passwords: List<PasswordEntity>
    )

    /**
     * Exports all database data, encrypts it using AES-256-GCM (key derived from password via PBKDF2),
     * and writes it to the output stream. Runs on Dispatchers.IO context.
     */
    suspend fun exportBackup(password: String, outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Fetch all data
            val notes = db.noteDao().getActiveNotes().first() + 
                        db.noteDao().getArchivedNotes().first() + 
                        db.noteDao().getTrashNotes().first()
            val categories = db.passwordDao().getAllCategories().first()
            
            // For passwords, we get them by category
            val passwords = mutableListOf<PasswordEntity>()
            categories.forEach { category ->
                passwords.addAll(db.passwordDao().getPasswordsByCategory(category.id).first())
            }

            val backupObj = BackupData(notes, categories, passwords)
            val jsonString = gson.toJson(backupObj)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            // 2. Derive Key and Encrypt
            val random = SecureRandom()
            val salt = ByteArray(16)
            random.nextBytes(salt)

            val iv = ByteArray(12) // 96 bits standard for GCM
            random.nextBytes(iv)

            val secretKey = deriveKey(password, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv) // 128 bit auth tag
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            val ciphertext = cipher.doFinal(jsonBytes)

            // 3. Write Salt, IV, and Ciphertext
            outputStream.use { out ->
                out.write(salt)
                out.write(iv)
                out.write(ciphertext)
            }
        }
    }

    /**
     * Reads the input stream, decrypts the content, parses it, and restores it in the database.
     * Runs on Dispatchers.IO context.
     */
    suspend fun importBackup(password: String, inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Read Salt, IV, and Ciphertext
            inputStream.use { stream ->
                val salt = ByteArray(16)
                if (stream.read(salt) != 16) throw IllegalArgumentException("Invalid backup file: Salt missing")

                val iv = ByteArray(12)
                if (stream.read(iv) != 12) throw IllegalArgumentException("Invalid backup file: IV missing")

                val ciphertext = stream.readBytes()
                if (ciphertext.isEmpty()) throw IllegalArgumentException("Invalid backup file: Data empty")

                // 2. Decrypt
                val secretKey = deriveKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                val decryptedBytes = cipher.doFinal(ciphertext)
                val jsonString = String(decryptedBytes, Charsets.UTF_8)

                // 3. Restore to Database
                val backupData = gson.fromJson(jsonString, BackupData::class.java)

                restoreDatabase(backupData)
            }
        }
    }

    private suspend fun restoreDatabase(backupData: BackupData) = withContext(Dispatchers.IO) {
        // Clear all current tables
        // Note: CASCADE foreign key will clear passwords automatically when categories are cleared.
        // We'll clear notes and categories first.
        val noteDao = db.noteDao()
        val passwordDao = db.passwordDao()

        // Deleting active notes
        noteDao.getActiveNotes().first().forEach { noteDao.deleteNote(it) }
        noteDao.getArchivedNotes().first().forEach { noteDao.deleteNote(it) }
        noteDao.getTrashNotes().first().forEach { noteDao.deleteNote(it) }

        // Deleting categories (cascade deletes passwords)
        passwordDao.getAllCategories().first().forEach { passwordDao.deleteCategory(it) }

        // Insert new data
        backupData.notes.forEach { noteDao.insertNote(it) }
        
        // We must re-insert categories first to preserve relationships,
        // but if IDs change or are preserved, Room will auto-generate new ones if we insert them with 0.
        // Since we want to preserve exact category-password linkages, let's insert categories keeping their original IDs,
        // and do the same for passwords.
        // Room insert with OnConflictStrategy.REPLACE will preserve IDs if specified!
        backupData.categories.forEach { passwordDao.insertCategory(it) }
        backupData.passwords.forEach { passwordDao.insertPassword(it) }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val iterationCount = 10000
        val keyLength = 256
        val spec = PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
