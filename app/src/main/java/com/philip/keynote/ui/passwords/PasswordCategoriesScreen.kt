package com.philip.keynote.ui.passwords

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.philip.keynote.data.local.entity.CategoryEntity
import com.philip.keynote.ui.components.SecureScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordCategoriesScreen(
    viewModel: PasswordViewModel,
    isDarkTheme: Boolean,
    onCategoryClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    SecureScreen()
    val categories by viewModel.categories.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    var selectedCategoryForMenu by remember { mutableStateOf<CategoryEntity?>(null) }
    
    var newCategoryName by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("Folder") }

    val themeAccentColor = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF1976D2)

    val availableIcons = listOf(
        "Folder" to Icons.Default.Folder,
        "Language" to Icons.Default.Language,
        "Share" to Icons.Default.Share,
        "AccountBalance" to Icons.Default.AccountBalance,
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "Gamepad" to Icons.Default.Gamepad,
        "Email" to Icons.Default.Email,
        "Work" to Icons.Default.Work,
        "VpnKey" to Icons.Default.VpnKey
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        newCategoryName = ""
                        selectedIconName = "Folder"
                        showAddDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Kategori")
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
            Text(
                text = "Kelompok Akun Terenkripsi",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada kategori.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(categories, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            accentColor = themeAccentColor,
                            onClick = { onCategoryClick(category.id) },
                            onLongClick = { selectedCategoryForMenu = category }
                        )
                    }
                }
            }
        }

        // Long Press Context Menu Dialog
        if (selectedCategoryForMenu != null && !showEditDialog && !showDeleteConfirmDialog) {
            val category = selectedCategoryForMenu!!
            AlertDialog(
                onDismissRequest = { selectedCategoryForMenu = null },
                title = { Text("Pilihan Kategori: ${category.name}") },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                newCategoryName = category.name
                                selectedIconName = category.iconName
                                showEditDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Sunting Kategori", style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                showDeleteConfirmDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Hapus Kategori", style = MaterialTheme.typography.bodyLarge, color = Color.Red)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedCategoryForMenu = null }) {
                        Text("Tutup")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog && selectedCategoryForMenu != null) {
            val category = selectedCategoryForMenu!!
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Hapus Kategori?") },
                text = {
                    Text("Apakah Anda yakin ingin menghapus kategori \"${category.name}\"? Seluruh akun di dalamnya akan ikut terhapus secara permanen.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCategory(category)
                            showDeleteConfirmDialog = false
                            selectedCategoryForMenu = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Add Category Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Tambah Kategori Baru") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Nama Kategori") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                        
                        Text("Pilih Ikon Kategori:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Render 3x3 Grid of Clickable Category Icons
                        val chunked = availableIcons.chunked(3)
                        chunked.forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                rowIcons.forEach { (iconName, iconVector) ->
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedIconName == iconName) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(
                                                width = if (selectedIconName == iconName) 2.2.dp else 1.dp,
                                                color = if (selectedIconName == iconName) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedIconName = iconName },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = if (selectedIconName == iconName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.addCategory(newCategoryName, selectedIconName)
                                newCategoryName = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Tambah")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Edit Category Dialog
        if (showEditDialog && selectedCategoryForMenu != null) {
            val category = selectedCategoryForMenu!!
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Sunting Kategori") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Nama Kategori") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                        
                        Text("Pilih Ikon Kategori:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Render 3x3 Grid of Clickable Category Icons
                        val chunked = availableIcons.chunked(3)
                        chunked.forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                rowIcons.forEach { (iconName, iconVector) ->
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedIconName == iconName) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(
                                                width = if (selectedIconName == iconName) 2.2.dp else 1.dp,
                                                color = if (selectedIconName == iconName) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedIconName = iconName },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = if (selectedIconName == iconName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.updateCategory(category, newCategoryName, selectedIconName)
                                showEditDialog = false
                                selectedCategoryForMenu = null
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEditDialog = false
                        selectedCategoryForMenu = null
                    }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryCard(
    category: CategoryEntity,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val icon = when (category.iconName) {
                "Language" -> Icons.Default.Language
                "Share" -> Icons.Default.Share
                "AccountBalance" -> Icons.Default.AccountBalance
                "ShoppingCart" -> Icons.Default.ShoppingCart
                "Gamepad" -> Icons.Default.Gamepad
                "Email" -> Icons.Default.Email
                "Work" -> Icons.Default.Work
                "VpnKey" -> Icons.Default.VpnKey
                else -> Icons.Default.Folder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
