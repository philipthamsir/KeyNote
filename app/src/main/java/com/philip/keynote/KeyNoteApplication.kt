package com.philip.keynote

import android.app.Application
class KeyNoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
    }
}
