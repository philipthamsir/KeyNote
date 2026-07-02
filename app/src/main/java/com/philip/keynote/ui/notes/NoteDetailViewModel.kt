package com.philip.keynote.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.philip.keynote.data.local.BlockType
import com.philip.keynote.data.local.ChecklistItem
import com.philip.keynote.data.local.NoteBlock
import com.philip.keynote.data.local.TableData
import com.philip.keynote.data.local.entity.NoteEntity
import com.philip.keynote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NoteDetailViewModel(private val repository: NoteRepository) : ViewModel() {

    private val gson = Gson()

    private val _noteId = MutableStateFlow<Long?>(null)
    val noteId: StateFlow<Long?> = _noteId.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _backgroundColor = MutableStateFlow(0xFFFFFFFF.toInt())
    val backgroundColor: StateFlow<Int> = _backgroundColor.asStateFlow()

    private val _blocks = MutableStateFlow<List<NoteBlock>>(emptyList())
    val blocks: StateFlow<List<NoteBlock>> = _blocks.asStateFlow()

    fun loadNote(id: Long?) {
        if (id == null || id == 0L) {
            _noteId.value = null
            _title.value = ""
            _backgroundColor.value = 0xFFFFFFFF.toInt()
            _blocks.value = listOf(NoteBlock(UUID.randomUUID().toString(), BlockType.TEXT))
            return
        }
        _noteId.value = id
        viewModelScope.launch {
            repository.getNoteById(id)?.let { note ->
                _title.value = note.title
                _backgroundColor.value = note.backgroundColor
                
                // Migrate legacy note types to multi-block on the fly
                val migratedBlocks = when (note.noteType) {
                    "MERGED" -> {
                        try {
                            val type = object : TypeToken<List<NoteBlock>>() {}.type
                            gson.fromJson<List<NoteBlock>>(note.content, type) ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                    "TEXT" -> {
                        listOf(NoteBlock(UUID.randomUUID().toString(), BlockType.TEXT, textContent = note.content))
                    }
                    "CHECKLIST" -> {
                        val items = try {
                            val type = object : TypeToken<List<ChecklistItem>>() {}.type
                            gson.fromJson<List<ChecklistItem>>(note.content, type) ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                        listOf(NoteBlock(UUID.randomUUID().toString(), BlockType.CHECKLIST, checklistItems = items))
                    }
                    "TABLE" -> {
                        val table = try {
                            gson.fromJson(note.content, TableData::class.java) ?: TableData()
                        } catch (e: Exception) {
                            TableData()
                        }
                        listOf(NoteBlock(UUID.randomUUID().toString(), BlockType.TABLE, tableData = table))
                    }
                    else -> emptyList()
                }

                _blocks.value = migratedBlocks.ifEmpty {
                    listOf(NoteBlock(UUID.randomUUID().toString(), BlockType.TEXT))
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun updateBackgroundColor(color: Int) {
        _backgroundColor.value = color
    }

    // Dynamic blocks operations
    fun addBlock(type: BlockType) {
        val newBlock = when (type) {
            BlockType.TEXT -> NoteBlock(UUID.randomUUID().toString(), type)
            BlockType.CHECKLIST -> NoteBlock(UUID.randomUUID().toString(), type, checklistItems = listOf(ChecklistItem(UUID.randomUUID().toString(), "", false)))
            BlockType.TABLE -> NoteBlock(
                UUID.randomUUID().toString(), type,
                tableData = TableData(headers = listOf("Kolom 1", "Kolom 2"), rows = listOf(listOf("", "")))
            )
            BlockType.IMAGE -> NoteBlock(UUID.randomUUID().toString(), type)
        }
        _blocks.value = _blocks.value + newBlock
    }

    fun deleteBlock(blockId: String) {
        _blocks.value = _blocks.value.filter { it.id != blockId }
    }

    fun updateTextBlockContent(blockId: String, text: String) {
        _blocks.value = _blocks.value.map {
            if (it.id == blockId) it.copy(textContent = text) else it
        }
    }

    // Formatting operations
    fun updateBlockFormatting(
        blockId: String,
        fontSize: Float? = null,
        fontColor: Int? = null,
        fontStyle: String? = null,
        textAlignment: String? = null
    ) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                block.copy(
                    fontSize = fontSize ?: block.fontSize,
                    fontColor = fontColor ?: block.fontColor,
                    fontStyle = fontStyle ?: block.fontStyle,
                    textAlignment = textAlignment ?: block.textAlignment
                )
            } else block
        }
    }

    // Checklist operations within blocks
    fun addChecklistItem(blockId: String, text: String = "") {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                val newItem = ChecklistItem(UUID.randomUUID().toString(), text, false)
                block.copy(checklistItems = block.checklistItems + newItem)
            } else block
        }
    }

    fun updateChecklistItem(blockId: String, itemId: String, text: String, isChecked: Boolean) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                val updatedItems = block.checklistItems.map { item ->
                    if (item.id == itemId) item.copy(text = text, isChecked = isChecked) else item
                }
                block.copy(checklistItems = updatedItems)
            } else block
        }
    }

    fun removeChecklistItem(blockId: String, itemId: String) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                block.copy(checklistItems = block.checklistItems.filter { it.id != itemId })
            } else block
        }
    }

    fun moveChecklistItemUp(blockId: String, itemId: String) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                val items = block.checklistItems.toMutableList()
                val index = items.indexOfFirst { it.id == itemId }
                if (index > 0) {
                    val temp = items[index]
                    items[index] = items[index - 1]
                    items[index - 1] = temp
                }
                block.copy(checklistItems = items)
            } else block
        }
    }

    fun moveChecklistItemDown(blockId: String, itemId: String) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                val items = block.checklistItems.toMutableList()
                val index = items.indexOfFirst { it.id == itemId }
                if (index in 0 until items.size - 1) {
                    val temp = items[index]
                    items[index] = items[index + 1]
                    items[index + 1] = temp
                }
                block.copy(checklistItems = items)
            } else block
        }
    }

    // Table operations within blocks
    fun addTableColumn(blockId: String) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                val current = block.tableData
                val newHeaders = current.headers + "Kolom ${current.headers.size + 1}"
                val newRows = current.rows.map { it + "" }
                block.copy(tableData = TableData(newHeaders, newRows))
            } else block
        }
    }

    fun addTableRow(blockId: String) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                val current = block.tableData
                val newRow = List(current.headers.size) { "" }
                block.copy(tableData = TableData(current.headers, current.rows + listOf(newRow)))
            } else block
        }
    }

    fun updateTableCell(blockId: String, rowIndex: Int, colIndex: Int, value: String) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                val current = block.tableData
                val newRows = current.rows.mapIndexed { rIdx, row ->
                    if (rIdx == rowIndex) {
                        row.mapIndexed { cIdx, cell -> if (cIdx == colIndex) value else cell }
                    } else {
                        row
                    }
                }
                block.copy(tableData = TableData(current.headers, newRows))
            } else block
        }
    }

    fun removeTableRow(blockId: String, rowIndex: Int) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                val current = block.tableData
                if (current.rows.size > 1) {
                    block.copy(tableData = TableData(current.headers, current.rows.filterIndexed { index, _ -> index != rowIndex }))
                } else block
            } else block
        }
    }

    // Image operations
    fun updateImageUri(blockId: String, uri: String) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) block.copy(imageUri = uri) else block
        }
    }

    fun saveNote(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val content = gson.toJson(_blocks.value)
            val currentId = _noteId.value
            val time = System.currentTimeMillis()
            
            val isTitleEmpty = _title.value.trim().isEmpty()
            val isContentEmpty = _blocks.value.all {
                it.type == BlockType.TEXT && it.textContent.trim().isEmpty()
            }
            
            if (isTitleEmpty && isContentEmpty) {
                if (currentId != null && currentId != 0L) {
                    repository.getNoteById(currentId)?.let { existing ->
                        repository.deleteNote(existing)
                    }
                }
                onComplete()
                return@launch
            }
            
            if (currentId == null || currentId == 0L) {
                val newNote = NoteEntity(
                    title = _title.value.ifEmpty { "Tanpa Judul" },
                    content = content,
                    backgroundColor = _backgroundColor.value,
                    createdAt = time,
                    updatedAt = time,
                    noteType = "MERGED"
                )
                repository.insertNote(newNote)
            } else {
                repository.getNoteById(currentId)?.let { existing ->
                    val updatedNote = existing.copy(
                        title = _title.value.ifEmpty { "Tanpa Judul" },
                        content = content,
                        backgroundColor = _backgroundColor.value,
                        updatedAt = time,
                        noteType = "MERGED"
                    )
                    repository.updateNote(updatedNote)
                }
            }
            onComplete()
        }
    }
}
