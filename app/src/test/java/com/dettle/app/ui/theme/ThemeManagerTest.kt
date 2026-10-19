package com.dettle.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeManagerTest {

    @Test
    fun testFromId_resolvesAllKnownThemes() {
        assertEquals(AppTheme.TITANIUM, AppTheme.fromId("titanium"))
        assertEquals(AppTheme.CYBER_INDIGO, AppTheme.fromId("cyber_indigo"))
        assertEquals(AppTheme.EMERALD_MATRIX, AppTheme.fromId("emerald_matrix"))
        assertEquals(AppTheme.SUNSET_AMBER, AppTheme.fromId("sunset_amber"))
        assertEquals(AppTheme.TOKYO_NEON, AppTheme.fromId("tokyo_neon"))
        assertEquals(AppTheme.OAT_LIGHT, AppTheme.fromId("oat_light"))
        assertEquals(AppTheme.APPLE_LIGHT, AppTheme.fromId("apple_light"))
        assertEquals(AppTheme.OLED_BLACK, AppTheme.fromId("oled_black"))
        assertEquals(AppTheme.OBSIDIAN, AppTheme.fromId("obsidian"))
    }

    @Test
    fun testFromId_handlesNullAndUnknownFallback() {
        assertEquals(AppTheme.OBSIDIAN, AppTheme.fromId(null))
        assertEquals(AppTheme.OBSIDIAN, AppTheme.fromId(""))
        assertEquals(AppTheme.OBSIDIAN, AppTheme.fromId("non_existent_theme"))
    }
}
