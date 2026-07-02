package com.philip.keynote.ui.passwords

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.philip.keynote.data.local.FieldType
import com.philip.keynote.data.local.PasswordField
import com.philip.keynote.data.local.entity.CategoryEntity
import com.philip.keynote.data.local.entity.PasswordEntity
import com.philip.keynote.data.repository.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PasswordViewModel(
    private val repository: PasswordRepository,
    context: Context
) : ViewModel() {

    private val sharedPrefs = try {
        createEncryptedSharedPreferences(context)
    } catch (e: Exception) {
        e.printStackTrace()
        // Recovery: wipe corrupted prefs and KeyStore entry
        try {
            context.deleteSharedPreferences("keynote_pass_manager_prefs")
            context.deleteSharedPreferences("__androidx_security_crypto_encrypted_shared_preferences_keyset__")
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry("_androidx_security_crypto_encrypted_shared_preferences_keyset_")
        } catch (recoveryEx: Exception) {
            recoveryEx.printStackTrace()
        }
        // Retry creation
        try {
            createEncryptedSharedPreferences(context)
        } catch (retryEx: Exception) {
            // Last resort: fallback to standard SharedPreferences to prevent crash
            context.getSharedPreferences("keynote_pass_manager_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    private fun createEncryptedSharedPreferences(context: Context): android.content.SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            "keynote_pass_manager_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_MASTER_PIN = "master_pin"
    }

    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _passwords = MutableStateFlow<List<PasswordEntity>>(emptyList())
    val passwords = _passwords.asStateFlow()

    // Editor State
    private val _editPasswordId = MutableStateFlow<Long?>(null)
    val editPasswordId = _editPasswordId.asStateFlow()

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _fields = MutableStateFlow<List<PasswordField>>(emptyList())
    val fields = _fields.asStateFlow()

    init {
        // Automatically pre-populate default categories if none exist
        viewModelScope.launch {
            val currentList = repository.categories.first()
            if (currentList.isEmpty()) {
                repository.insertCategory(CategoryEntity(name = "Google Akun", iconName = "Google"))
                repository.insertCategory(CategoryEntity(name = "Media Sosial", iconName = "Share"))
                repository.insertCategory(CategoryEntity(name = "Perbankan", iconName = "AccountBalance"))
                repository.insertCategory(CategoryEntity(name = "Lainnya", iconName = "Folder"))
            }
        }
    }

    fun selectCategory(categoryId: Long) {
        _selectedCategoryId.value = categoryId
        viewModelScope.launch {
            repository.getPasswordsByCategory(categoryId).collect {
                _passwords.value = it
            }
        }
    }

    // Master PIN management
    fun isPinSetup(): Boolean {
        return sharedPrefs.getString(KEY_MASTER_PIN, null) != null
    }

    fun setupMasterPin(pin: String): Boolean {
        if (pin.length < 4) return false
        sharedPrefs.edit().putString(KEY_MASTER_PIN, pin).apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedPin = sharedPrefs.getString(KEY_MASTER_PIN, null)
        return storedPin == pin
    }

    // Category Operations
    fun addCategory(name: String, iconName: String = "Folder") {
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name = name, iconName = iconName))
        }
    }

    fun updateCategory(category: CategoryEntity, newName: String, newIconName: String) {
        viewModelScope.launch {
            repository.updateCategory(category.copy(name = newName, iconName = newIconName))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Password Entity Management
    fun loadPassword(id: Long?, defaultCategoryId: Long) {
        if (id == null || id == 0L) {
            _editPasswordId.value = null
            _title.value = ""
            _selectedCategoryId.value = defaultCategoryId
            _fields.value = listOf(
                PasswordField(UUID.randomUUID().toString(), FieldType.IDENTIFIER, "Username/Email", ""),
                PasswordField(UUID.randomUUID().toString(), FieldType.PASSWORD, "Password", "")
            )
            return
        }
        _editPasswordId.value = id
        viewModelScope.launch {
            repository.getPasswordById(id)?.let { pass ->
                _title.value = pass.title
                _selectedCategoryId.value = pass.categoryId
                _fields.value = pass.fields
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun addCustomField(type: FieldType) {
        val label = when (type) {
            FieldType.IDENTIFIER -> "Identitas Baru"
            FieldType.PASSWORD -> "Password Baru"
            FieldType.PIN -> "PIN"
            FieldType.URL -> "URL Link"
            FieldType.CUSTOM -> "Field Kustom"
        }
        val newField = PasswordField(UUID.randomUUID().toString(), type, label, "")
        _fields.value = _fields.value + newField
    }

    fun updateField(id: String, value: String, label: String = "") {
        _fields.value = _fields.value.map {
            if (it.id == id) {
                it.copy(
                    value = value,
                    label = if (label.isNotEmpty()) label else it.label
                )
            } else it
        }
    }

    fun removeField(id: String) {
        _fields.value = _fields.value.filter { it.id != id }
    }

    fun savePassword(onComplete: () -> Unit = {}) {
        val categoryId = _selectedCategoryId.value ?: return
        viewModelScope.launch {
            val time = System.currentTimeMillis()
            val currentId = _editPasswordId.value
            if (currentId == null || currentId == 0L) {
                val newPass = PasswordEntity(
                    categoryId = categoryId,
                    title = _title.value.ifEmpty { "Akun Tanpa Judul" },
                    fields = _fields.value,
                    createdAt = time,
                    updatedAt = time
                )
                repository.insertPassword(newPass)
            } else {
                repository.getPasswordById(currentId)?.let { existing ->
                    val updatedPass = existing.copy(
                        categoryId = categoryId,
                        title = _title.value.ifEmpty { "Akun Tanpa Judul" },
                        fields = _fields.value,
                        updatedAt = time
                    )
                    repository.updatePassword(updatedPass)
                }
            }
            onComplete()
        }
    }

    fun deletePassword(id: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.getPasswordById(id)?.let {
                repository.deletePassword(it)
            }
            onComplete()
        }
    }
}
