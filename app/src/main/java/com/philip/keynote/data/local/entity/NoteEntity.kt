package com.philip.keynote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["isArchived", "isTrash", "updatedAt"]),
        Index(value = ["isTrash", "updatedAt"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val content: String,
    val backgroundColor: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val isTrash: Boolean = false,
    val isPinned: Boolean = false,
    val noteType: String = "TEXT", // TEXT, CHECKLIST, TABLE, IMAGE
    val isLocked: Boolean = false
)
