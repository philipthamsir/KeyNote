package com.philip.keynote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.philip.keynote.data.local.dao.NoteDao
import com.philip.keynote.data.local.dao.PasswordDao
import com.philip.keynote.data.local.entity.CategoryEntity
import com.philip.keynote.data.local.entity.NoteEntity
import com.philip.keynote.data.local.entity.PasswordEntity
import com.philip.keynote.security.DatabasePassphraseManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [NoteEntity::class, PasswordEntity::class, CategoryEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KeyNoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun passwordDao(): PasswordDao

    companion object {
        @Volatile
        private var INSTANCE: KeyNoteDatabase? = null

        fun getDatabase(context: Context): KeyNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = buildDatabase(context.applicationContext)
                    // Force open the database to trigger key validation
                    instance.openHelper.writableDatabase
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If decryption fails (e.g. due to Keystore reset/passphrase change), recreate the database
                    try {
                        context.deleteDatabase("keynote_encrypted.db")
                    } catch (delEx: Exception) {
                        delEx.printStackTrace()
                    }
                    val instance = buildDatabase(context.applicationContext)
                    INSTANCE = instance
                    instance
                }
            }
        }

        private fun buildDatabase(context: Context): KeyNoteDatabase {
            // 1. Retrieve or generate the database passphrase securely
            val passphraseManager = DatabasePassphraseManager(context)
            val passphrase = passphraseManager.getOrCreatePassphrase()

            // 2. Create the SQLCipher Open Helper Factory
            val factory = SupportOpenHelperFactory(passphrase)

            // 3. Build the database using Room and provide the factory
            return Room.databaseBuilder(
                context,
                KeyNoteDatabase::class.java,
                "keynote_encrypted.db"
            )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
