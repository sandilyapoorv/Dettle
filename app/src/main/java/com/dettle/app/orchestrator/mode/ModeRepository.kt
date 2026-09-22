package com.dettle.app.orchestrator.mode

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ModeRepository"

/**
 * Manages agent mode configurations — defaults + user customizations.
 *
 * Customizations are stored in DataStore as JSON (one key per ModeId).
 * At read time, defaults are merged with any user overrides to produce
 * the [AgentMode.effectiveConfig] that the loop actually uses.
 *
 * The user can:
 * - Change the model waterfall (add, remove, reorder models)
 * - Toggle individual tools on/off
 * - Adjust the step budget
 * - Add custom instructions appended to the system prompt
 * - Reset any mode back to defaults
 */
@Singleton
class ModeRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    // ── Read ──────────────────────────────────────────────────────────────────

    /** Observe all modes with their effective (default + user) configs as a live Flow */
    fun observeAllModes(): Flow<List<AgentMode>> = dataStore.data
        .catch { e -> Log.e(TAG, "DataStore read error: ${e.message}"); emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> AgentModes.ALL.map { mode -> mergeWithOverride(mode, prefs) } }

    /** Observe a single mode's effective config */
    fun observeMode(id: ModeId): Flow<AgentMode> = dataStore.data
        .catch { e -> Log.e(TAG, "DataStore read error: ${e.message}"); emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> mergeWithOverride(AgentModes.forId(id), prefs) }

    /** Get mode synchronously from defaults (no DataStore — use in non-coroutine contexts) */
    fun getDefault(id: ModeId): AgentMode = AgentModes.forId(id)

    // ── Write ─────────────────────────────────────────────────────────────────

    /** Update the model waterfall for a mode */
    suspend fun updateModelWaterfall(id: ModeId, modelIds: List<String>) {
        updateConfig(id) { it.copy(modelWaterfall = modelIds) }
    }

    /** Toggle a tool on/off for a mode */
    suspend fun setToolEnabled(id: ModeId, toolName: String, enabled: Boolean) {
        updateConfig(id) { config ->
            val updated = if (enabled) {
                (config.enabledTools + toolName).distinct()
            } else {
                config.enabledTools - toolName
            }
            config.copy(enabledTools = updated)
        }
    }

    /** Update step budget for a mode */
    suspend fun updateMaxSteps(id: ModeId, steps: Int) {
        updateConfig(id) { it.copy(maxSteps = steps.coerceIn(2, 100)) }
    }

    /** Update custom instructions for a mode */
    suspend fun updateCustomInstructions(id: ModeId, instructions: String) {
        updateConfig(id) { it.copy(customInstructions = instructions) }
    }

    /** Bulk-update the full config for a mode */
    suspend fun saveConfig(id: ModeId, config: ModeConfig) {
        dataStore.edit { prefs ->
            prefs[prefKey(id)] = json.encodeToString(config)
        }
        Log.d(TAG, "Saved config for ${id.name}")
    }

    /** Reset a mode to its defaults (removes DataStore override) */
    suspend fun resetToDefault(id: ModeId) {
        dataStore.edit { prefs -> prefs.remove(prefKey(id)) }
        Log.d(TAG, "Reset ${id.name} to defaults")
    }

    /** Reset all modes to defaults */
    suspend fun resetAll() {
        dataStore.edit { prefs -> ModeId.values().forEach { prefs.remove(prefKey(it)) } }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun updateConfig(id: ModeId, transform: (ModeConfig) -> ModeConfig) {
        dataStore.edit { prefs ->
            val current = readConfig(id, prefs) ?: AgentModes.forId(id).defaultConfig
            prefs[prefKey(id)] = json.encodeToString(transform(current))
        }
    }

    private fun mergeWithOverride(mode: AgentMode, prefs: Preferences): AgentMode {
        val override = readConfig(mode.id, prefs)
        return if (override != null) {
            mode.copy(effectiveConfig = mergeConfigs(mode.defaultConfig, override))
        } else {
            mode
        }
    }

    /**
     * Merge strategy: user override wins field-by-field, but empty lists fall back to defaults.
     * This prevents the user from accidentally clearing a mode's tools entirely.
     */
    private fun mergeConfigs(default: ModeConfig, override: ModeConfig): ModeConfig = ModeConfig(
        enabledTools = override.enabledTools.ifEmpty { default.enabledTools },
        modelWaterfall = override.modelWaterfall.ifEmpty { default.modelWaterfall },
        maxSteps = override.maxSteps.takeIf { it > 0 } ?: default.maxSteps,
        completionGates = override.completionGates.ifEmpty { default.completionGates },
        requireApprovalFor = override.requireApprovalFor.ifEmpty { default.requireApprovalFor },
        customInstructions = override.customInstructions.ifBlank { default.customInstructions }
    )

    private fun readConfig(id: ModeId, prefs: Preferences): ModeConfig? {
        val raw = prefs[prefKey(id)] ?: return null
        return try {
            json.decodeFromString<ModeConfig>(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse config for ${id.name}: ${e.message}")
            null
        }
    }

    private fun prefKey(id: ModeId) = stringPreferencesKey("mode_config_${id.name}")
}
