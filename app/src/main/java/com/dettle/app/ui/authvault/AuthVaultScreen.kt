package com.dettle.app.ui.authvault

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.data.webview.WebViewPool
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange

@Composable
fun AuthVaultScreen(
    viewModel: AuthVaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.activeLoginProvider != null) {
        WebViewLoginScreen(
            providerType = uiState.activeLoginProvider!!,
            webView = viewModel.getWebViewForLogin(uiState.activeLoginProvider!!),
            onDone = viewModel::onLoginDone,
            onCancel = viewModel::onLoginCancelled
        )
    } else {
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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Auth Vault",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Log into your subscription providers once. Dettle securely preserves your session on-device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stats row
            item {
                val loggedIn = statuses.count { it.isLoggedIn }
                val available = statuses.count { it.isAvailable }
                val needsReauth = statuses.count { it.needsReauth }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard("$loggedIn / ${statuses.size}", "Logged In", modifier = Modifier.weight(1f))
                    StatCard("$available", "Available", modifier = Modifier.weight(1f))
                    StatCard(
                        "$needsReauth",
                        "Needs Action",
                        isWarning = needsReauth > 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    "SUBSCRIPTION PROVIDERS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(statuses) { status ->
                ProviderStatusCard(status = status, onLogin = { onLogin(status.providerType) })
            }
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (isWarning) MaterialTheme.colorScheme.error.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isWarning) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isOk) {
                    MaterialTheme.colorScheme.primaryContainer
                } else if (status.needsReauth) {
                    DettleOrange.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isOk) Icons.Outlined.CheckCircle else if (status.needsReauth) Icons.Outlined.WarningAmber else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (isOk) MaterialTheme.colorScheme.onPrimaryContainer else if (status.needsReauth) DettleOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    provider.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when {
                        isOk -> "Ready — session active"
                        status.needsReauth -> "Session expired — tap to re-authenticate"
                        !status.isLoggedIn -> "Not signed in — tap to connect"
                        else -> "Initializing session..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isOk -> DettleGreen
                        status.needsReauth -> DettleOrange
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    provider.baseUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (needsAction) {
                FilledTonalButton(
                    onClick = onLogin,
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        if (status.needsReauth) Icons.Outlined.LockOpen else Icons.Outlined.Key,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (status.needsReauth) "Re-login" else "Sign In",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "Connected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─── WebView Login Screen ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewLoginScreen(
    providerType: AIProviderType,
    webView: WebView?,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancel")
                    }
                },
                title = {
                    Text(
                        "Sign in to ${providerType.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Reload page")
                    }
                    Button(
                        onClick = onDone,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Done")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (webView != null) {
                AndroidView(
                    factory = {
                        webView.apply {
                            visibility = android.view.View.VISIBLE
                            (parent as? ViewGroup)?.removeView(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Loading ${providerType.displayName}...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
