package com.philip.keynote.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.philip.keynote.data.settings.SettingsManager
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settingsManager = SettingsManager(context)

        // 1. Check if online backup is enabled
        if (!settingsManager.isOnlineBackupEnabled()) {
            cancelAutoBackup(context)
            return Result.success()
        }

        // 2. Get last signed in Google account
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val driveScope = Scope("https://www.googleapis.com/auth/drive.appdata")
        if (account == null || !GoogleSignIn.hasPermissions(account, driveScope)) {
            return Result.success()
        }

        val googleAccount = account.account
        if (googleAccount == null) {
            return Result.success()
        }

        try {
            // 3. Export database backup using unique password
            val backupManager = BackupManager(context)
            val outputStream = ByteArrayOutputStream()
            val backupKeyManager = com.philip.keynote.security.BackupKeyManager(context)
            val backupPass = backupKeyManager.getOrCreateBackupKey()
            val exportResult = backupManager.exportBackup(backupPass, outputStream)

            if (exportResult.isSuccess) {
                val backupBytes = outputStream.toByteArray()
                // 4. Upload to Google Drive
                val googleDriveBackupHelper = GoogleDriveBackupHelper(context)
                val uploadResult = googleDriveBackupHelper.uploadBackup(googleAccount, backupBytes)

                if (uploadResult.isSuccess) {
                    val now = System.currentTimeMillis()
                    settingsManager.setLastOnlineBackupTime(now)
                    return Result.success()
                } else {
                    return Result.retry()
                }
            } else {
                return Result.failure()
            }
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "google_drive_auto_backup_work"

        fun scheduleAutoBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val backupWorkRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing work to not reset the timer
                backupWorkRequest
            )
        }

        fun cancelAutoBackup(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
