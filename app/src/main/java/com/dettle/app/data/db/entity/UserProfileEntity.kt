package com.dettle.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent profile of the user, built up over time by the LearningEngine.
 *
 * There is exactly ONE row in this table (singleton, id=1).
 * The AI uses this to personalize every response — it knows who it's talking to.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,  // singleton
    @ColumnInfo(name = "name") val name: String = "",
    @ColumnInfo(name = "occupation") val occupation: String = "",
    @ColumnInfo(name = "timezone") val timezone: String = "",
    @ColumnInfo(name = "primary_language") val primaryLanguage: String = "Kotlin",
    @ColumnInfo(name = "primary_frameworks") val primaryFrameworks: String = "",  // comma-separated
    @ColumnInfo(name = "working_style") val workingStyle: String = "",
    @ColumnInfo(name = "communication_pref") val communicationPref: String = "",
    @ColumnInfo(name = "current_focus") val currentFocus: String = "",
    @ColumnInfo(name = "interests") val interests: String = "",   // comma-separated
    @ColumnInfo(name = "streak_count") val streakCount: Int = 1,
    @ColumnInfo(name = "last_active_timestamp") val lastActiveTimestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "streak_freeze_tokens") val streakFreezeTokens: Int = 1,
    @ColumnInfo(name = "total_xp") val totalXp: Long = 50L,
    @ColumnInfo(name = "level") val level: Int = 1,
    @ColumnInfo(name = "level_title") val levelTitle: String = "Script Apprentice",
    @ColumnInfo(name = "badges") val badges: List<String> = emptyList(),
    @ColumnInfo(name = "onboarding_completed") val onboardingCompleted: Boolean = false,
    @ColumnInfo(name = "completed_quests") val completedQuests: List<String> = emptyList(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
