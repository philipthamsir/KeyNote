package com.philip.keynote.data.backup

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

class GoogleDriveBackupHelper(private val context: Context) {

    private val scope = "oauth2:https://www.googleapis.com/auth/drive.appdata"
    private val fileName = "keynote_backup.json"

    private suspend fun getAccessToken(account: Account): String? = withContext(Dispatchers.IO) {
        try {
            GoogleAuthUtil.getToken(context, account, scope)
        } catch (e: Exception) {
            Log.e("GDriveBackup", "Failed to get access token", e)
            null
        }
    }

    suspend fun uploadBackup(account: Account, backupBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val token = getAccessToken(account) ?: return@withContext false
        try {
            // 1. Search if the file already exists
            val existingFileId = findFileId(token)
            if (existingFileId != null) {
                // Update file content
                updateFile(token, existingFileId, backupBytes)
            } else {
                // Create new file metadata
                val newFileId = createFileMetadata(token)
                if (newFileId != null) {
                    updateFile(token, newFileId, backupBytes)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("GDriveBackup", "Upload failed", e)
            false
        }
    }

    suspend fun downloadBackup(account: Account): ByteArray? = withContext(Dispatchers.IO) {
        val token = getAccessToken(account) ?: return@withContext null
        try {
            val fileId = findFileId(token) ?: return@withContext null
            downloadFileContent(token, fileId)
        } catch (e: Exception) {
            Log.e("GDriveBackup", "Download failed", e)
            null
        }
    }

    private fun findFileId(token: String): String? {
        val url = URL("https://www.googleapis.com/drive/v3/files?q=name%3D%27$fileName%27+and+trashed%3Dfalse&spaces=appDataFolder")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        
        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val files = json.getJSONArray("files")
            if (files.length() > 0) {
                return files.getJSONObject(0).getString("id")
            }
        }
        return null
    }

    private fun createFileMetadata(token: String): String? {
        val url = URL("https://www.googleapis.com/drive/v3/files")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val metadata = JSONObject().apply {
            put("name", fileName)
            put("parents", JSONArray().put("appDataFolder"))
        }

        conn.outputStream.use { out ->
            out.write(metadata.toString().toByteArray(Charsets.UTF_8))
        }

        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            return json.getString("id")
        }
        return null
    }

    private fun updateFile(token: String, fileId: String, content: ByteArray): Boolean {
        val url = URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "PATCH"
        } catch (e: Exception) {
            conn.requestMethod = "POST"
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
        }
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/octet-stream")
        conn.doOutput = true

        conn.outputStream.use { out ->
            out.write(content)
        }

        return conn.responseCode == 200
    }

    private fun downloadFileContent(token: String, fileId: String): ByteArray? {
        val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")

        if (conn.responseCode == 200) {
            return conn.inputStream.use { it.readBytes() }
        }
        return null
    }
}
