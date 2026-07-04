package com.philip.keynote.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val trashNotes by viewModel.trashNotes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tong Sampah") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (trashNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Tong sampah kosong.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trashNotes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(note.backgroundColor))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val contentColor = if (note.backgroundColor == 0xFFFFFFFF.toInt()) Color.Black else Color.DarkGray
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                                Text(
                                    text = "Dihapus pada: ${dateFormat.format(Date(note.updatedAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.7f)
                                )
                            }
                            Row {
                                IconButton(onClick = { viewModel.restoreFromTrash(note) }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Pulihkan", tint = contentColor)
                                }
                                IconButton(onClick = { viewModel.deletePermanently(note) }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Hapus Selamanya", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
