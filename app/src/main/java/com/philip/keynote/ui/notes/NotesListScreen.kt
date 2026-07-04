package com.philip.keynote.ui.notes

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.philip.keynote.data.local.BlockType
import com.philip.keynote.data.local.NoteBlock
import com.philip.keynote.data.local.entity.NoteEntity
import com.philip.keynote.data.settings.SettingsManager
import com.philip.keynote.security.BiometricAuthenticator
import com.philip.keynote.ui.passwords.PasswordViewModel
import com.philip.keynote.ui.theme.Localization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    notesViewModel: NotesViewModel,
    passwordViewModel: PasswordViewModel,
    settingsManager: SettingsManager,
    onNoteClick: (Long, Boolean) -> Unit,
    onAddNoteClick: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenPasswordManager: () -> Unit,
    onOpenPasswordSetup: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val notes by notesViewModel.activeNotes.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    val language = settingsManager.getLanguage()
    val viewMode = settingsManager.getNotesViewMode()

    // Pull-to-Archive drag accumulator state
    var accumulatedPullDown by remember { mutableStateOf(0f) }
    var isArchiveTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isArchiveTriggered = false
    }

    // PIN Verification Dialog states for locked notes
    var showPinVerificationDialog by remember { mutableStateOf(false) }
    var pinVerificationText by remember { mutableStateOf("") }
    var pendingLockedNoteId by remember { mutableStateOf<Long?>(null) }
    var pendingEditMode by remember { mutableStateOf(false) }
    var pendingToggleLockAction by remember { mutableStateOf(false) }
    var isBatchLockAction by remember { mutableStateOf(false) }
    var batchLockTargetState by remember { mutableStateOf(false) }
    var pendingPasswordManagerAccess by remember { mutableStateOf(false) }

    val biometricAuthenticator = remember { BiometricAuthenticator(context) }

    // Long press context menu state
    var selectedNoteForMenu by remember { mutableStateOf<NoteEntity?>(null) }
    var showMoreOptionsDialog by remember { mutableStateOf(false) }

    // Multi-Selection state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedNotes = remember { mutableStateListOf<NoteEntity>() }
    var showMultiColorPicker by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedNotes.clear()
    }

    fun toggleSelection(note: NoteEntity) {
        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note)
            if (selectedNotes.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            selectedNotes.add(note)
        }
    }

    // Nested scroll connection to intercept pull down overscroll events
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Trigger only if pulling down (available.y > 0) when scroll is at the top
                if (available.y > 0f && source == NestedScrollSource.Drag && !isArchiveTriggered) {
                    accumulatedPullDown += available.y
                    if (accumulatedPullDown > 200f) {
                        isArchiveTriggered = true
                        accumulatedPullDown = 0f
                        onOpenArchive()
                    }
                    return Offset(x = 0f, y = available.y)
                }
                // Reset accumulator if user drags back upwards
                if (available.y < 0f) {
                    accumulatedPullDown = 0f
                    isArchiveTriggered = false
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                accumulatedPullDown = 0f
                isArchiveTriggered = false
                return Velocity.Zero
            }
        }
    }

    // Exact 9 colors matching the reference image layout
    val noteBgColors = listOf(
        0xFFEF5350.toInt(), // Red
        0xFFFF9800.toInt(), // Orange
        0xFFFFEE58.toInt(), // Yellow
        0xFF4CAF50.toInt(), // Green
        0xFF2196F3.toInt(), // Blue
        0xFFAB47BC.toInt(), // Purple
        0xFF121212.toInt(), // Soft Black
        0xFF8E8E93.toInt(), // Gray
        0xFFFFFFFF.toInt()  // White (Default)
    )

    // Double-tap tracker for logo
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    fun handleLogoTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < 500) {
            tapCount++
        } else {
            tapCount = 1
        }
        lastTapTime = now

        if (tapCount >= 3) {
            tapCount = 0
            val status = biometricAuthenticator.isBiometricAvailable()
            if (status == BiometricAuthenticator.BiometricStatus.AVAILABLE) {
                biometricAuthenticator.authenticate(
                    activity = context as FragmentActivity,
                    title = Localization.getString("password_manager", language),
                    subtitle = "Autentikasi biometrik diperlukan",
                    description = "Gunakan sidik jari atau PIN Anda.",
                    callback = object : BiometricAuthenticator.BiometricCallback {
                        override fun onAuthenticationSuccess(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                            if (passwordViewModel.isPinSetup()) {
                                onOpenPasswordManager()
                            } else {
                                onOpenPasswordSetup()
                            }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            Toast.makeText(context, "Autentikasi gagal: $errString", Toast.LENGTH_SHORT).show()
                        }

                        override fun onAuthenticationFailed() {
                            Toast.makeText(context, "Autentikasi tidak cocok", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                if (passwordViewModel.isPinSetup()) {
                    pendingPasswordManagerAccess = true
                    showPinVerificationDialog = true
                } else {
                    onOpenPasswordSetup()
                }
            }
        }
    }

    // Helper function to handle locked notes access checks securely
    fun handleNoteClick(note: NoteEntity, isEdit: Boolean) {
        if (note.isLocked) {
            val status = biometricAuthenticator.isBiometricAvailable()
            if (status == BiometricAuthenticator.BiometricStatus.AVAILABLE) {
                biometricAuthenticator.authenticate(
                    activity = context as FragmentActivity,
                    title = "Catatan Dikunci",
                    subtitle = "Gunakan sidik jari atau PIN Anda untuk membuka catatan.",
                    description = "",
                    callback = object : BiometricAuthenticator.BiometricCallback {
                        override fun onAuthenticationSuccess(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                            onNoteClick(note.id, isEdit)
                        }
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            Toast.makeText(context, "Batal membuka catatan", Toast.LENGTH_SHORT).show()
                        }
                        override fun onAuthenticationFailed() {
                            Toast.makeText(context, "Autentikasi gagal", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                if (passwordViewModel.isPinSetup()) {
                    pendingLockedNoteId = note.id
                    pendingEditMode = isEdit
                    pendingToggleLockAction = false
                    showPinVerificationDialog = true
                } else {
                    Toast.makeText(context, "Harap aktifkan PIN di Pengaturan Pengelola Kata Sandi terlebih dahulu", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            onNoteClick(note.id, isEdit)
        }
    }

    fun handleNoteItemClick(note: NoteEntity) {
        if (isSelectionMode) {
            toggleSelection(note)
        } else {
            handleNoteClick(note, false)
        }
    }

    fun handleNoteItemLongClick(note: NoteEntity) {
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedNotes.clear()
            selectedNotes.add(note)
        } else {
            toggleSelection(note)
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedNotes.size} terpilih",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedNotes.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Batal")
                        }
                    },
                    actions = {
                        val allNotesSelected = selectedNotes.size == notes.size
                        IconButton(onClick = {
                            if (allNotesSelected) {
                                selectedNotes.clear()
                                isSelectionMode = false
                            } else {
                                selectedNotes.clear()
                                selectedNotes.addAll(notes)
                            }
                        }) {
                            Icon(
                                imageVector = if (allNotesSelected) Icons.Default.CheckBox else Icons.Default.SelectAll,
                                contentDescription = "Pilih Semua"
                            )
                        }
                        IconButton(onClick = { showMultiColorPicker = true }) {
                            Icon(Icons.Default.ColorLens, contentDescription = "Ubah Warna")
                        }
                        IconButton(onClick = {
                            val allSelectedLocked = selectedNotes.all { it.isLocked }
                            val targetLockedState = !allSelectedLocked
                            val actionText = if (targetLockedState) "Mengunci catatan terpilih" else "Membuka kunci catatan terpilih"
                            
                            val notesCopy = selectedNotes.toList()
                            val performBatchLock = {
                                notesViewModel.setSelectedNotesLock(notesCopy, targetLockedState)
                                val msg = if (targetLockedState) "${notesCopy.size} catatan dikunci" else "${notesCopy.size} kunci catatan dibuka"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                selectedNotes.clear()
                                isSelectionMode = false
                            }

                            val status = biometricAuthenticator.isBiometricAvailable()
                            if (status == BiometricAuthenticator.BiometricStatus.AVAILABLE) {
                                biometricAuthenticator.authenticate(
                                    activity = context as FragmentActivity,
                                    title = actionText,
                                    subtitle = "Autentikasi diperlukan",
                                    description = "",
                                    callback = object : BiometricAuthenticator.BiometricCallback {
                                        override fun onAuthenticationSuccess(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                            performBatchLock()
                                        }
                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            Toast.makeText(context, "Autentikasi gagal", Toast.LENGTH_SHORT).show()
                                        }
                                        override fun onAuthenticationFailed() {
                                            Toast.makeText(context, "Autentikasi tidak cocok", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            } else {
                                if (passwordViewModel.isPinSetup()) {
                                    isBatchLockAction = true
                                    batchLockTargetState = targetLockedState
                                    showPinVerificationDialog = true
                                } else {
                                    Toast.makeText(context, "Harap aktifkan PIN di Pengaturan Pengelola Kata Sandi terlebih dahulu", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            val allSelectedLocked = selectedNotes.all { it.isLocked }
                            Icon(
                                imageVector = if (allSelectedLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Kunci/Buka Catatan"
                            )
                        }
                        IconButton(onClick = {
                            selectedNotes.forEach { notesViewModel.moveToArchive(it) }
                            Toast.makeText(context, "${selectedNotes.size} catatan diarsipkan", Toast.LENGTH_SHORT).show()
                            selectedNotes.clear()
                            isSelectionMode = false
                        }) {
                            Icon(Icons.Default.Archive, contentDescription = "Arsip")
                        }
                        IconButton(onClick = {
                            selectedNotes.forEach { notesViewModel.moveToTrash(it) }
                            Toast.makeText(context, "${selectedNotes.size} catatan dipindahkan ke sampah", Toast.LENGTH_SHORT).show()
                            selectedNotes.clear()
                            isSelectionMode = false
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "KeyNote",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    handleLogoTap()
                                }
                                .padding(4.dp)
                        )
                    },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(Localization.getString("settings", language)) },
                                onClick = {
                                    showMenu = false
                                    onOpenSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Localization.getString("trash", language)) },
                                onClick = {
                                    showMenu = false
                                    onOpenTrash()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Localization.getString("archive", language)) },
                                onClick = {
                                    showMenu = false
                                    onOpenArchive()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Localization.getString("sync_backup", language)) },
                                onClick = {
                                    showMenu = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                IconButton(
                    onClick = onAddNoteClick,
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Catatan", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Localization.getString("empty_notes", language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            if (viewMode == "GRID") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedNotes.contains(note),
                            onClick = { handleNoteItemClick(note) },
                            onDoubleClick = { if (!isSelectionMode) handleNoteClick(note, true) else toggleSelection(note) },
                            onLongClick = { handleNoteItemLongClick(note) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteListItem(
                            note = note,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedNotes.contains(note),
                            onClick = { handleNoteItemClick(note) },
                            onDoubleClick = { if (!isSelectionMode) handleNoteClick(note, true) else toggleSelection(note) },
                            onLongClick = { handleNoteItemLongClick(note) }
                        )
                    }
                }
            }
        }

        // Long Press Bottom Sheet
        if (selectedNoteForMenu != null) {
            val note = selectedNoteForMenu!!
            ModalBottomSheet(
                onDismissRequest = { selectedNoteForMenu = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Color Picker Grid Layout matching 3x3 alignment of image
                    Text("Ubah Warna:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(130.dp)
                    ) {
                        items(noteBgColors) { colorVal ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(colorVal))
                                    .border(
                                        width = if (note.backgroundColor == colorVal) 2.dp else 1.dp,
                                        color = if (note.backgroundColor == colorVal) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        notesViewModel.updateNoteColor(note, colorVal)
                                        selectedNoteForMenu = note.copy(backgroundColor = colorVal)
                                    }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    ListItem(
                        headlineContent = { Text("Arsir / Arsipkan") },
                        leadingContent = { Icon(Icons.Default.Archive, contentDescription = null) },
                        modifier = Modifier.clickable {
                            notesViewModel.moveToArchive(note)
                            selectedNoteForMenu = null
                            Toast.makeText(context, "Diarsipkan", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Hapus (Pindahkan ke Sampah)") },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                        modifier = Modifier.clickable {
                            notesViewModel.moveToTrash(note)
                            selectedNoteForMenu = null
                            Toast.makeText(context, "Dipindahkan ke Tong Sampah", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Pasang Pengingat") },
                        leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "Pengingat dipasang untuk ${note.title}", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Pilihan Lainnya...") },
                        leadingContent = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showMoreOptionsDialog = true
                        }
                    )
                }
            }
        }

        // More options dialog
        if (showMoreOptionsDialog && selectedNoteForMenu != null) {
            val note = selectedNoteForMenu!!
            AlertDialog(
                onDismissRequest = { showMoreOptionsDialog = false },
                title = { Text("Pilihan Lainnya") },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                showMoreOptionsDialog = false
                                selectedNoteForMenu = null
                                notesViewModel.duplicateNote(note)
                                Toast.makeText(context, "Catatan diduplikasi", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Duplikat Catatan", style = MaterialTheme.typography.bodyLarge)
                        }
                        TextButton(
                            onClick = {
                                showMoreOptionsDialog = false
                                selectedNoteForMenu = null
                                shareNote(context, note.title, note.content)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kirim / Bagikan", style = MaterialTheme.typography.bodyLarge)
                        }
                        
                        // Note Lock Option requiring auth check
                        TextButton(
                            onClick = {
                                showMoreOptionsDialog = false
                                val isLockAction = !note.isLocked
                                val actionText = if (isLockAction) "Mengunci catatan" else "Membuka kunci catatan"

                                val status = biometricAuthenticator.isBiometricAvailable()
                                if (status == BiometricAuthenticator.BiometricStatus.AVAILABLE) {
                                    biometricAuthenticator.authenticate(
                                        activity = context as FragmentActivity,
                                        title = actionText,
                                        subtitle = "Autentikasi diperlukan",
                                        description = "",
                                        callback = object : BiometricAuthenticator.BiometricCallback {
                                            override fun onAuthenticationSuccess(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                                notesViewModel.toggleNoteLock(note)
                                                val msg = if (isLockAction) "Catatan dikunci" else "Kunci catatan dibuka"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                selectedNoteForMenu = null
                                            }
                                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                Toast.makeText(context, "Autentikasi gagal", Toast.LENGTH_SHORT).show()
                                                selectedNoteForMenu = null
                                            }
                                            override fun onAuthenticationFailed() {
                                                Toast.makeText(context, "Autentikasi tidak cocok", Toast.LENGTH_SHORT).show()
                                                selectedNoteForMenu = null
                                            }
                                        }
                                    )
                                } else {
                                    if (passwordViewModel.isPinSetup()) {
                                        pendingLockedNoteId = note.id
                                        pendingToggleLockAction = true
                                        showPinVerificationDialog = true
                                    } else {
                                        Toast.makeText(context, "Harap aktifkan PIN di Pengaturan Pengelola Kata Sandi terlebih dahulu", Toast.LENGTH_LONG).show()
                                        selectedNoteForMenu = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (note.isLocked) "Buka Kunci Sandi" else "Kunci Sandi",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMoreOptionsDialog = false }) {
                        Text("Tutup")
                    }
                }
            )
        }

        // Custom Master PIN verification dialog for note locks
        if (showPinVerificationDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPinVerificationDialog = false
                    pinVerificationText = ""
                    pendingLockedNoteId = null
                    isBatchLockAction = false
                },
                title = { Text("Catatan Dikunci") },
                text = {
                    Column {
                        Text("Masukkan PIN Master untuk memverifikasi identitas Anda:")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pinVerificationText,
                            onValueChange = { if (it.length <= 4) pinVerificationText = it },
                            label = { Text("PIN Master") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (passwordViewModel.verifyPin(pinVerificationText)) {
                                showPinVerificationDialog = false
                                pinVerificationText = ""
                                if (isBatchLockAction) {
                                    val notesCopy = selectedNotes.toList()
                                    notesViewModel.setSelectedNotesLock(notesCopy, batchLockTargetState)
                                    val msg = if (batchLockTargetState) "${notesCopy.size} catatan dikunci" else "${notesCopy.size} kunci catatan dibuka"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    selectedNotes.clear()
                                    isSelectionMode = false
                                    isBatchLockAction = false
                                } else if (pendingPasswordManagerAccess) {
                                    pendingPasswordManagerAccess = false
                                    onOpenPasswordManager()
                                } else {
                                    val noteId = pendingLockedNoteId
                                    if (noteId != null) {
                                        val targetNote = notes.find { it.id == noteId }
                                        if (targetNote != null) {
                                            if (pendingToggleLockAction) {
                                                notesViewModel.toggleNoteLock(targetNote)
                                                val msg = if (targetNote.isLocked) "Kunci catatan dibuka" else "Catatan dikunci"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            } else {
                                                onNoteClick(noteId, pendingEditMode)
                                            }
                                        }
                                    }
                                    pendingLockedNoteId = null
                                }
                                selectedNoteForMenu = null
                            } else {
                                Toast.makeText(context, "PIN salah!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Buka")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPinVerificationDialog = false
                            pinVerificationText = ""
                            pendingLockedNoteId = null
                            selectedNoteForMenu = null
                            isBatchLockAction = false
                            pendingPasswordManagerAccess = false
                        }
                    ) {
                        Text("Batal")
                    }
                }
            )
        }

        // Color Picker Dialog for Multi-Selection Mode
        if (showMultiColorPicker) {
            AlertDialog(
                onDismissRequest = { showMultiColorPicker = false },
                title = { Text("Ubah Warna Catatan Terpilih") },
                text = {
                    Column {
                        Text("Pilih warna latar belakang untuk semua catatan yang terpilih:")
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(130.dp)
                        ) {
                            items(noteBgColors) { colorVal ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(colorVal))
                                        .border(
                                            width = 1.dp,
                                            color = Color.Gray,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedNotes.forEach { notesViewModel.updateNoteColor(it, colorVal) }
                                            selectedNotes.clear()
                                            isSelectionMode = false
                                            showMultiColorPicker = false
                                        }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMultiColorPicker = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(
    note: NoteEntity,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // If background is white (default), use standard surfaceVariant color
    val isDefaultBg = note.backgroundColor == 0xFFFFFFFF.toInt() || note.backgroundColor == 0xFF121212.toInt()
    
    val stripeColor = if (isDefaultBg) Color.Gray else Color(note.backgroundColor)
    val containerBgColor = if (isSelectionMode && isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else if (isDefaultBg) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        Color(note.backgroundColor).copy(alpha = 0.15f)
    }
    
    val contentTextColor = if (isDefaultBg) MaterialTheme.colorScheme.onSurface else Color(note.backgroundColor)

    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = containerBgColor),
        border = if (isSelectionMode && isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(stripeColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Lock Indicator icon next to the date
            if (note.isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Terverifikasi Kunci",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(end = 8.dp).size(18.dp)
                )
            }

            // Selection Checkmark icon on the right side next to date
            if (isSelectionMode && isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Terpilih",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp).size(18.dp)
                )
            }

            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            Text(
                text = dateFormat.format(Date(note.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDefaultBg = note.backgroundColor == 0xFFFFFFFF.toInt() || note.backgroundColor == 0xFF121212.toInt()
    val isSystemDark = isSystemInDarkTheme()
    
    val cardColor = if (isSelectionMode && isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else if (isDefaultBg) {
        if (isSystemDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    } else {
        Color(note.backgroundColor)
    }
    
    val contentColor = if (isDefaultBg) {
        if (isSystemDark) Color.White else Color.Black
    } else {
        if (note.backgroundColor == 0xFF121212.toInt()) Color.White else Color.Black
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelectionMode && isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cardColor)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Lock Indicator icon in the top right corner of grid cards
                if (note.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Terverifikasi Kunci",
                        tint = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Selection Checkmark icon in the top right corner
                if (isSelectionMode && isSelected) {
                    if (note.isLocked) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Terpilih",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            val bodyText = if (note.content.startsWith("[")) {
                try {
                    val gson = com.google.gson.Gson()
                    val type = object : com.google.gson.reflect.TypeToken<List<NoteBlock>>() {}.type
                    val blocksList = gson.fromJson<List<NoteBlock>>(note.content, type) ?: emptyList()
                    val firstBlock = blocksList.firstOrNull()
                    when (firstBlock?.type) {
                        BlockType.TEXT -> firstBlock.textContent
                        BlockType.CHECKLIST -> "[Daftar Centang]"
                        BlockType.TABLE -> "[Tabel Data]"
                        BlockType.IMAGE -> "[Gambar Media]"
                        null -> ""
                    }
                } catch (e: Exception) {
                    ""
                }
            } else {
                note.content
            }

            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun shareNote(context: android.content.Context, title: String, content: String) {
    val builder = java.lang.StringBuilder()
    builder.append("=== ").append(title).append(" ===\n\n")
    
    if (content.startsWith("[")) {
        try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.philip.keynote.data.local.NoteBlock>>() {}.type
            val blocks: List<com.philip.keynote.data.local.NoteBlock> = gson.fromJson(content, type) ?: emptyList()
            
            blocks.forEach { block ->
                when (block.type) {
                    com.philip.keynote.data.local.BlockType.TEXT -> {
                        if (block.textContent.isNotEmpty()) {
                            builder.append(block.textContent).append("\n\n")
                        }
                    }
                    com.philip.keynote.data.local.BlockType.CHECKLIST -> {
                        block.checklistItems.forEach { item ->
                            val status = if (item.isChecked) "[x]" else "[ ]"
                            builder.append("$status ${item.text}\n")
                        }
                        builder.append("\n")
                    }
                    com.philip.keynote.data.local.BlockType.TABLE -> {
                        val table = block.tableData
                        if (table.headers.isNotEmpty()) {
                            builder.append(table.headers.joinToString(" | ")).append("\n")
                            builder.append("-".repeat(table.headers.joinToString(" | ").length)).append("\n")
                            table.rows.forEach { row ->
                                builder.append(row.joinToString(" | ")).append("\n")
                            }
                        }
                        builder.append("\n")
                    }
                    com.philip.keynote.data.local.BlockType.IMAGE -> {
                        if (block.imageUri.isNotEmpty()) {
                            builder.append("[Gambar: ${block.imageUri}]\n\n")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            builder.append(content)
        }
    } else {
        builder.append(content)
    }

    val shareText = builder.toString().trim()
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    val shareIntent = android.content.Intent.createChooser(sendIntent, "Bagikan Catatan")
    context.startActivity(shareIntent)
}
