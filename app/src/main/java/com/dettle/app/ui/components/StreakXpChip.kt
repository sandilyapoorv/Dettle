package com.dettle.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dettle.app.data.db.entity.UserProfileEntity

/**
 * Top bar Apple Liquid Glass gamification pill displaying the daily streak and XP level.
 */
@Composable
fun StreakXpChip(
    profile: UserProfileEntity?,
    modifier: Modifier = Modifier
) {
    val streak = profile?.streakCount ?: 1
    val level = profile?.level ?: 1
    val xp = profile?.totalXp ?: 50L

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Streak
            Text(
                text = "🔥 $streak",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = Color(0xFFFF9500) // Apple Flame Orange
            )

            Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            // Level & XP
            Text(
                text = "⚡ Lvl $level ($xp XP)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = Color(0xFF007AFF) // Electric Cyan/Blue
            )
        }
    }
}
