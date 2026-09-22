package com.dettle.app.ui.authvault

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.data.webview.WebViewPool
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.ui.theme.DettleCard
import com.dettle.app.ui.theme.DettleCardBorder
import com.dettle.app.ui.theme.DettleCyan
import com.dettle.app.ui.theme.DettleDark
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.DettleRed
import com.dettle.app.ui.theme.DettleSurface
import com.dettle.app.ui.theme.DettleTextMuted
import com.dettle.app.ui.theme.DettleTextSecondary

/**
 * Auth Vault — where the user logs into their AI subscriptions.
 *
 * There are two modes:
 * 1. OVERVIEW: Shows status of all 8 WebView providers (logged in / needs login)
 * 2. LOGIN: Shows the actual WebView for a specific provider, fullscreen,
 *    so the user can log in like a normal browser
 *
 * After login, the JS loginCheck selector detects success and:
 * - Hides the WebView (back to GONE)
 * - Marks the session as available
 * - Returns to overview
 *
 * Cookies are persisted to disk via CookieManager.flush() on every page load,
 * so login survives app restarts indefinitely.
 */
@Composable
fun AuthVaultScreen(
    viewModel: AuthVaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.activeLoginProvider != null) {
        // Full-screen WebView login for a specific provider
        WebViewLoginScreen(
            providerType = uiState.activeLoginProvider!!,
            webView = viewModel.getWebViewForLogin(uiState.activeLoginProvider!!),
            onDone = viewModel::onLoginDone,
            onCancel = viewModel::onLoginCancelled
        )
    } else {
        // Overview of all WebView providers
        ProviderOverview(
            statuses = uiState.providerStatuses,
            onLogin = viewModel::startLogin
        )
    }
}

// ─── Provider Overview ─────────────────────────────────────────────────────

@Composable
fun ProviderOverview(
    statuses: List<WebViewPool.PoolStatus>,
    onLogin: (AIProviderType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DettleDark)
            .padding(16.dp)
    ) {
        Text(
            "AI Subscriptions",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Log into your paid accounts once — Dettle saves the session permanently.",
            style = MaterialTheme.typography.bodySmall,
            color = DettleTextMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Stats row
        val loggedIn = statuses.count { it.isLoggedIn }
        val available = statuses.count { it.isAvailable }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip("$loggedIn / ${statuses.size}", "Logged In", DettleGreen)
            StatChip("$available", "Available Now", DettleCyan)
            StatChip(
                "${statuses.count { it.needsReauth }}",
                "Needs Login",
                if (statuses.any { it.needsReauth }) DettleOrange else DettleTextMuted
            )
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(statuses) { status ->
                ProviderStatusCard(status = status, onLogin = { onLogin(status.providerType) })
            }
        }
    }
}

@Composable
fun StatChip(value: String, label: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = DettleTextMuted)
    }
}

@Composable
fun ProviderStatusCard(
    status: WebViewPool.PoolStatus,
    onLogin: () -> Unit
) {
    val provider = status.providerType
    val isOk = status.isAvailable
    val needsAction = status.needsReauth || !status.isLoggedIn

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DettleCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .border(
                    1.dp,
                    when {
                        isOk -> DettleGreen.copy(alpha = 0.3f)
                        status.needsReauth -> DettleOrange.copy(alpha = 0.4f)
                        else -> DettleCardBorder
                    },
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isOk -> DettleGreen
                            status.needsReauth -> DettleOrange
                            else -> DettleTextMuted
                        }
                    )
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    provider.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when {
                        isOk -> "✅ Ready — session active"
                        status.needsReauth -> "⚠️ Session expired — tap to re-login"
                        !status.isLoggedIn -> "🔒 Not logged in — tap to connect"
                        else -> "Loading..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isOk -> DettleGreen
                        status.needsReauth -> DettleOrange
                        else -> DettleTextSecondary
                    }
                )
                Text(
                    provider.baseUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = DettleTextMuted
                )
            }

            if (needsAction) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onLogin,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status.needsReauth) DettleOrange.copy(alpha = 0.2f) else DettleCyan.copy(alpha = 0.15f),
                        contentColor = if (status.needsReauth) DettleOrange else DettleCyan
                    )
                ) {
                    Icon(
                        if (status.needsReauth) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (status.needsReauth) "Re-login" else "Login",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Icon(Icons.Filled.Check, contentDescription = null, tint = DettleGreen, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── WebView Login Screen ──────────────────────────────────────────────────

@Composable
fun WebViewLoginScreen(
    providerType: AIProviderType,
    webView: WebView?,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DettleDark)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DettleSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.LockOpen, contentDescription = null, tint = DettleCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Login to ${providerType.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Log in normally — Dettle saves your session permanently",
                    style = MaterialTheme.typography.bodySmall,
                    color = DettleTextMuted
                )
            }
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DettleGreen, contentColor = DettleDark)
            ) {
                Text("Done", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onCancel,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DettleRed.copy(alpha = 0.2f),
                    contentColor = DettleRed
                )
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelMedium)
            }
        }

        // The actual WebView — made visible here so user can interact with it
        if (webView != null) {
            AndroidView(
                factory = {
                    webView.apply {
                        visibility = android.view.View.VISIBLE
                        (parent as? ViewGroup)?.removeView(this)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { /* No dynamic updates needed */ }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading ${providerType.displayName}...", color = DettleTextMuted)
            }
        }
    }
}
