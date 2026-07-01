package com.philip.keynote

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.philip.keynote.data.settings.SettingsManager
import com.philip.keynote.ui.ViewModelFactory
import com.philip.keynote.ui.navigation.Screen
import com.philip.keynote.ui.notes.*
import com.philip.keynote.ui.passwords.*
import com.philip.keynote.ui.settings.SettingsScreen

class MainActivity : FragmentActivity() {

    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModelFactory = ViewModelFactory(this)
        settingsManager = SettingsManager(this)

        setContent {
            var appTheme by remember { mutableStateOf(settingsManager.getAppTheme()) }
            var currentLanguage by remember { mutableStateOf(settingsManager.getLanguage()) }
            var viewMode by remember { mutableStateOf(settingsManager.getNotesViewMode()) }

            val isDark = when (appTheme) {
                "DARK", "BLACK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            // Standard M3 color scheme mapped dynamically to KeyNote custom 9 colors palette
            val colorScheme = when (appTheme) {
                "RED" -> if (isSystemInDarkTheme()) {
                    darkColorScheme(primary = Color(0xFFEF5350), onPrimary = Color.White, surfaceVariant = Color(0xFF3C1F1F))
                } else {
                    lightColorScheme(primary = Color(0xFFEF5350), onPrimary = Color.White, surfaceVariant = Color(0xFFFFEBEE))
                }
                "ORANGE" -> if (isSystemInDarkTheme()) {
                    darkColorScheme(primary = Color(0xFFFF9800), onPrimary = Color.Black, surfaceVariant = Color(0xFF3E2C1E))
                } else {
                    lightColorScheme(primary = Color(0xFFFF9800), onPrimary = Color.Black, surfaceVariant = Color(0xFFFFF3E0))
                }
                "YELLOW" -> if (isSystemInDarkTheme()) {
                    darkColorScheme(primary = Color(0xFFFFEE58), onPrimary = Color.Black, surfaceVariant = Color(0xFF3E3D1E))
                } else {
                    lightColorScheme(primary = Color(0xFFFFEE58), onPrimary = Color.Black, surfaceVariant = Color(0xFFFFFDE7))
                }
                "GREEN" -> if (isSystemInDarkTheme()) {
                    darkColorScheme(primary = Color(0xFF4CAF50), onPrimary = Color.White, surfaceVariant = Color(0xFF1E3C1F))
                } else {
                    lightColorScheme(primary = Color(0xFF4CAF50), onPrimary = Color.White, surfaceVariant = Color(0xFFE8F5E9))
                }
                "BLUE" -> if (isSystemInDarkTheme()) {
                    darkColorScheme(primary = Color(0xFF2196F3), onPrimary = Color.White, surfaceVariant = Color(0xFF1E2E3E))
                } else {
                    lightColorScheme(primary = Color(0xFF2196F3), onPrimary = Color.White, surfaceVariant = Color(0xFFE3F2FD))
                }
                "PURPLE" -> if (isSystemInDarkTheme()) {
                    darkColorScheme(primary = Color(0xFFAB47BC), onPrimary = Color.White, surfaceVariant = Color(0xFF3A1E3E))
                } else {
                    lightColorScheme(primary = Color(0xFFAB47BC), onPrimary = Color.White, surfaceVariant = Color(0xFFF3E5F5))
                }
                "GRAY" -> if (isSystemInDarkTheme()) {
                    darkColorScheme(primary = Color(0xFF8E8E93), onPrimary = Color.White, surfaceVariant = Color(0xFF2C2C2E))
                } else {
                    lightColorScheme(primary = Color(0xFF8E8E93), onPrimary = Color.White, surfaceVariant = Color(0xFFF2F2F7))
                }
                "BLACK" -> {
                    darkColorScheme(primary = Color.White, onPrimary = Color.Black, background = Color(0xFF121212), surface = Color(0xFF121212))
                }
                else -> { // SYSTEM, LIGHT, DARK
                    if (isDark) darkColorScheme() else lightColorScheme()
                }
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val notesViewModel: NotesViewModel = ViewModelProvider(
                        this,
                        viewModelFactory
                    )[NotesViewModel::class.java]

                    val noteDetailViewModel: NoteDetailViewModel = ViewModelProvider(
                        this,
                        viewModelFactory
                    )[NoteDetailViewModel::class.java]

                    val passwordViewModel: PasswordViewModel = ViewModelProvider(
                        this,
                        viewModelFactory
                    )[PasswordViewModel::class.java]

                    NavHost(
                        navController = navController,
                        startDestination = Screen.NotesList.route
                    ) {
                        composable(Screen.NotesList.route) {
                            LaunchedEffect(Unit) {
                                appTheme = settingsManager.getAppTheme()
                                currentLanguage = settingsManager.getLanguage()
                                viewMode = settingsManager.getNotesViewMode()
                            }
                            NotesListScreen(
                                notesViewModel = notesViewModel,
                                passwordViewModel = passwordViewModel,
                                settingsManager = settingsManager,
                                onNoteClick = { id, isEditMode ->
                                    navController.navigate(Screen.NoteDetail.createRoute(id, isEditMode))
                                },
                                onAddNoteClick = {
                                    navController.navigate(Screen.NoteDetail.createRoute(0L, true))
                                },
                                onOpenTrash = {
                                    navController.navigate(Screen.Trash.route)
                                },
                                onOpenArchive = {
                                    navController.navigate(Screen.Archive.route)
                                },
                                onOpenPasswordManager = {
                                    navController.navigate(Screen.PasswordCategories.route)
                                },
                                onOpenPasswordSetup = {
                                    navController.navigate(Screen.PasswordSetup.route)
                                },
                                onOpenSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                settingsManager = settingsManager,
                                onThemeChanged = {
                                    appTheme = settingsManager.getAppTheme()
                                },
                                onBack = {
                                    appTheme = settingsManager.getAppTheme()
                                    currentLanguage = settingsManager.getLanguage()
                                    viewMode = settingsManager.getNotesViewMode()
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = Screen.NoteDetail.route,
                            arguments = listOf(
                                navArgument("noteId") { type = NavType.LongType },
                                navArgument("editMode") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getLong("noteId")
                            val isEditMode = backStackEntry.arguments?.getBoolean("editMode") ?: false
                            NoteDetailScreen(
                                viewModel = noteDetailViewModel,
                                noteId = noteId,
                                initialEditMode = isEditMode,
                                settingsManager = settingsManager,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Trash.route) {
                            TrashScreen(
                                viewModel = notesViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Archive.route) {
                            ArchiveScreen(
                                viewModel = notesViewModel,
                                onNoteClick = { id ->
                                    navController.navigate(Screen.NoteDetail.createRoute(id, false))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.PasswordSetup.route) {
                            PasswordSetupScreen(
                                viewModel = passwordViewModel,
                                onBack = { navController.popBackStack() },
                                onSetupSuccess = {
                                    navController.popBackStack()
                                    navController.navigate(Screen.PasswordCategories.route)
                                }
                            )
                        }

                        composable(Screen.PasswordCategories.route) {
                            PasswordCategoriesScreen(
                                viewModel = passwordViewModel,
                                isDarkTheme = isDark,
                                onCategoryClick = { id ->
                                    navController.navigate(Screen.PasswordList.createRoute(id))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.PasswordList.route,
                            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
                            PasswordListScreen(
                                viewModel = passwordViewModel,
                                categoryId = categoryId,
                                onPasswordClick = { passwordId, catId, isEditMode ->
                                    navController.navigate(Screen.PasswordDetail.createRoute(passwordId, catId, isEditMode))
                                },
                                onAddPasswordClick = { catId ->
                                    navController.navigate(Screen.PasswordDetail.createRoute(0L, catId, true))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.PasswordDetail.route,
                            arguments = listOf(
                                navArgument("passwordId") { type = NavType.LongType },
                                navArgument("categoryId") { type = NavType.LongType },
                                navArgument("editMode") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry ->
                            val passwordId = backStackEntry.arguments?.getLong("passwordId")
                            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
                            val isEditMode = backStackEntry.arguments?.getBoolean("editMode") ?: false
                            PasswordDetailScreen(
                                viewModel = passwordViewModel,
                                passwordId = passwordId,
                                categoryId = categoryId,
                                initialEditMode = isEditMode,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
