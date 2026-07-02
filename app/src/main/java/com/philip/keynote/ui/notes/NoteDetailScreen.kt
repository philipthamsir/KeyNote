package com.philip.keynote.ui.notes

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philip.keynote.data.local.BlockType
import com.philip.keynote.data.local.ChecklistItem
import com.philip.keynote.data.local.NoteBlock
import com.philip.keynote.data.local.TableData
import com.philip.keynote.data.settings.SettingsManager
import com.philip.keynote.ui.components.notebookLines
import com.philip.keynote.ui.theme.Localization
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel,
    noteId: Long?,
    initialEditMode: Boolean,
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val title by viewModel.title.collectAsState()
    val backgroundColor by viewModel.backgroundColor.collectAsState()
    val blocks by viewModel.blocks.collectAsState()

    var isEditMode by remember { mutableStateOf(initialEditMode) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showInsertFABMenu by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    var activeFormattingBlockId by remember { mutableStateOf<String?>(null) }
    var showFormattingBar by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    // Auto-Save Scheduler Loop
    val autoSaveEnabled = settingsManager.isAutoSaveEnabled()
    val autoSaveInterval = settingsManager.getAutoSaveInterval()
    if (autoSaveEnabled && isEditMode) {
        LaunchedEffect(autoSaveInterval) {
            while (true) {
                delay(autoSaveInterval)
                viewModel.saveNote()
            }
        }
    }

    // Back button interceptor
    BackHandler(enabled = true) {
        val isTitleEmpty = title.trim().isEmpty()
        val isContentEmpty = blocks.all { it.type == BlockType.TEXT && it.textContent.trim().isEmpty() }
        if (isEditMode) {
            viewModel.saveNote {
                if (isTitleEmpty && isContentEmpty) {
                    Toast.makeText(context, "Catatan kosong dibuang", Toast.LENGTH_SHORT).show()
                }
                isEditMode = false
            }
        } else {
            onBack()
        }
    }

    // Dynamic Theme Adaptation Logic
    val isSystemDark = isSystemInDarkTheme()
    val isDefaultBgColor = backgroundColor == 0xFFFFFFFF.toInt() || backgroundColor == 0xFF121212.toInt()
    
    val noteBgColor = if (isDefaultBgColor) {
        if (isSystemDark) Color(0xFF121212) else Color(0xFFFFFFFF)
    } else {
        Color(backgroundColor)
    }

    val contentTextColor = if (isDefaultBgColor) {
        if (isSystemDark) Color.White else Color.Black
    } else {
        if (backgroundColor == 0xFF121212.toInt()) Color.White else Color.Black
    }

    // Configure lined background colors to match selected theme
    val isLinedBgDark = isSystemDark || (backgroundColor == 0xFF121212.toInt())
    val linedBgColor = if (isLinedBgDark) Color.White.copy(alpha = 0.12f) else Color.LightGray.copy(alpha = 0.5f)
    val linedMarginColor = Color.Transparent

    // Map note color to corresponding top bar accent colors
    val topBarColor = when (backgroundColor) {
        0xFFEF5350.toInt() -> Color(0xFFD32F2F) // Red -> Dark Red
        0xFFFF9800.toInt() -> Color(0xFFE65100) // Orange -> Dark Orange
        0xFFFFEE58.toInt() -> Color(0xFFFBC02D) // Yellow -> Dark Yellow
        0xFF4CAF50.toInt() -> Color(0xFF2E7D32) // Green -> Dark Green
        0xFF2196F3.toInt() -> Color(0xFF1565C0) // Blue -> Dark Blue
        0xFFAB47BC.toInt() -> Color(0xFF6A1B9A) // Purple -> Dark Purple
        0xFF121212.toInt() -> Color(0xFF212121) // Black -> Charcoal
        0xFF8E8E93.toInt() -> Color(0xFF555558) // Gray -> Dark Gray
        else -> {
            if (isSystemDark) Color(0xFF212121) else Color(0xFFE0E0E0)
        }
    }

    val topBarContentColor = if (backgroundColor == 0xFFFFFFFF.toInt() && !isSystemDark) Color.Black else Color.White

    // Format date string for status bar
    val formattedDate = remember(noteId) {
        SimpleDateFormat("dd/MM/yy HH.mm", Locale.getDefault()).format(Date())
    }

    val textColors = listOf(
        0xFFFFFFFF.toInt(), // White
        0xFF000000.toInt(), // Black
        0xFFEF5350.toInt(), // Red
        0xFF4CAF50.toInt(), // Green
        0xFF2196F3.toInt(), // Blue
        0xFFFFEE58.toInt()  // Yellow
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // White card title text field inside Top Bar
                    Card(
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(end = 8.dp),
                        border = if (isEditMode) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800)) else null
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = title,
                                onValueChange = { viewModel.updateTitle(it) },
                                readOnly = !isEditMode,
                                textStyle = TextStyle(
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                cursorBrush = SolidColor(Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (title.isEmpty()) {
                                Text("Judul", color = Color.Gray.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            // Transparent overlay to safely capture double click gestures in Read Mode
                            if (!isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Transparent)
                                        .combinedClickable(
                                            onClick = {},
                                            onDoubleClick = { if (!isEditMode) isEditMode = true }
                                        )
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val isTitleEmpty = title.trim().isEmpty()
                        val isContentEmpty = blocks.all { it.type == BlockType.TEXT && it.textContent.trim().isEmpty() }
                        viewModel.saveNote {
                            if (isTitleEmpty && isContentEmpty) {
                                Toast.makeText(context, "Catatan kosong dibuang", Toast.LENGTH_SHORT).show()
                            }
                            if (isEditMode) {
                                isEditMode = false
                            } else {
                                onBack()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Selesai/Kembali", tint = topBarContentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor),
                actions = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(backgroundColor))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .clickable {
                                if (isEditMode) {
                                    showColorPicker = !showColorPicker
                                }
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = topBarContentColor)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Kirim Catatan") },
                            onClick = {
                                showMenu = false
                                shareNoteFromDetail(context, title, blocks)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pengingat (Reminder)") },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Fitur Pengingat diaktifkan", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (isEditMode) {
                Box {
                    FloatingActionButton(onClick = { showInsertFABMenu = !showInsertFABMenu }) {
                        Icon(
                            if (showInsertFABMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Tambah Elemen Media"
                        )
                    }

                    if (showInsertFABMenu) {
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 72.dp)
                                .width(180.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                TextButton(
                                    onClick = {
                                        showInsertFABMenu = false
                                        viewModel.addBlock(BlockType.TEXT)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Notes, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Teks Catatan")
                                }
                                TextButton(
                                    onClick = {
                                        showInsertFABMenu = false
                                        viewModel.addBlock(BlockType.TABLE)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sisipkan Tabel")
                                }
                                TextButton(
                                    onClick = {
                                        showInsertFABMenu = false
                                        viewModel.addBlock(BlockType.CHECKLIST)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FormatListBulleted, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Daftar Centang")
                                }
                                TextButton(
                                    onClick = {
                                        showInsertFABMenu = false
                                        viewModel.addBlock(BlockType.IMAGE)
                                        Toast.makeText(context, "Ketuk tombol 'Tempel' di gambar block baru", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tempel Gambar")
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = noteBgColor,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Color Selector Grid
            if (showColorPicker && isEditMode) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pilih Warna Latar Catatan:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFEF5350)).clickable { viewModel.updateBackgroundColor(0xFFEF5350.toInt()) })
                            Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF9800)).clickable { viewModel.updateBackgroundColor(0xFFFF9800.toInt()) })
                            Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFFEE58)).clickable { viewModel.updateBackgroundColor(0xFFFFEE58.toInt()) })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF4CAF50)).clickable { viewModel.updateBackgroundColor(0xFF4CAF50.toInt()) })
                            Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2196F3)).clickable { viewModel.updateBackgroundColor(0xFF2196F3.toInt()) })
                            Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFAB47BC)).clickable { viewModel.updateBackgroundColor(0xFFAB47BC.toInt()) })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF121212)).clickable { viewModel.updateBackgroundColor(0xFF121212.toInt()) })
                            Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF8E8E93)).clickable { viewModel.updateBackgroundColor(0xFF8E8E93.toInt()) })
                            Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFFFFFF)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).clickable { viewModel.updateBackgroundColor(0xFFFFFFFF.toInt()) })
                        }
                    }
                }
            }

            // Edit Timestamp Subbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(noteBgColor)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Suntingan", style = MaterialTheme.typography.bodySmall, color = contentTextColor.copy(alpha = 0.6f))
                Text(formattedDate, style = MaterialTheme.typography.bodySmall, color = contentTextColor.copy(alpha = 0.6f))
            }

            // Note Editor Core Lined Background Container
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .notebookLines(lineHeight = 32.dp, lineColor = linedBgColor, marginColor = linedMarginColor)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(blocks, key = { it.id }) { block ->
                    NoteBlockItem(
                        block = block,
                        isEditMode = isEditMode,
                        noteBgColor = noteBgColor,
                        contentTextColor = contentTextColor,
                        onBlockClick = {
                            if (isEditMode) {
                                activeFormattingBlockId = block.id
                                showFormattingBar = block.type == BlockType.TEXT
                            }
                        },
                        onEnterEditMode = {
                            if (!isEditMode) {
                                isEditMode = true
                            }
                        },
                        onContentChange = { newText ->
                            viewModel.updateTextBlockContent(block.id, newText)
                        },
                        onDeleteBlock = {
                            viewModel.deleteBlock(block.id)
                            if (activeFormattingBlockId == block.id) {
                                showFormattingBar = false
                            }
                        },
                        onAddChecklistItem = { viewModel.addChecklistItem(block.id) },
                        onUpdateChecklistItem = { itemId, itemText, isChecked ->
                            viewModel.updateChecklistItem(block.id, itemId, itemText, isChecked)
                        },
                        onRemoveChecklistItem = { itemId -> viewModel.removeChecklistItem(block.id, itemId) },
                        onMoveChecklistItemUp = { itemId -> viewModel.moveChecklistItemUp(block.id, itemId) },
                        onMoveChecklistItemDown = { itemId -> viewModel.moveChecklistItemDown(block.id, itemId) },
                        onAddTableColumn = { viewModel.addTableColumn(block.id) },
                        onAddTableRow = { viewModel.addTableRow(block.id) },
                        onUpdateTableCell = { row, col, valStr ->
                            viewModel.updateTableCell(block.id, row, col, valStr)
                        },
                        onRemoveTableRow = { row -> viewModel.removeTableRow(block.id, row) },
                        onUpdateImageUri = { uri -> viewModel.updateImageUri(block.id, uri) }
                    )
                }
            }

            // Bottom Formatting Bar
            if (showFormattingBar && activeFormattingBlockId != null && isEditMode) {
                val block = blocks.firstOrNull { it.id == activeFormattingBlockId }
                if (block != null && block.type == BlockType.TEXT) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    val newStyle = when (block.fontStyle) {
                                        "BOLD" -> "NORMAL"
                                        "ITALIC" -> "BOLD_ITALIC"
                                        "BOLD_ITALIC" -> "ITALIC"
                                        else -> "BOLD"
                                    }
                                    viewModel.updateBlockFormatting(block.id, fontStyle = newStyle)
                                }) {
                                    Icon(
                                        Icons.Default.FormatBold,
                                        contentDescription = "Tebal",
                                        tint = if (block.fontStyle.contains("BOLD")) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    )
                                }
                                IconButton(onClick = {
                                    val newStyle = when (block.fontStyle) {
                                        "ITALIC" -> "NORMAL"
                                        "BOLD" -> "BOLD_ITALIC"
                                        "BOLD_ITALIC" -> "BOLD"
                                        else -> "ITALIC"
                                    }
                                    viewModel.updateBlockFormatting(block.id, fontStyle = newStyle)
                                }) {
                                    Icon(
                                        Icons.Default.FormatItalic,
                                        contentDescription = "Miring",
                                        tint = if (block.fontStyle.contains("ITALIC")) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    )
                                }
                                IconButton(onClick = { viewModel.updateBlockFormatting(block.id, textAlignment = "LEFT") }) {
                                    Icon(
                                        Icons.Default.FormatAlignLeft,
                                        contentDescription = "Rata Kiri",
                                        tint = if (block.textAlignment == "LEFT") MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    )
                                }
                                IconButton(onClick = { viewModel.updateBlockFormatting(block.id, textAlignment = "CENTER") }) {
                                    Icon(
                                        Icons.Default.FormatAlignCenter,
                                        contentDescription = "Rata Tengah",
                                        tint = if (block.textAlignment == "CENTER") MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    )
                                }
                                IconButton(onClick = { viewModel.updateBlockFormatting(block.id, textAlignment = "JUSTIFIED") }) {
                                    Icon(
                                        Icons.Default.FormatAlignJustify,
                                        contentDescription = "Rata Kanan Kiri",
                                        tint = if (block.textAlignment == "JUSTIFIED") MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    )
                                }
                                IconButton(onClick = {
                                    val nextSize = if (block.fontSize < 30f) block.fontSize + 2f else block.fontSize
                                    viewModel.updateBlockFormatting(block.id, fontSize = nextSize)
                                }) {
                                    Icon(Icons.Default.TextIncrease, contentDescription = "Perbesar Font")
                                }
                                IconButton(onClick = {
                                    val nextSize = if (block.fontSize > 10f) block.fontSize - 2f else block.fontSize
                                    viewModel.updateBlockFormatting(block.id, fontSize = nextSize)
                                }) {
                                    Icon(Icons.Default.TextDecrease, contentDescription = "Perkecil Font")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Warna Teks:", style = MaterialTheme.typography.bodySmall)
                                textColors.forEach { colorVal ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorVal))
                                            .border(
                                                width = if (block.fontColor == colorVal) 2.dp else 1.dp,
                                                color = if (block.fontColor == colorVal) MaterialTheme.colorScheme.primary else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                viewModel.updateBlockFormatting(block.id, fontColor = colorVal)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteBlockItem(
    block: NoteBlock,
    isEditMode: Boolean,
    noteBgColor: Color,
    contentTextColor: Color,
    onBlockClick: () -> Unit,
    onEnterEditMode: () -> Unit,
    onContentChange: (String) -> Unit,
    onDeleteBlock: () -> Unit,
    onAddChecklistItem: () -> Unit,
    onUpdateChecklistItem: (String, String, Boolean) -> Unit,
    onRemoveChecklistItem: (String) -> Unit,
    onMoveChecklistItemUp: (String) -> Unit,
    onMoveChecklistItemDown: (String) -> Unit,
    onAddTableColumn: () -> Unit,
    onAddTableRow: () -> Unit,
    onUpdateTableCell: (Int, Int, String) -> Unit,
    onRemoveTableRow: (Int) -> Unit,
    onUpdateImageUri: (String) -> Unit
) {
    val context = LocalContext.current

    when (block.type) {
        BlockType.TEXT -> {
            val fSize = block.fontSize.sp
            val fStyle = if (block.fontStyle.contains("ITALIC")) FontStyle.Italic else FontStyle.Normal
            val fWeight = if (block.fontStyle.contains("BOLD")) FontWeight.Bold else FontWeight.Normal
            val fAlignment = when (block.textAlignment) {
                "CENTER" -> TextAlign.Center
                "JUSTIFIED" -> TextAlign.Justify
                else -> TextAlign.Left
            }
            val fColor = if (block.fontColor == 0xFF000000.toInt() && contentTextColor == Color.White) {
                Color.White
            } else if (block.fontColor == 0xFFFFFFFF.toInt() && contentTextColor == Color.Black) {
                Color.Black
            } else {
                Color(block.fontColor)
            }

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = block.textContent,
                    onValueChange = onContentChange,
                    readOnly = !isEditMode,
                    textStyle = TextStyle(
                        fontSize = fSize,
                        fontStyle = fStyle,
                        fontWeight = fWeight,
                        textAlign = fAlignment,
                        color = fColor,
                        lineHeight = 32.sp
                    ),
                    cursorBrush = SolidColor(contentTextColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                )

                if (block.textContent.isEmpty()) {
                    Text(
                        text = "Menulis teks di sini...",
                        style = TextStyle(
                            fontSize = fSize,
                            fontStyle = fStyle,
                            color = contentTextColor.copy(alpha = 0.4f),
                            lineHeight = 32.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                
                // Transparent overlay to safely capture double click gestures in Read Mode
                if (!isEditMode) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Transparent)
                            .combinedClickable(
                                onClick = onBlockClick,
                                onDoubleClick = { if (!isEditMode) onEnterEditMode() }
                            )
                    )
                }

                if (isEditMode) {
                    IconButton(
                        onClick = onDeleteBlock,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus Blok Teks", tint = contentTextColor.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        BlockType.CHECKLIST -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(noteBgColor)
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Daftar Centang", fontWeight = FontWeight.Bold, color = contentTextColor)
                            if (isEditMode) {
                                IconButton(onClick = onDeleteBlock, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Blok", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        block.checklistItems.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    enabled = isEditMode,
                                    onCheckedChange = { isChecked ->
                                        onUpdateChecklistItem(item.id, item.text, isChecked)
                                    }
                                )
                                TextField(
                                    value = item.text,
                                    onValueChange = { text ->
                                        onUpdateChecklistItem(item.id, text, item.isChecked)
                                    },
                                    readOnly = !isEditMode,
                                    placeholder = { Text("Item...") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = contentTextColor,
                                        unfocusedTextColor = contentTextColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (isEditMode) {
                                    IconButton(onClick = { onMoveChecklistItemUp(item.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Ke atas", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { onMoveChecklistItemDown(item.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Ke bawah", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { onRemoveChecklistItem(item.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Hapus", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        if (isEditMode) {
                            TextButton(onClick = onAddChecklistItem, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah Barang")
                            }
                        }
                    }
                }
            }
        }
        BlockType.TABLE -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(noteBgColor)
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tabel Data", fontWeight = FontWeight.Bold, color = contentTextColor)
                            Row {
                                if (isEditMode) {
                                    TextButton(onClick = onAddTableColumn) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Kolom", style = MaterialTheme.typography.bodySmall)
                                    }
                                    TextButton(onClick = onAddTableRow) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Baris", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = onDeleteBlock, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus Blok", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, contentTextColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(contentTextColor.copy(alpha = 0.05f))
                                    .padding(4.dp)
                            ) {
                                block.tableData.headers.forEach { header ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = header,
                                            fontWeight = FontWeight.Bold,
                                            color = contentTextColor,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                if (isEditMode) {
                                    Spacer(modifier = Modifier.width(36.dp))
                                }
                            }

                            block.tableData.rows.forEachIndexed { rIdx, row ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    row.forEachIndexed { cIdx, cell ->
                                        TextField(
                                            value = cell,
                                            onValueChange = { onUpdateTableCell(rIdx, cIdx, it) },
                                            readOnly = !isEditMode,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedTextColor = contentTextColor,
                                                unfocusedTextColor = contentTextColor
                                            ),
                                            textStyle = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(2.dp)
                                        )
                                    }
                                    if (isEditMode) {
                                        IconButton(
                                            onClick = { onRemoveTableRow(rIdx) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.RemoveCircleOutline,
                                                contentDescription = "Hapus Baris",
                                                tint = Color.Red.copy(alpha = 0.4f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        BlockType.IMAGE -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(noteBgColor)
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gambar Media", fontWeight = FontWeight.Bold, color = contentTextColor)
                            if (isEditMode) {
                                IconButton(onClick = onDeleteBlock, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Blok", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (block.imageUri.isEmpty()) {
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                    .height(120.dp)
                                    .border(1.dp, contentTextColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .clickable {
                                        pasteImageFromClipboard(context) { successUri ->
                                            onUpdateImageUri(successUri)
                                        }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = contentTextColor.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tempel URI Gambar dari Clipboard", color = contentTextColor.copy(alpha = 0.6f))
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Gambar tidak tersedia", color = contentTextColor.copy(alpha = 0.5f))
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Path: ${block.imageUri}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentTextColor.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                if (isEditMode) {
                                    Button(onClick = { onUpdateImageUri("") }) {
                                        Text("Hapus Gambar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun pasteImageFromClipboard(context: Context, onResult: (String) -> Unit) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    if (!clipboard.hasPrimaryClip()) {
        Toast.makeText(context, "Clipboard kosong.", Toast.LENGTH_SHORT).show()
        return
    }
    val clip = clipboard.primaryClip
    if (clip != null && clip.itemCount > 0) {
        val item = clip.getItemAt(0)
        val uri = item.uri
        if (uri != null) {
            onResult(uri.toString())
            Toast.makeText(context, "Gambar berhasil ditempel!", Toast.LENGTH_SHORT).show()
        } else if (item.text != null) {
            onResult(item.text.toString())
            Toast.makeText(context, "Teks URI berhasil ditempel!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Format clipboard tidak didukung.", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Tidak ada item untuk ditempel.", Toast.LENGTH_SHORT).show()
    }
}

private fun shareNoteFromDetail(context: android.content.Context, title: String, blocks: List<NoteBlock>) {
    val gson = com.google.gson.Gson()
    val contentJson = gson.toJson(blocks)
    shareNoteLocal(context, title, contentJson)
}

private fun shareNoteLocal(context: android.content.Context, title: String, content: String) {
    val builder = java.lang.StringBuilder()
    builder.append("=== ").append(title).append(" ===\n\n")
    
    if (content.startsWith("[")) {
        try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<NoteBlock>>() {}.type
            val blocks: List<NoteBlock> = gson.fromJson(content, type) ?: emptyList()
            
            blocks.forEach { block ->
                when (block.type) {
                    BlockType.TEXT -> {
                        if (block.textContent.isNotEmpty()) {
                            builder.append(block.textContent).append("\n\n")
                        }
                    }
                    BlockType.CHECKLIST -> {
                        block.checklistItems.forEach { item ->
                            val status = if (item.isChecked) "[x]" else "[ ]"
                            builder.append("$status ${item.text}\n")
                        }
                        builder.append("\n")
                    }
                    BlockType.TABLE -> {
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
                    BlockType.IMAGE -> {
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
