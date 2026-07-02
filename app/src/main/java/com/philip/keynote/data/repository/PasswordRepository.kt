package com.philip.keynote.data.repository

import com.philip.keynote.data.local.dao.PasswordDao
import com.philip.keynote.data.local.entity.CategoryEntity
import com.philip.keynote.data.local.entity.PasswordEntity
import kotlinx.coroutines.flow.Flow

class PasswordRepository(private val passwordDao: PasswordDao) {
    val categories: Flow<List<CategoryEntity>> = passwordDao.getAllCategories()

    fun getPasswordsByCategory(categoryId: Long): Flow<List<PasswordEntity>> =
        passwordDao.getPasswordsByCategory(categoryId)

    suspend fun getPasswordById(id: Long): PasswordEntity? =
        passwordDao.getPasswordById(id)

    suspend fun insertCategory(category: CategoryEntity): Long =
        passwordDao.insertCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) =
        passwordDao.deleteCategory(category)

    suspend fun updateCategory(category: CategoryEntity) =
        passwordDao.updateCategory(category)

    suspend fun insertPassword(password: PasswordEntity): Long =
        passwordDao.insertPassword(password)

    suspend fun updatePassword(password: PasswordEntity) =
        passwordDao.updatePassword(password)

    suspend fun deletePassword(password: PasswordEntity) =
        passwordDao.deletePassword(password)
}
