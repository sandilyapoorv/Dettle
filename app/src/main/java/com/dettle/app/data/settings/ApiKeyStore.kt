package com.dettle.app.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dettle.app.domain.model.AIProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted key-value store for all API keys and sensitive tokens.
 * Uses Android Keystore + AES256 via EncryptedSharedPreferences.
 * Keys never leave device storage in plaintext.
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "dettle_secure_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ─── AI Provider Keys ─────────────────────────────────────────────────

    fun getKey(provider: AIProviderType): String? =
        prefs.getString(keyFor(provider), null)?.takeIf { it.isNotBlank() }

    fun setKey(provider: AIProviderType, apiKey: String) {
        prefs.edit().putString(keyFor(provider), apiKey.trim()).apply()
    }

    fun clearKey(provider: AIProviderType) {
        prefs.edit().remove(keyFor(provider)).apply()
    }

    fun hasKey(provider: AIProviderType): Boolean = !getKey(provider).isNullOrBlank()

    private fun keyFor(provider: AIProviderType) = "api_key_${provider.name}"

    // ─── GitHub ───────────────────────────────────────────────────────────

    var githubPat: String?
        get() = prefs.getString("github_pat", null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString("github_pat", value?.trim()).apply()

    var githubOwner: String?
        get() = prefs.getString("github_owner", null)
        set(value) = prefs.edit().putString("github_owner", value?.trim()).apply()

    // ─── Cloudflare ───────────────────────────────────────────────────────

    var cloudflareApiToken: String?
        get() = prefs.getString("cloudflare_token", null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString("cloudflare_token", value?.trim()).apply()

    var cloudflareAccountId: String?
        get() = prefs.getString("cloudflare_account_id", null)
        set(value) = prefs.edit().putString("cloudflare_account_id", value?.trim()).apply()

    // ─── Google Drive ─────────────────────────────────────────────────────
    // All tokens encrypted with Android Keystore — never committed or logged

    /** The OAuth2 ID token from Google Sign-In (short-lived, ~1hr) */
    var driveIdToken: String?
        get() = prefs.getString("drive_id_token", null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString("drive_id_token", value?.trim()).apply()

    /** The OAuth2 refresh token (long-lived, permanent until revoked) */
    var driveRefreshToken: String?
        get() = prefs.getString("drive_refresh_token", null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString("drive_refresh_token", value?.trim()).apply()

    /** Gmail address of the connected Drive account */
    var driveUserEmail: String?
        get() = prefs.getString("drive_user_email", null)
        set(value) = prefs.edit().putString("drive_user_email", value).apply()

    /** Cached Google Drive folder ID for the "Dettle" folder */
    var driveFolderId: String?
        get() = prefs.getString("drive_folder_id", null)
        set(value) = prefs.edit().putString("drive_folder_id", value).apply()

    // ─── Remote Selector Registry ─────────────────────────────────────────

    var selectorRegistryGistUrl: String?
        get() = prefs.getString("selector_gist_url", DEFAULT_SELECTOR_URL)
        set(value) = prefs.edit().putString("selector_gist_url", value).apply()

    // ─── Helper: which providers have keys configured ─────────────────────

    fun getConfiguredApiProviders(): List<AIProviderType> =
        AIProviderType.entries.filter { !it.isWebView && hasKey(it) }

    // ─── WebView provider enable/disable ──────────────────────────────────
    // Default: all WebView providers enabled (user just needs to log in)

    fun isWebViewProviderEnabled(providerType: AIProviderType): Boolean =
        prefs.getBoolean("webview_enabled_${providerType.name}", true)

    fun setWebViewProviderEnabled(providerType: AIProviderType, enabled: Boolean) =
        prefs.edit().putBoolean("webview_enabled_${providerType.name}", enabled).apply()

    // ─── Overnight mode settings ──────────────────────────────────────────

    /** Max hours the overnight loop will run before auto-stopping (default: 8h) */
    var overnightMaxHours: Long
        get() = prefs.getLong("overnight_max_hours", 8L)
        set(value) = prefs.edit().putLong("overnight_max_hours", value).apply()

    /** Whether to auto-start overnight loop at a scheduled time */
    var overnightAutoStart: Boolean
        get() = prefs.getBoolean("overnight_auto_start", false)
        set(value) = prefs.edit().putBoolean("overnight_auto_start", value).apply()

    /** Scheduled start time as HH:mm string (e.g. "23:00") */
    var overnightScheduledTime: String
        get() = prefs.getString("overnight_schedule_time", "23:00") ?: "23:00"
        set(value) = prefs.edit().putString("overnight_schedule_time", value).apply()

    /** JSON-serialized TaskQueue list (survives app restart) */
    var taskQueueJson: String?
        get() = prefs.getString("overnight_task_queue", null)
        set(value) = prefs.edit().putString("overnight_task_queue", value).apply()

    /** Whether to send a notification when overnight run completes */
    var overnightNotifyOnComplete: Boolean
        get() = prefs.getBoolean("overnight_notify", true)
        set(value) = prefs.edit().putBoolean("overnight_notify", value).apply()

    companion object {
        const val DEFAULT_SELECTOR_URL =
            "https://gist.githubusercontent.com/YOUR_GITHUB_USERNAME/GIST_ID/raw/dom_selectors.json"
    }
}
