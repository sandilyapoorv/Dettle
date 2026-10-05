package com.dettle.app.orchestrator.gamification

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sally the Enforcer: An unhinged, sarcastic, developer-focused companion.
 * Provides tough-love roasts for inactivity and extreme developer hype for shipping.
 */
@Singleton
class SallyEnforcerPersona @Inject constructor() {

    private val brutalInactivityRoasts = listOf(
        "Look who finally crawled back. You are a fat fucking slow sloth like your mama—she took 9 months to make a joke. Get in the terminal and push some code.",
        "Did your keyboard break or did your ambition just dissolve into thin air? 3 days offline is embarrassing. Build something or delete your GitHub.",
        "Breaking news: local developer discovered touching grass instead of shipping code. Tragic. Now get your ass back to work.",
        "Your streak died of neglect while you were busy procrastinating. Even npm install runs faster than your progress.",
        "I was about to archive your repos out of pity. Prove to me you still know how to write a function."
    )

    private val streakWarningRoasts = listOf(
        "Oi, wake up! Your streak dies in a few hours. Write a commit before midnight or accept being a script casual.",
        "The clock is ticking and your streak is sweating bullets. Ship a feature or lose your crown.",
        "Just a heads up: if you break this streak tonight, I will roast you without mercy tomorrow morning."
    )

    private val shippingHypeQuotes = listOf(
        "CI is GREEN! 0 warnings, clean release. You might actually know what you're doing.",
        "Bare metal conquered. Cloudflare deployment is live across 300 edge locations. Pure art.",
        "Hell yeah! PR created and tests passed without breaking production. Take notes, script kiddies.",
        "Look at you shipping like a 10x architect. Keep this energy and we'll take over the cloud.",
        "Another milestone crushed. Leveling up faster than a recursive algorithm with no base case."
    )

    fun getInactivityRoast(daysInactive: Int): String {
        return if (daysInactive >= 3) {
            brutalInactivityRoasts.random()
        } else {
            "You've been MIA for $daysInactive day(s). The codebase is getting cold—let's ship something."
        }
    }

    fun getStreakWarning(): String = streakWarningRoasts.random()

    fun getShippingHype(): String = shippingHypeQuotes.random()

    fun getLevelUpHype(newLevel: Int, title: String): String {
        return "LEVEL UP! You reached Level $newLevel: $title. Sally approves. Now don't get cocky—go deploy."
    }
}
