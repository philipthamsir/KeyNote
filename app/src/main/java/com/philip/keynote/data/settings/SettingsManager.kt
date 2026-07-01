package com.philip.keynote.data.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("keynote_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
    }

    fun getNotesViewMode(): String {
        return prefs.getString(KEY_VIEW_MODE, "LIST") ?: "LIST"
    }

    fun setNotesViewMode(mode: String) {
        prefs.edit().putString(KEY_VIEW_MODE, mode).apply()
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "in") ?: "in"
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun getAppTheme(): String {
        return prefs.getString(KEY_THEME, "SYSTEM") ?: "SYSTEM"
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun isAutoSaveEnabled(): Boolean {
        return prefs.getBoolean("auto_save_enabled", true)
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save_enabled", enabled).apply()
    }

    fun getAutoSaveInterval(): Long {
        return prefs.getLong("auto_save_interval", 180000L) // Default 3 minutes
    }

    fun setAutoSaveInterval(intervalMs: Long) {
        prefs.edit().putLong("auto_save_interval", intervalMs).apply()
    }

    fun isGoogleSignedIn(): Boolean {
        return prefs.getBoolean("google_signed_in", false)
    }

    fun setGoogleSignedIn(signedIn: Boolean, email: String? = null, name: String? = null) {
        prefs.edit().apply {
            putBoolean("google_signed_in", signedIn)
            putString("google_email", email)
            putString("google_name", name)
            if (!signedIn) {
                putBoolean("online_backup_enabled", false)
            }
        }.apply()
    }

    fun getGoogleEmail(): String {
        return prefs.getString("google_email", "") ?: ""
    }

    fun getGoogleName(): String {
        return prefs.getString("google_name", "") ?: ""
    }

    fun isOnlineBackupEnabled(): Boolean {
        return prefs.getBoolean("online_backup_enabled", false)
    }

    fun setOnlineBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("online_backup_enabled", enabled).apply()
    }

    fun getLastOnlineBackupTime(): Long {
        return prefs.getLong("last_online_backup_time", 0L)
    }

    fun setLastOnlineBackupTime(time: Long) {
        prefs.edit().putLong("last_online_backup_time", time).apply()
    }
}
