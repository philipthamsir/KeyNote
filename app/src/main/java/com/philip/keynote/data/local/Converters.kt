package com.philip.keynote.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Helper models for rich note contents
data class ChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean
)

data class TableData(
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList()
)

// Helper models for dynamic password manager credentials
enum class FieldType {
    IDENTIFIER, PASSWORD, PIN, URL, CUSTOM
}

data class PasswordField(
    val id: String,
    val type: FieldType,
    val label: String,
    val value: String
)

class Converters {
    private val gson = Gson()

    // 1. ChecklistItem converters
    @TypeConverter
    fun fromChecklistJson(json: String?): List<ChecklistItem>? {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<ChecklistItem>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun toChecklistJson(list: List<ChecklistItem>?): String {
        return gson.toJson(list ?: emptyList<ChecklistItem>())
    }

    // 2. TableData converters
    @TypeConverter
    fun fromTableJson(json: String?): TableData? {
        if (json.isNullOrEmpty()) return TableData()
        return gson.fromJson(json, TableData::class.java)
    }

    @TypeConverter
    fun toTableJson(table: TableData?): String {
        return gson.toJson(table ?: TableData())
    }

    // 3. PasswordField converters
    @TypeConverter
    fun fromFieldsJson(json: String?): List<PasswordField>? {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<PasswordField>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun toFieldsJson(list: List<PasswordField>?): String {
        return gson.toJson(list ?: emptyList<PasswordField>())
    }

    // 4. NoteBlock converters
    @TypeConverter
    fun fromNoteBlocksJson(json: String?): List<NoteBlock>? {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<NoteBlock>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun toNoteBlocksJson(list: List<NoteBlock>?): String {
        return gson.toJson(list ?: emptyList<NoteBlock>())
    }
}
