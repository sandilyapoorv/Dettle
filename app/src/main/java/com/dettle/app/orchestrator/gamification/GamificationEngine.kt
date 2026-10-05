package com.dettle.app.orchestrator.gamification

import com.dettle.app.audio.ProceduralAudioService
import com.dettle.app.data.db.dao.UserProfileDao
import com.dettle.app.data.db.entity.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class XpAction(val basePoints: Long, val label: String) {
    DAILY_CHECK_IN(25L, "Daily Check-in"),
    TOOL_SUCCESS(15L, "Tool Executed"),
    FILE_EDIT_VERIFIED(30L, "Code Edit Verified"),
    GITHUB_PR_CREATED(75L, "GitHub PR Opened"),
    CLOUDFLARE_PAGES_DEPLOYED(100L, "Cloudflare Pages Deployed"),
    CLOUDFLARE_WORKER_DEPLOYED(100L, "Cloudflare Worker Published"),
    OVERNIGHT_TASK_COMPLETED(150L, "Overnight Run Completed"),
    CI_BUILD_GREEN(100L, "GitHub Actions CI Passed"),
    ONBOARDING_QUEST_COMPLETED(50L, "Quest Completed")
}

@Singleton
class GamificationEngine @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val audioService: ProceduralAudioService,
    private val sallyPersona: SallyEnforcerPersona
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _profileState = MutableStateFlow<UserProfileEntity?>(null)
    val profileState: StateFlow<UserProfileEntity?> = _profileState.asStateFlow()

    init {
        scope.launch {
            userProfileDao.observe().collect { profile ->
                if (profile == null) {
                    val defaultProfile = UserProfileEntity()
                    userProfileDao.insert(defaultProfile)
                    _profileState.value = defaultProfile
                } else {
                    _profileState.value = profile
                }
            }
        }
    }

    /**
     * Records a developer action, updates streaks, awards XP, and checks level-ups.
     */
    fun recordActivity(action: XpAction, customXp: Long? = null) {
        scope.launch {
            val current = userProfileDao.get() ?: UserProfileEntity().also { userProfileDao.insert(it) }
            val now = System.currentTimeMillis()

            // 1. Calculate Streak
            val daysDiff = daysBetween(current.lastActiveTimestamp, now)
            val newStreak = when {
                daysDiff == 0L -> current.streakCount // Same day
                daysDiff == 1L -> current.streakCount + 1 // Consecutive day
                daysDiff > 1L -> {
                    if (current.streakFreezeTokens > 0) {
                        current.streakCount // Protected by freeze token
                    } else {
                        1 // Streak reset
                    }
                }
                else -> current.streakCount
            }

            val newFreezeTokens = if (daysDiff > 1L && current.streakFreezeTokens > 0) {
                current.streakFreezeTokens - 1
            } else if (action == XpAction.OVERNIGHT_TASK_COMPLETED) {
                (current.streakFreezeTokens + 1).coerceAtMost(3) // Earn freeze tokens overnight
            } else {
                current.streakFreezeTokens
            }

            // 2. Award XP & Level
            val earnedXp = customXp ?: action.basePoints
            val updatedTotalXp = current.totalXp + earnedXp
            val (updatedLevel, updatedTitle) = computeLevel(updatedTotalXp)

            val isLevelUp = updatedLevel > current.level

            val updatedProfile = current.copy(
                streakCount = newStreak,
                lastActiveTimestamp = now,
                streakFreezeTokens = newFreezeTokens,
                totalXp = updatedTotalXp,
                level = updatedLevel,
                levelTitle = updatedTitle,
                updatedAt = now
            )

            userProfileDao.update(updatedProfile)
            _profileState.value = updatedProfile

            // 3. Audio & Haptic celebration
            if (isLevelUp) {
                audioService.playLevelUpFanfare()
            } else {
                audioService.playSuccessChime()
            }
        }
    }

    /**
     * Marks first-time onboarding quest as complete.
     */
    fun completeOnboardingQuest(questId: String) {
        scope.launch {
            val current = userProfileDao.get() ?: UserProfileEntity()
            val completed = current.completedQuests.toMutableList()
            if (!completed.contains(questId)) {
                completed.add(questId)
            }
            val isAllDone = completed.containsAll(listOf("neural_core", "bare_metal", "project_anchor", "first_light"))
            val updated = current.copy(
                completedQuests = completed,
                onboardingCompleted = isAllDone || current.onboardingCompleted,
                totalXp = current.totalXp + 50L,
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.update(updated)
            _profileState.value = updated
            audioService.playSuccessChime()
        }
    }

    /**
     * Completes entire onboarding and awards graduation bonus XP.
     */
    fun finishOnboarding() {
        scope.launch {
            val current = userProfileDao.get() ?: UserProfileEntity()
            val allQuests = listOf("neural_core", "bare_metal", "project_anchor", "first_light")
            val completed = (current.completedQuests + allQuests).distinct()
            val (updatedLevel, updatedTitle) = computeLevel(current.totalXp + 200L)
            val updated = current.copy(
                completedQuests = completed,
                onboardingCompleted = true,
                totalXp = current.totalXp + 200L,
                level = updatedLevel,
                levelTitle = updatedTitle,
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.update(updated)
            _profileState.value = updated
            audioService.playLevelUpFanfare()
        }
    }

    private fun daysBetween(startMs: Long, endMs: Long): Long {
        if (startMs <= 0L) return 0L
        val calStart = Calendar.getInstance().apply { timeInMillis = startMs }
        val calEnd = Calendar.getInstance().apply { timeInMillis = endMs }
        val diffMs = calEnd.timeInMillis - calStart.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMs).coerceAtLeast(0L)
    }

    private fun computeLevel(totalXp: Long): Pair<Int, String> {
        return when {
            totalXp >= 10_000L -> Pair(20, "Bare-Metal Overlord")
            totalXp >= 5_000L  -> Pair(15, "Cloudflare Sovereign")
            totalXp >= 2_500L  -> Pair(10, "Agent Orchestrator")
            totalXp >= 1_000L  -> Pair(5,  "Full-Stack Artisan")
            totalXp >= 500L    -> Pair(3,  "Syntax Operator")
            totalXp >= 200L    -> Pair(2,  "Junior Hacker")
            else               -> Pair(1,  "Script Apprentice")
        }
    }
}
