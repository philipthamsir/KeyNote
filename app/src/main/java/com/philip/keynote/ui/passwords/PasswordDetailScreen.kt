package com.philip.keynote.ui.passwords

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.philip.keynote.data.local.FieldType
import com.philip.keynote.data.local.PasswordField
import com.philip.keynote.ui.components.SecureScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDetailScreen(
    viewModel: PasswordViewModel,
    passwordId: Long?,
    categoryId: Long,
    initialEditMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val title by viewModel.title.collectAsState()
    val fields by viewModel.fields.collectAsState()

    var isEditMode by remember { mutableStateOf(initialEditMode) }
    var showAddFieldMenu by remember { mutableStateOf(false) }
    var passwordVisibilities by remember { mutableStateOf(mapOf<String, Boolean>()) }
    val coroutineScope = rememberCoroutineScope()

    SecureScreen()

    LaunchedEffect(passwordId, categoryId) {
        viewModel.loadPassword(passwordId, categoryId)
    }

    BackHandler(enabled = true) {
        if (isEditMode) {
            viewModel.savePassword {
                isEditMode = false
            }
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Akun" else "Detail Akun") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            viewModel.savePassword { isEditMode = false }
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (isEditMode) {
                        if (passwordId != null && passwordId != 0L) {
                            IconButton(onClick = {
                                viewModel.deletePassword(passwordId) {
                                    Toast.makeText(context, "Akun dihapus", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Akun", tint = Color.Red)
                            }
                        }
                        IconButton(onClick = {
                            viewModel.savePassword { isEditMode = false }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Selesai Edit")
                        }
                    } else {
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Mulai Edit")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Title Input aligned to theme colors
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateTitle(it) },
                readOnly = !isEditMode,
                label = { Text("Nama Layanan / Judul") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Informasi Akun",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isEditMode) {
                    Box {
                        IconButton(onClick = { showAddFieldMenu = true }) {
                            Icon(
                                Icons.Default.AddCircle,
                                contentDescription = "Tambah Field",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showAddFieldMenu,
                            onDismissRequest = { showAddFieldMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Field Identitas (Username/Email)") },
                                onClick = {
                                    showAddFieldMenu = false
                                    viewModel.addCustomField(FieldType.IDENTIFIER)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Field Password") },
                                onClick = {
                                    showAddFieldMenu = false
                                    viewModel.addCustomField(FieldType.PASSWORD)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Field PIN") },
                                onClick = {
                                    showAddFieldMenu = false
                                    viewModel.addCustomField(FieldType.PIN)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Field URL") },
                                onClick = {
                                    showAddFieldMenu = false
                                    viewModel.addCustomField(FieldType.URL)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Field Kustom") },
                                onClick = {
                                    showAddFieldMenu = false
                                    viewModel.addCustomField(FieldType.CUSTOM)
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(fields) { field ->
                    CredentialFieldItem(
                        field = field,
                        isEditMode = isEditMode,
                        isPasswordVisible = passwordVisibilities[field.id] ?: false,
                        onVisibilityToggle = {
                            val current = passwordVisibilities[field.id] ?: false
                            passwordVisibilities = passwordVisibilities + (field.id to !current)
                        },
                        onValueChange = { valStr ->
                            viewModel.updateField(field.id, valStr)
                        },
                        onLabelChange = { labelStr ->
                            viewModel.updateField(field.id, field.value, labelStr)
                        },
                        onRemove = {
                            viewModel.removeField(field.id)
                        },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(field.label, field.value)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "${field.label} berhasil disalin!", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                delay(30_000)
                                if (clipboard.primaryClip?.getItemAt(0)?.text == field.value) {
                                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CredentialFieldItem(
    field: PasswordField,
    isEditMode: Boolean,
    isPasswordVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    onValueChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onRemove: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (field.type == FieldType.CUSTOM && isEditMode) {
                    TextField(
                        value = field.label,
                        onValueChange = onLabelChange,
                        textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.weight(1f).height(44.dp)
                    )
                } else {
                    Text(
                        text = field.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (isEditMode) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Cancel, contentDescription = "Hapus Field", tint = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val visualTransformation = if (field.type == FieldType.PASSWORD && !isPasswordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                }

                val keyboardOptions = when (field.type) {
                    FieldType.PIN -> KeyboardOptions(keyboardType = KeyboardType.Number)
                    FieldType.URL -> KeyboardOptions(keyboardType = KeyboardType.Uri)
                    else -> KeyboardOptions.Default
                }

                TextField(
                    value = field.value,
                    onValueChange = onValueChange,
                    readOnly = !isEditMode,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    placeholder = { Text("Masukkan nilai...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (field.type == FieldType.PASSWORD) {
                    IconButton(onClick = onVisibilityToggle) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Visibility"
                        )
                    }
                }

                if (field.value.isNotEmpty()) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Cepat"
                        )
                    }
                }
            }
        }
    }
}
