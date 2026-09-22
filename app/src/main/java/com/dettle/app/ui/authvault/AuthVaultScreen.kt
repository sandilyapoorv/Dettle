package com.dettle.app.ui.authvault

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.data.webview.WebViewPool
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.WebViewAccount
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange

@Composable
fun AuthVaultScreen(
    viewModel: AuthVaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeAccount = uiState.activeLoginAccount

    if (activeAccount != null) {
        val loginWebView = remember(activeAccount.id) {
            viewModel.getWebViewForLogin(activeAccount)
        }
        WebViewLoginScreen(
            account = activeAccount,
            webView = loginWebView,
            progress = uiState.loginProgress,
            onReload = viewModel::reloadLogin,
            onDone = viewModel::onLoginDone,
            onCancel = viewModel::onLoginCancelled
        )
    } else {
        ProviderOverview(
            statuses = uiState.providerStatuses,
            onLogin = viewModel::startLogin,
            onDelete = viewModel::deleteSubscriptionAccount,
            onAddAccount = viewModel::addSubscriptionAccount
        )
    }
}

// ─── Provider Overview ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderOverview(
    statuses: List<WebViewPool.PoolStatus>,
    onLogin: (WebViewAccount) -> Unit,
    onDelete: (String) -> Unit,
    onAddAccount: (AIProviderType, String, String?, String?) -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<WebViewAccount?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        "Auth Vault",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add subscription provider")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Manage your AI subscription providers and accounts. Dettle securely preserves your sessions locally on-device.",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SUBSCRIPTION PROVIDERS (${statuses.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Provider")
                    }
                }
            }

            if (statuses.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "No subscription accounts configured",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Tap 'Add Provider' above to connect ChatGPT, Claude, DeepSeek, or other subscriptions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(statuses, key = { it.account.id }) { status ->
                AccountStatusCard(
                    status = status,
                    onLogin = { onLogin(status.account) },
                    onDelete = { accountToDelete = status.account }
                )
            }
        }
    }

    if (showAddSheet) {
        AddSubscriptionSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { provider, label, loginUrl, baseUrl ->
                onAddAccount(provider, label, loginUrl, baseUrl)
                showAddSheet = false
            }
        )
    }

    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Remove Account") },
            text = { Text("Are you sure you want to remove '${account.label}'? This will terminate its active session and remove it from your provider pool.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(account.id)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
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
fun AccountStatusCard(
    status: WebViewPool.PoolStatus,
    onLogin: () -> Unit,
    onDelete: () -> Unit
) {
    val account = status.account
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
                    account.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when {
                        isOk -> "Ready — session active"
                        status.needsReauth -> "Session expired — tap to re-authenticate"
                        !status.isLoggedIn -> "Not signed in — tap to connect"
                        else -> "Session configured"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isOk -> DettleGreen
                        status.needsReauth -> DettleOrange
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    account.loginUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
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

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove account",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Add Subscription Account Sheet ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionSheet(
    onDismiss: () -> Unit,
    onAdd: (AIProviderType, String, String?, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val webviewProviders = listOf(
        AIProviderType.CHATGPT_WEB,
        AIProviderType.CLAUDE_WEB,
        AIProviderType.DEEPSEEK_WEB,
        AIProviderType.GROK_WEB,
        AIProviderType.GEMINI_WEB,
        AIProviderType.KIMI_WEB,
        AIProviderType.MISTRAL_WEB,
        AIProviderType.QWEN_WEB
    )

    var selectedProvider by remember { mutableStateOf(webviewProviders.first()) }
    var accountLabel by remember { mutableStateOf("${selectedProvider.displayName} Account") }
    var customLoginUrl by remember { mutableStateOf(selectedProvider.loginUrl) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Add Subscription Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Select the subscription provider you want to add to your rotation pool:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Provider selection chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyColumn(modifier = Modifier.height(140.dp)) {
                    items(webviewProviders) { provider ->
                        Surface(
                            onClick = {
                                selectedProvider = provider
                                accountLabel = "${provider.displayName} Account"
                                customLoginUrl = provider.loginUrl
                            },
                            shape = MaterialTheme.shapes.small,
                            color = if (selectedProvider == provider) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.dp,
                                if (selectedProvider == provider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    provider.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedProvider == provider) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    provider.loginUrl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = accountLabel,
                onValueChange = { accountLabel = it },
                label = { Text("Account Label") },
                placeholder = { Text("e.g. Work ChatGPT or Personal Claude") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )

            OutlinedTextField(
                value = customLoginUrl,
                onValueChange = { customLoginUrl = it },
                label = { Text("Login URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        onAdd(
                            selectedProvider,
                            accountLabel.trim(),
                            customLoginUrl.trim(),
                            selectedProvider.baseUrl
                        )
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Add Account")
                }
            }
        }
    }
}

// ─── WebView Login Screen ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewLoginScreen(
    account: WebViewAccount,
    webView: WebView?,
    progress: Int,
    onReload: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Sign in to ${account.label}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                account.loginUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(account.loginUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }) {
                            Icon(Icons.Outlined.OpenInBrowser, contentDescription = "Open in browser")
                        }
                        IconButton(onClick = onReload) {
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
                if (progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
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
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            isFocusable = true
                            isFocusableInTouchMode = true
                            requestFocus()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        view.visibility = android.view.View.VISIBLE
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Loading ${account.label}...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
