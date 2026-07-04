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

class GoogleDriveAuthException(message: String) : Exception(message)

class GoogleDriveBackupHelper(private val context: Context) {

    private val scope = "oauth2:https://www.googleapis.com/auth/drive.appdata"
    private val fileName = "keynote_backup.json"

    private suspend fun getAccessToken(account: Account): String = withContext(Dispatchers.IO) {
        GoogleAuthUtil.getToken(context, account, scope)
    }

    suspend fun uploadBackup(account: Account, backupBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            var token: String? = null
            try {
                token = getAccessToken(account)
                // 1. Search if the file already exists
                val existingFileId = findFileId(token)
                if (existingFileId != null) {
                    // Update file content
                    val success = updateFile(token, existingFileId, backupBytes)
                    if (!success) throw Exception("Gagal memperbarui file di Google Drive.")
                } else {
                    // Create new file metadata
                    val newFileId = createFileMetadata(token) ?: throw Exception("Gagal membuat metadata file di Google Drive.")
                    val success = updateFile(token, newFileId, backupBytes)
                    if (!success) throw Exception("Gagal menulis isi file di Google Drive.")
                }
            } catch (e: Exception) {
                Log.e("GDriveBackup", "Exception in uploadBackup", e)
                if (token != null) {
                    clearCachedToken(token)
                }
                if (e is GoogleDriveAuthException) {
                    throw e
                } else {
                    val msg = e.message ?: ""
                    if (msg.contains("permission", ignoreCase = true) || msg.contains("auth", ignoreCase = true) || msg.contains("denied", ignoreCase = true)) {
                        throw GoogleDriveAuthException("Izin Google Drive ditolak: $msg")
                    }
                    throw e
                }
            }
        }
    }

    suspend fun downloadBackup(account: Account): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            var token: String? = null
            try {
                token = getAccessToken(account)
                val fileId = findFileId(token) ?: throw Exception("File cadangan tidak ditemukan di Google Drive.")
                downloadFileContent(token, fileId) ?: throw Exception("Gagal mengunduh file cadangan dari Google Drive.")
            } catch (e: Exception) {
                Log.e("GDriveBackup", "Exception in downloadBackup", e)
                if (token != null) {
                    clearCachedToken(token)
                }
                if (e is GoogleDriveAuthException) {
                    throw e
                } else {
                    val msg = e.message ?: ""
                    if (msg.contains("permission", ignoreCase = true) || msg.contains("auth", ignoreCase = true) || msg.contains("denied", ignoreCase = true)) {
                        throw GoogleDriveAuthException("Izin Google Drive ditolak: $msg")
                    }
                    throw e
                }
            }
        }
    }

    private fun clearCachedToken(token: String) {
        try {
            GoogleAuthUtil.clearToken(context, token)
            Log.d("GDriveBackup", "Cached token successfully cleared")
        } catch (e: Exception) {
            Log.e("GDriveBackup", "Failed to clear token cache", e)
        }
    }

    private fun parseErrorMessage(jsonStr: String?): String? {
        if (jsonStr.isNullOrEmpty()) return null
        return try {
            val json = JSONObject(jsonStr)
            val error = json.getJSONObject("error")
            error.getString("message")
        } catch (e: Exception) {
            jsonStr
        }
    }

    private fun findFileId(token: String): String? {
        val url = URL("https://www.googleapis.com/drive/v3/files?q=name%3D%27$fileName%27+and+trashed%3Dfalse&spaces=appDataFolder")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        
        val responseCode = conn.responseCode
        if (responseCode == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val files = json.getJSONArray("files")
            if (files.length() > 0) {
                return files.getJSONObject(0).getString("id")
            }
        } else if (responseCode == 401 || responseCode == 403) {
            val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e("GDriveBackup", "Auth Error $responseCode: $errorResponse")
            val parsedMsg = parseErrorMessage(errorResponse) ?: "Akses ditolak (HTTP $responseCode)"
            throw GoogleDriveAuthException(parsedMsg)
        } else {
            val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e("GDriveBackup", "HTTP Error $responseCode: $errorResponse")
            val parsedMsg = parseErrorMessage(errorResponse) ?: "HTTP Error $responseCode"
            throw Exception(parsedMsg)
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

        val responseCode = conn.responseCode
        if (responseCode == 200 || responseCode == 201) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            return json.getString("id")
        } else if (responseCode == 401 || responseCode == 403) {
            val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e("GDriveBackup", "Auth Error $responseCode: $errorResponse")
            val parsedMsg = parseErrorMessage(errorResponse) ?: "Akses ditolak (HTTP $responseCode)"
            throw GoogleDriveAuthException(parsedMsg)
        } else {
            val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e("GDriveBackup", "HTTP Error $responseCode: $errorResponse")
            val parsedMsg = parseErrorMessage(errorResponse) ?: "HTTP Error $responseCode"
            throw Exception(parsedMsg)
        }
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

        val responseCode = conn.responseCode
        if (responseCode == 200 || responseCode == 204) {
            return true
        } else if (responseCode == 401 || responseCode == 403) {
            val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e("GDriveBackup", "Auth Error $responseCode: $errorResponse")
            val parsedMsg = parseErrorMessage(errorResponse) ?: "Akses ditolak (HTTP $responseCode)"
            throw GoogleDriveAuthException(parsedMsg)
        } else {
            val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e("GDriveBackup", "HTTP Error $responseCode: $errorResponse")
            val parsedMsg = parseErrorMessage(errorResponse) ?: "HTTP Error $responseCode"
            throw Exception(parsedMsg)
        }
    }

    private fun downloadFileContent(token: String, fileId: String): ByteArray? {
        val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")

        val responseCode = conn.responseCode
        if (responseCode == 200) {
            return conn.inputStream.use { it.readBytes() }
        } else if (responseCode == 401 || responseCode == 403) {
            val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e("GDriveBackup", "Auth Error $responseCode: $errorResponse")
            val parsedMsg = parseErrorMessage(errorResponse) ?: "Akses ditolak (HTTP $responseCode)"
            throw GoogleDriveAuthException(parsedMsg)
        } else {
            val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e("GDriveBackup", "HTTP Error $responseCode: $errorResponse")
            val parsedMsg = parseErrorMessage(errorResponse) ?: "HTTP Error $responseCode"
            throw Exception(parsedMsg)
        }
    }
}
