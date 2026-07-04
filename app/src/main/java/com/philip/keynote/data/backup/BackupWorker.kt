package com.philip.keynote.data.backup

import android.content.Context
import android.util.Log
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
        Log.d("BackupWorker", "doWork: Starting background auto backup")
        val context = applicationContext
        val settingsManager = SettingsManager(context)

        // 1. Check if online backup is enabled
        if (!settingsManager.isOnlineBackupEnabled()) {
            Log.d("BackupWorker", "doWork: Online backup is disabled, skipping")
            cancelAutoBackup(context)
            return Result.success()
        }

        // 2. Get last signed in Google account
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val driveScope = Scope("https://www.googleapis.com/auth/drive.appdata")
        if (account == null || !GoogleSignIn.hasPermissions(account, driveScope)) {
            Log.w("BackupWorker", "doWork: Google account has no Drive permissions, skipping")
            return Result.success()
        }

        val googleAccount = account.account
        if (googleAccount == null) {
            Log.w("BackupWorker", "doWork: Google account is null, skipping")
            return Result.success()
        }

        try {
            // 3. Export database backup using simulated password
            val backupManager = BackupManager(context)
            val outputStream = ByteArrayOutputStream()
            val simulatedPass = "google_drive_secure_backup"
            val exportResult = backupManager.exportBackup(simulatedPass, outputStream)

            if (exportResult.isSuccess) {
                val backupBytes = outputStream.toByteArray()
                // 4. Upload to Google Drive
                val googleDriveBackupHelper = GoogleDriveBackupHelper(context)
                val uploadResult = googleDriveBackupHelper.uploadBackup(googleAccount, backupBytes)

                if (uploadResult.isSuccess) {
                    val now = System.currentTimeMillis()
                    settingsManager.setLastOnlineBackupTime(now)
                    Log.i("BackupWorker", "doWork: Auto backup succeeded!")
                    return Result.success()
                } else {
                    val error = uploadResult.exceptionOrNull()
                    Log.e("BackupWorker", "doWork: GDrive upload failed", error)
                    return Result.retry()
                }
            } else {
                val error = exportResult.exceptionOrNull()
                Log.e("BackupWorker", "doWork: Database export failed", error)
                return Result.failure()
            }
        } catch (e: Exception) {
            Log.e("BackupWorker", "doWork: Unexpected exception", e)
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
            Log.d("BackupWorker", "scheduleAutoBackup: Scheduled daily backup")
        }

        fun cancelAutoBackup(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d("BackupWorker", "cancelAutoBackup: Cancelled scheduled backup")
        }
    }
}
