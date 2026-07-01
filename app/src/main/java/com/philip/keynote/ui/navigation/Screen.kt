package com.philip.keynote.ui.navigation

sealed class Screen(val route: String) {
    object NotesList : Screen("notes_list")
    
    object NoteDetail : Screen("note_detail/{noteId}?editMode={isEditMode}") {
        fun createRoute(noteId: Long, isEditMode: Boolean = false) = "note_detail/$noteId?editMode=$isEditMode"
    }
    
    object Trash : Screen("trash")
    
    object Archive : Screen("archive")
    
    object PasswordSetup : Screen("password_setup")
    
    object PasswordCategories : Screen("password_categories")
    
    object PasswordList : Screen("password_list/{categoryId}") {
        fun createRoute(categoryId: Long) = "password_list/$categoryId"
    }
    
    object PasswordDetail : Screen("password_detail/{passwordId}/{categoryId}?editMode={isEditMode}") {
        fun createRoute(passwordId: Long, categoryId: Long, isEditMode: Boolean = false) = "password_detail/$passwordId/$categoryId?editMode=$isEditMode"
    }
}
