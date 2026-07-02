package com.philip.keynote.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.philip.keynote.data.local.entity.NoteEntity
import com.philip.keynote.data.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    val activeNotes: StateFlow<List<NoteEntity>> = repository.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashNotes: StateFlow<List<NoteEntity>> = repository.trashNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isTrash = true, isArchived = false, updatedAt = System.currentTimeMillis()))
        }
    }

    fun moveToArchive(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isArchived = true, isTrash = false, updatedAt = System.currentTimeMillis()))
        }
    }

    fun restoreFromTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isTrash = false, isArchived = false, updatedAt = System.currentTimeMillis()))
        }
    }

    fun restoreFromArchive(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isArchived = false, isTrash = false, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun duplicateNote(note: NoteEntity) {
        viewModelScope.launch {
            val time = System.currentTimeMillis()
            val duplicate = note.copy(
                id = 0L,
                title = "${note.title} (Salinan)",
                createdAt = time,
                updatedAt = time
            )
            repository.insertNote(duplicate)
        }
    }

    fun updateNoteColor(note: NoteEntity, color: Int) {
        viewModelScope.launch {
            repository.updateNote(note.copy(backgroundColor = color, updatedAt = System.currentTimeMillis()))
        }
    }

    fun toggleNoteLock(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isLocked = !note.isLocked, updatedAt = System.currentTimeMillis()))
        }
    }
}
