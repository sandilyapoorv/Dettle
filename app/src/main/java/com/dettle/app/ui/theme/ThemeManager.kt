package com.dettle.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val previewPrimary: Color,
    val previewBackground: Color
) {
    OBSIDIAN(
        id = "obsidian",
        displayName = "Obsidian Void",
        description = "Precision dark slate & electric indigo",
        previewPrimary = Color(0xFF5E6AD2),
        previewBackground = Color(0xFF0C0D10)
    ),
    APPLE_LIGHT(
        id = "apple_light",
        displayName = "Apple Studio Light",
        description = "Crisp Cupertino white & titanium slate",
        previewPrimary = Color(0xFF0071E3),
        previewBackground = Color(0xFFF5F5F7)
    ),
    TITANIUM(
        id = "titanium",
        displayName = "Midnight Titanium",
        description = "Space titanium gray & frosted glass",
        previewPrimary = Color(0xFF9096A2),
        previewBackground = Color(0xFF141518)
    ),
    CYBER_INDIGO(
        id = "cyber_indigo",
        displayName = "Cyber Indigo",
        description = "Deep navy void & electric neon violet",
        previewPrimary = Color(0xFF7C3AED),
        previewBackground = Color(0xFF0B0F19)
    ),
    EMERALD_MATRIX(
        id = "emerald_matrix",
        displayName = "Emerald Matrix",
        description = "Deep forest moss & crisp emerald",
        previewPrimary = Color(0xFF10B981),
        previewBackground = Color(0xFF0A120E)
    ),
    SUNSET_AMBER(
        id = "sunset_amber",
        displayName = "Sunset Amber",
        description = "Dark charcoal & warm amber gold",
        previewPrimary = Color(0xFFF59E0B),
        previewBackground = Color(0xFF12100E)
    ),
    TOKYO_NEON(
        id = "tokyo_neon",
        displayName = "Tokyo Neon",
        description = "Midnight violet & vivid cyan neon",
        previewPrimary = Color(0xFF06B6D4),
        previewBackground = Color(0xFF100C1A)
    ),
    OLED_BLACK(
        id = "oled_black",
        displayName = "Pure OLED Black",
        description = "Absolute true black for AMOLED efficiency",
        previewPrimary = Color(0xFF5E6AD2),
        previewBackground = Color(0xFF000000)
    );

    companion object {
        fun fromId(id: String?): AppTheme =
            values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OBSIDIAN
    }
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    DARK("Force Dark"),
    LIGHT("Force Light")
}

data class ThemeConfig(
    val theme: AppTheme = AppTheme.OBSIDIAN,
    val mode: ThemeMode = ThemeMode.DARK,
    val isPureOled: Boolean = false,
    val customAccentHex: Long? = null
)

@Singleton
class ThemeManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val KEY_THEME = stringPreferencesKey("app_theme_id")
    private val KEY_MODE = stringPreferencesKey("app_theme_mode")
    private val KEY_OLED = booleanPreferencesKey("app_theme_oled")
    private val KEY_ACCENT = stringPreferencesKey("app_theme_accent_hex")

    val themeConfig: StateFlow<ThemeConfig> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val themeId = prefs[KEY_THEME]
            val modeStr = prefs[KEY_MODE]
            val oled = prefs[KEY_OLED] ?: false
            val accentStr = prefs[KEY_ACCENT]

            val theme = AppTheme.fromId(themeId)
            val mode = ThemeMode.values().firstOrNull { it.name.equals(modeStr, ignoreCase = true) }
                ?: ThemeMode.DARK
            val accent = accentStr?.toLongOrNull()

            ThemeConfig(
                theme = theme,
                mode = mode,
                isPureOled = oled,
                customAccentHex = accent
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeConfig()
        )

    fun setTheme(theme: AppTheme) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_THEME] = theme.id
            }
        }
    }

    fun setMode(mode: ThemeMode) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_MODE] = mode.name
            }
        }
    }

    fun setPureOled(enabled: Boolean) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_OLED] = enabled
            }
        }
    }

    fun setCustomAccent(colorHex: Long?) {
        scope.launch {
            dataStore.edit { prefs ->
                if (colorHex == null) {
                    prefs.remove(KEY_ACCENT)
                } else {
                    prefs[KEY_ACCENT] = colorHex.toString()
                }
            }
        }
    }

    fun resetToDefaults() {
        scope.launch {
            dataStore.edit { prefs ->
                prefs.remove(KEY_THEME)
                prefs.remove(KEY_MODE)
                prefs.remove(KEY_OLED)
                prefs.remove(KEY_ACCENT)
            }
        }
    }
}
