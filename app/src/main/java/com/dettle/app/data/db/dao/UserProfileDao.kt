package com.dettle.app.data.db.dao

import androidx.room.*
import com.dettle.app.data.db.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun get(): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("UPDATE user_profile SET name = :name, updated_at = :time WHERE id = 1")
    suspend fun updateName(name: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET current_focus = :focus, updated_at = :time WHERE id = 1")
    suspend fun updateFocus(focus: String, time: Long = System.currentTimeMillis())
}
