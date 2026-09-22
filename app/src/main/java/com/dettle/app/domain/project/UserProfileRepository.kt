package com.dettle.app.domain.project

import com.dettle.app.data.db.dao.UserProfileDao
import com.dettle.app.data.db.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao
) {
    fun observe(): Flow<UserProfileEntity?> = dao.observe()

    suspend fun get(): UserProfileEntity? = dao.get()

    suspend fun getOrCreate(): UserProfileEntity {
        return dao.get() ?: UserProfileEntity().also { dao.insert(it) }
    }

    suspend fun update(transform: (UserProfileEntity) -> UserProfileEntity) {
        val current = getOrCreate()
        dao.insert(transform(current).copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateName(name: String) = update { it.copy(name = name) }
    suspend fun updateFocus(focus: String) = update { it.copy(currentFocus = focus) }
    suspend fun updateStack(language: String, frameworks: List<String>) = update {
        it.copy(primaryLanguage = language, primaryFrameworks = frameworks.joinToString(","))
    }
    suspend fun updateStyle(style: String) = update { it.copy(workingStyle = style) }
    suspend fun updateCommunicationPref(pref: String) = update { it.copy(communicationPref = pref) }
}
