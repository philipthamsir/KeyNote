package com.philip.keynote

import android.app.Application
import com.philip.keynote.data.backup.BackupWorker
import com.philip.keynote.data.settings.SettingsManager

class KeyNoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        
        val settingsManager = SettingsManager(this)
        if (settingsManager.isOnlineBackupEnabled()) {
            BackupWorker.scheduleAutoBackup(this)
        }
    }
}
