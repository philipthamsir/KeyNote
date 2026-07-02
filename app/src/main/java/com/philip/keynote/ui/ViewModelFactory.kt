package com.philip.keynote.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.philip.keynote.data.local.KeyNoteDatabase
import com.philip.keynote.data.repository.NoteRepository
import com.philip.keynote.data.repository.PasswordRepository
import com.philip.keynote.ui.notes.NoteDetailViewModel
import com.philip.keynote.ui.notes.NotesViewModel
import com.philip.keynote.ui.passwords.PasswordViewModel

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    private val db = KeyNoteDatabase.getDatabase(context)
    private val noteRepository = NoteRepository(db.noteDao())
    private val passwordRepository = PasswordRepository(db.passwordDao())

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(NotesViewModel::class.java) -> {
                NotesViewModel(noteRepository) as T
            }
            modelClass.isAssignableFrom(NoteDetailViewModel::class.java) -> {
                NoteDetailViewModel(noteRepository) as T
            }
            modelClass.isAssignableFrom(PasswordViewModel::class.java) -> {
                PasswordViewModel(passwordRepository, context.applicationContext) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
