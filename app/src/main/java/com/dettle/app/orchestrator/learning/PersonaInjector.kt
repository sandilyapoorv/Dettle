package com.dettle.app.orchestrator.learning

import com.dettle.app.data.db.dao.UserProfileDao
import com.dettle.app.data.db.entity.UserProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the "WHO YOU ARE WORKING WITH" block injected at the top of every system prompt.
 *
 * This makes the AI feel like it knows you. It reads the UserProfile built up
 * over time by the LearningEngine and formats it into a concise context block.
 *
 * Example output:
 * === YOUR USER ===
 * Name: Ritesh
 * Current focus: Building Dettle — an autonomous AI coding agent for Android
 * Stack: Kotlin, Jetpack Compose, Material 3, Hilt
 * Style: Minimal abstractions, TDD-first, no boilerplate
 * Comms: Direct and concise. No preambles or lengthy explanations.
 * === END USER PROFILE ===
 */
@Singleton
class PersonaInjector @Inject constructor(
    private val userProfileDao: UserProfileDao
) {
    suspend fun buildPersonaBlock(): String {
        val profile = userProfileDao.get() ?: return ""
        if (!profile.hasAnyData()) return ""

        return buildString {
            appendLine("=== YOUR USER ===")
            if (profile.name.isNotBlank()) appendLine("Name: ${profile.name}")
            if (profile.occupation.isNotBlank()) appendLine("Role: ${profile.occupation}")
            if (profile.currentFocus.isNotBlank()) appendLine("Current focus: ${profile.currentFocus}")
            if (profile.primaryLanguage.isNotBlank()) {
                val frameworks = profile.primaryFrameworks.split(",").filter { it.isNotBlank() }
                val stack = if (frameworks.isNotEmpty()) {
                    "${profile.primaryLanguage}, ${frameworks.joinToString(", ")}"
                } else profile.primaryLanguage
                appendLine("Stack: $stack")
            }
            if (profile.workingStyle.isNotBlank()) appendLine("Style: ${profile.workingStyle}")
            if (profile.communicationPref.isNotBlank()) appendLine("Comms: ${profile.communicationPref}")
            if (profile.interests.isNotBlank()) appendLine("Interests: ${profile.interests}")
            appendLine("=== END USER PROFILE ===")
            appendLine()
        }
    }

    private fun UserProfileEntity.hasAnyData() =
        name.isNotBlank() || currentFocus.isNotBlank() || occupation.isNotBlank()
}
