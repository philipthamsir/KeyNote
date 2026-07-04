package com.philip.keynote.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.philip.keynote.data.backup.BackupManager
import com.philip.keynote.data.backup.GoogleDriveBackupHelper
import com.philip.keynote.data.backup.GoogleDriveAuthException
import com.philip.keynote.data.backup.BackupWorker
import com.philip.keynote.data.settings.SettingsManager
import com.philip.keynote.ui.theme.Localization
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onThemeChanged: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }
    val googleDriveBackupHelper = remember { GoogleDriveBackupHelper(context) }

    var viewMode by remember { mutableStateOf(settingsManager.getNotesViewMode()) }
    var language by remember { mutableStateOf(settingsManager.getLanguage()) }
    var appTheme by remember { mutableStateOf(settingsManager.getAppTheme()) }

    // Auto-Save States
    var autoSaveEnabled by remember { mutableStateOf(settingsManager.isAutoSaveEnabled()) }
    var autoSaveInterval by remember { mutableStateOf(settingsManager.getAutoSaveInterval()) }
    var showCustomIntervalInput by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf((autoSaveInterval / 60000L).toString()) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }

    // Google Cloud Sync States
    var googleSignedIn by remember { mutableStateOf(settingsManager.isGoogleSignedIn()) }
    var googleEmail by remember { mutableStateOf(settingsManager.getGoogleEmail()) }
    var googleName by remember { mutableStateOf(settingsManager.getGoogleName()) }
    var onlineBackupEnabled by remember { mutableStateOf(settingsManager.isOnlineBackupEnabled()) }
    var lastBackupTime by remember { mutableStateOf(settingsManager.getLastOnlineBackupTime()) }
    var isSyncing by remember { mutableStateOf(false) }



    // Configure Google Sign In SDK Client
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val email = account.email ?: ""
                    val name = account.displayName ?: ""
                    settingsManager.setGoogleSignedIn(true, email, name)
                    googleSignedIn = true
                    googleEmail = email
                    googleName = name
                    Toast.makeText(context, "Masuk sebagai $email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Akun Google kosong.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Gagal masuk Google: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                coroutineScope.launch {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { stream ->
                            val result = backupManager.exportBackup(backupPassword, stream)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Backup berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gagal backup: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        backupPassword = ""
                    }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                coroutineScope.launch {
                    try {
                        context.contentResolver.openInputStream(it)?.use { stream ->
                            val result = backupManager.importBackup(backupPassword, stream)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Restore backup berhasil! Mulai ulang aplikasi untuk menyegarkan data.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Gagal restore: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        backupPassword = ""
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.getString("settings", language)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // View Mode Setting
            Text(
                text = Localization.getString("view_mode", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .clickable {
                            viewMode = "LIST"
                            settingsManager.setNotesViewMode("LIST")
                        }
                        .padding(8.dp)
                ) {
                    RadioButton(selected = viewMode == "LIST", onClick = {
                        viewMode = "LIST"
                        settingsManager.setNotesViewMode("LIST")
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("List (Baris)")
                }
                Spacer(modifier = Modifier.width(24.dp))
                Row(
                    modifier = Modifier
                        .clickable {
                            viewMode = "GRID"
                            settingsManager.setNotesViewMode("GRID")
                        }
                        .padding(8.dp)
                ) {
                    RadioButton(selected = viewMode == "GRID", onClick = {
                        viewMode = "GRID"
                        settingsManager.setNotesViewMode("GRID")
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grid (Kotak)")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Language Setting
            Text(
                text = Localization.getString("language", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .clickable {
                            language = "in"
                            settingsManager.setLanguage("in")
                        }
                        .padding(8.dp)
                ) {
                    RadioButton(selected = language == "in", onClick = {
                        language = "in"
                        settingsManager.setLanguage("in")
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bahasa Indonesia")
                }
                Spacer(modifier = Modifier.width(24.dp))
                Row(
                    modifier = Modifier
                        .clickable {
                            language = "en"
                            settingsManager.setLanguage("en")
                        }
                        .padding(8.dp)
                ) {
                    RadioButton(selected = language == "en", onClick = {
                        language = "en"
                        settingsManager.setLanguage("en")
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("English")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Auto-Save Setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Simpan Otomatis (Auto-Save)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = autoSaveEnabled,
                    onCheckedChange = {
                        autoSaveEnabled = it
                        settingsManager.setAutoSaveEnabled(it)
                    }
                )
            }

            if (autoSaveEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Interval Auto-Save:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.clickable {
                        autoSaveInterval = 60000L
                        settingsManager.setAutoSaveInterval(60000L)
                        showCustomIntervalInput = false
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = autoSaveInterval == 60000L && !showCustomIntervalInput, onClick = {
                            autoSaveInterval = 60000L
                            settingsManager.setAutoSaveInterval(60000L)
                            showCustomIntervalInput = false
                        })
                        Text("1 Mnt", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.clickable {
                        autoSaveInterval = 180000L
                        settingsManager.setAutoSaveInterval(180000L)
                        showCustomIntervalInput = false
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = autoSaveInterval == 180000L && !showCustomIntervalInput, onClick = {
                            autoSaveInterval = 180000L
                            settingsManager.setAutoSaveInterval(180000L)
                            showCustomIntervalInput = false
                        })
                        Text("3 Mnt", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.clickable {
                        autoSaveInterval = 300000L
                        settingsManager.setAutoSaveInterval(300000L)
                        showCustomIntervalInput = false
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = autoSaveInterval == 300000L && !showCustomIntervalInput, onClick = {
                            autoSaveInterval = 300000L
                            settingsManager.setAutoSaveInterval(300000L)
                            showCustomIntervalInput = false
                        })
                        Text("5 Mnt", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.clickable {
                        showCustomIntervalInput = true
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = showCustomIntervalInput, onClick = {
                            showCustomIntervalInput = true
                        })
                        Text("Kustom", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (showCustomIntervalInput) {
                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = { text ->
                            customMinutesText = text
                            val minutes = text.toLongOrNull() ?: 1L
                            if (minutes > 0) {
                                autoSaveInterval = minutes * 60000L
                                settingsManager.setAutoSaveInterval(minutes * 60000L)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Interval (Menit)") },
                        modifier = Modifier.width(150.dp).padding(top = 8.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Theme Setting
            Text(
                text = Localization.getString("theme", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            appTheme = "SYSTEM"
                            settingsManager.setAppTheme("SYSTEM")
                            onThemeChanged()
                        }
                        .padding(8.dp)
                ) {
                    RadioButton(selected = appTheme == "SYSTEM", onClick = {
                        appTheme = "SYSTEM"
                        settingsManager.setAppTheme("SYSTEM")
                        onThemeChanged()
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ikuti Sistem")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            appTheme = "LIGHT"
                            settingsManager.setAppTheme("LIGHT")
                            onThemeChanged()
                        }
                        .padding(8.dp)
                ) {
                    RadioButton(selected = appTheme == "LIGHT", onClick = {
                        appTheme = "LIGHT"
                        settingsManager.setAppTheme("LIGHT")
                        onThemeChanged()
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terang (Light)")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            appTheme = "DARK"
                            settingsManager.setAppTheme("DARK")
                            onThemeChanged()
                        }
                        .padding(8.dp)
                ) {
                    RadioButton(selected = appTheme == "DARK", onClick = {
                        appTheme = "DARK"
                        settingsManager.setAppTheme("DARK")
                        onThemeChanged()
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gelap (Dark)")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Custom Color Palette Themes Grid matching the 9 colors palette
                Text("Tema Warna Kustom (Palet):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    listOf(
                        "RED" to Color(0xFFEF5350),
                        "ORANGE" to Color(0xFFFF9800),
                        "YELLOW" to Color(0xFFFFEE58),
                        "GREEN" to Color(0xFF4CAF50)
                    ).forEach { (themeKey, color) ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (appTheme == themeKey) 3.dp else 1.dp,
                                    color = if (appTheme == themeKey) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    appTheme = themeKey
                                    settingsManager.setAppTheme(themeKey)
                                    onThemeChanged()
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    listOf(
                        "BLUE" to Color(0xFF2196F3),
                        "PURPLE" to Color(0xFFAB47BC),
                        "GRAY" to Color(0xFF8E8E93),
                        "BLACK" to Color(0xFF121212)
                    ).forEach { (themeKey, color) ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (appTheme == themeKey) 3.dp else 1.dp,
                                    color = if (appTheme == themeKey) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    appTheme = themeKey
                                    settingsManager.setAppTheme(themeKey)
                                    onThemeChanged()
                                }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Offline Backup Actions (.json)
            Text(
                text = Localization.getString("sync_backup", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showBackupDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ekspor Backup (.json)")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Impor Backup (.json)")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Cloud Backup (Google Drive) Section
            Text(
                text = "Pencadangan Online (Cloud Backup)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (!googleSignedIn) {
                // Official Google Sign-In SDK Trigger
                Button(
                    onClick = {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)), // Google Blue
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncing
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Masuk dengan Akun Google", color = Color.White)
                }
            } else {
                // User Profile Card and Sync controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = googleName.firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: "U",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(googleName, fontWeight = FontWeight.Bold)
                                Text(googleEmail, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pencadangan Otomatis", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = onlineBackupEnabled,
                                onCheckedChange = {
                                    onlineBackupEnabled = it
                                    settingsManager.setOnlineBackupEnabled(it)
                                    if (it) {
                                        BackupWorker.scheduleAutoBackup(context)
                                        Toast.makeText(context, "Pencadangan otomatis Google Drive aktif!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        BackupWorker.cancelAutoBackup(context)
                                        Toast.makeText(context, "Pencadangan otomatis dinonaktifkan.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        
                        if (lastBackupTime > 0L) {
                            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH.mm", Locale.getDefault())
                            Text(
                                text = "Pencadangan Terakhir: ${dateFormat.format(Date(lastBackupTime))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Text(
                                text = "Belum pernah dicadangkan",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    isSyncing = true
                                    coroutineScope.launch {
                                        try {
                                            val account = GoogleSignIn.getLastSignedInAccount(context)
                                            val driveScope = Scope("https://www.googleapis.com/auth/drive.appdata")
                                            if (account == null || !GoogleSignIn.hasPermissions(account, driveScope)) {
                                                Toast.makeText(context, "Izin Google Drive diperlukan. Mohon pilih akun Anda dan setujui izinnya.", Toast.LENGTH_LONG).show()
                                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                                return@launch
                                            }

                                            if (account.account != null) {
                                                val backupKeyManager = com.philip.keynote.security.BackupKeyManager(context)
                                                val simulatedPass = backupKeyManager.getOrCreateBackupKey()
                                                val outputStream = ByteArrayOutputStream()
                                                val exportResult = backupManager.exportBackup(simulatedPass, outputStream)
                                                if (exportResult.isSuccess) {
                                                    val backupBytes = outputStream.toByteArray()
                                                    val uploadResult = googleDriveBackupHelper.uploadBackup(account.account!!, backupBytes)
                                                    if (uploadResult.isSuccess) {
                                                        val now = System.currentTimeMillis()
                                                        settingsManager.setLastOnlineBackupTime(now)
                                                        lastBackupTime = now
                                                        Toast.makeText(context, "Pencadangan Google Drive berhasil!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                         val exception = uploadResult.exceptionOrNull()
                                                         if (exception is UserRecoverableAuthException) {
                                                             Toast.makeText(context, "Izin Google Drive diperlukan. Mohon setujui permintaan izin yang muncul.", Toast.LENGTH_LONG).show()
                                                             exception.intent?.let { googleSignInLauncher.launch(it) }
                                                         } else if (exception is GoogleDriveAuthException) {
                                                               Toast.makeText(context, "${exception.message}. Mengatur ulang sesi...", Toast.LENGTH_LONG).show()
                                                               googleSignInClient.revokeAccess().addOnCompleteListener {
                                                                   settingsManager.setGoogleSignedIn(false)
                                                                   googleSignedIn = false
                                                                   onlineBackupEnabled = false
                                                                   BackupWorker.cancelAutoBackup(context)
                                                                   googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                                               }
                                                         } else {
                                                             Toast.makeText(context, "Gagal mengunggah: ${exception?.message}", Toast.LENGTH_SHORT).show()
                                                         }
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Gagal mengekspor data: ${exportResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Akun Google tidak ditemukan.", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isSyncing = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isSyncing
                            ) {
                                Text("Cadangkan Sekarang")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    isSyncing = true
                                    coroutineScope.launch {
                                        try {
                                            val account = GoogleSignIn.getLastSignedInAccount(context)
                                            val driveScope = Scope("https://www.googleapis.com/auth/drive.appdata")
                                            if (account == null || !GoogleSignIn.hasPermissions(account, driveScope)) {
                                                Toast.makeText(context, "Izin Google Drive diperlukan. Mohon pilih akun Anda dan setujui izinnya.", Toast.LENGTH_LONG).show()
                                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                                return@launch
                                            }

                                            if (account.account != null) {
                                                val downloadResult = googleDriveBackupHelper.downloadBackup(account.account!!)
                                                if (downloadResult.isSuccess) {
                                                    val backupBytes = downloadResult.getOrNull()
                                                    if (backupBytes != null && backupBytes.isNotEmpty()) {
                                                        val inputStream = ByteArrayInputStream(backupBytes)
                                                        val backupKeyManager = com.philip.keynote.security.BackupKeyManager(context)
                                                        val simulatedPass = backupKeyManager.getOrCreateBackupKey()
                                                        val result = backupManager.importBackup(simulatedPass, inputStream)
                                                        if (result.isSuccess) {
                                                            Toast.makeText(context, "Restore pencadangan online berhasil! Mulai ulang aplikasi.", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(context, "Gagal memulihkan: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Data pencadangan kosong.", Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    val exception = downloadResult.exceptionOrNull()
                                                    if (exception is UserRecoverableAuthException) {
                                                        Toast.makeText(context, "Izin Google Drive diperlukan. Mohon setujui permintaan izin yang muncul.", Toast.LENGTH_LONG).show()
                                                        exception.intent?.let { googleSignInLauncher.launch(it) }
                                                    } else if (exception is GoogleDriveAuthException) {
                                                          Toast.makeText(context, "${exception.message}. Mengatur ulang sesi...", Toast.LENGTH_LONG).show()
                                                          googleSignInClient.revokeAccess().addOnCompleteListener {
                                                             settingsManager.setGoogleSignedIn(false)
                                                             googleSignedIn = false
                                                             onlineBackupEnabled = false
                                                             BackupWorker.cancelAutoBackup(context)
                                                             googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                                         }
                                                    } else {
                                                        Toast.makeText(context, "Gagal memulihkan: ${exception?.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(context, "Akun Google tidak ditemukan.", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isSyncing = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Pulihkan Cadangan")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        TextButton(
                            onClick = {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    settingsManager.setGoogleSignedIn(false)
                                    googleSignedIn = false
                                    onlineBackupEnabled = false
                                    BackupWorker.cancelAutoBackup(context)
                                    Toast.makeText(context, "Keluar dari Akun Google", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Keluar Akun", color = Color.Red)
                        }
                    }
                }
            }

            if (isSyncing) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }


        // Backup Password dialogs (.json name matching)
        if (showBackupDialog) {
            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                title = { Text("Set Password Backup") },
                text = {
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Password Enkripsi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (backupPassword.isNotBlank()) {
                                showBackupDialog = false
                                exportLauncher.launch("keynote_backup_${System.currentTimeMillis()}.json")
                            } else {
                                Toast.makeText(context, "Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Mulai Ekspor")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Masukkan Password Backup") },
                text = {
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Password Enkripsi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (backupPassword.isNotBlank()) {
                                showImportDialog = false
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            } else {
                                Toast.makeText(context, "Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Mulai Impor")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
