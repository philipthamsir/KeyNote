package com.philip.keynote.data.local

enum class BlockType {
    TEXT, CHECKLIST, TABLE, IMAGE
}

data class NoteBlock(
    val id: String,
    val type: BlockType,
    val textContent: String = "",
    val checklistItems: List<ChecklistItem> = emptyList(),
    val tableData: TableData = TableData(),
    val imageUri: String = "",
    // Formatting styles
    val fontSize: Float = 16f,
    val fontColor: Int = 0xFF000000.toInt(),
    val fontStyle: String = "NORMAL", // NORMAL, BOLD, ITALIC, BOLD_ITALIC
    val textAlignment: String = "LEFT" // LEFT, CENTER, JUSTIFIED
)
