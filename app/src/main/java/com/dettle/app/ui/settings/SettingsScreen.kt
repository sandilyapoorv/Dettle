package com.dettle.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.ProviderAccount
import com.dettle.app.domain.model.GitHubAccount
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DesignServices
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

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
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Configure encrypted on-device API credentials, providers, and storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Aggregate Mathematics & Token Metrics Card
            item {
                AggregateMetricsCard(metrics = state.aggregateMetrics)
            }

            // Unlimited Multi-Account API Pool Section
            item {
                MultiAccountPoolSection(
                    accounts = state.providerAccounts,
                    onAddAccount = viewModel::addProviderAccount,
                    onToggleAccount = viewModel::toggleProviderAccount,
                    onDeleteAccount = viewModel::deleteProviderAccount
                )
            }

            // GitHub Multi-Account & Interactive Scopes Builder
            item {
                GitHubMultiAccountSection(
                    accounts = state.gitHubAccounts,
                    selectedScopes = state.selectedGitHubScopes,
                    onToggleScope = viewModel::toggleGitHubScope,
                    onGenerateTokenLink = {
                        val url = viewModel.buildCustomGitHubTokenUrl()
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    onAddAccount = { label, pat, user ->
                        viewModel.addGitHubAccount(label, pat, user)
                    },
                    onSetActiveAccount = viewModel::setActiveGitHubAccount,
                    onDeleteAccount = viewModel::deleteGitHubAccount
                )
            }

            // Cloudflare
            item {
                SettingsSection(
                    title = "Cloudflare Pages & Workers",
                    subtitle = "Free tier: 500 builds/month, 100K Worker requests/day"
                ) {
                    ApiKeyField(
                        label = "Cloudflare API Token",
                        hint = "API Token with Pages write access",
                        value = state.cloudflareToken,
                        isSaved = state.cloudflareSaved,
                        onSave = viewModel::saveCloudflareToken,
                        link = "dash.cloudflare.com/profile/api-tokens"
                    )
                    Spacer(Modifier.height(10.dp))
                    SimpleTextField(
                        label = "Cloudflare Account ID",
                        hint = "32-character hex account string",
                        value = state.cloudflareAccountId,
                        onValueChange = viewModel::setCloudflareAccountId
                    )
                }
            }

            // Google Drive Card
            item {
                GoogleDriveCard(
                    isConnected = state.driveConnected,
                    userEmail = state.driveUserEmail,
                    onConnect = { },
                    onDisconnect = viewModel::disconnectDrive
                )
            }

            // Voice Typing (OpenWhispr)
            item {
                VoiceTypingSettingsCard(
                    selectedEngine = state.voiceTypingEngine,
                    openWhisprUrl = state.openWhisprUrl,
                    onEngineSelected = viewModel::setVoiceTypingEngine,
                    onUrlChanged = viewModel::setOpenWhisprUrl
                )
            }

            // Provider Budget
            item {
                SettingsSection(
                    title = "Daily Provider Budget",
                    subtitle = "Rolling usage counters reset at midnight"
                ) {
                    state.providerStatuses.forEach { status ->
                        ProviderStatusRow(status)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun ApiKeyField(
    label: String,
    hint: String,
    value: String,
    isSaved: Boolean,
    onSave: (String) -> Unit,
    link: String = ""
) {
    var text by remember(isSaved) { mutableStateOf(if (isSaved) "••••••••••••" else value) }
    var visible by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(!isSaved) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            if (isSaved && !editing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Check, null, tint = DettleGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Saved", style = MaterialTheme.typography.labelSmall, color = DettleGreen)
                }
            }
        }
        if (link.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    link,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (editing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            hint,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    shape = MaterialTheme.shapes.small,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { onSave(text); editing = false },
                    enabled = text.isNotBlank() && text != "••••••••••••",
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Key,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "••••••••••••",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Change",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { editing = true; text = "" }
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleTextField(label: String, hint: String, value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ProviderStatusRow(status: com.dettle.app.data.api.ProviderStatus) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !status.isAvailable -> MaterialTheme.colorScheme.error
                            status.requestsPercentUsed > 0.8f -> DettleOrange
                            else -> DettleGreen
                        }
                    )
            )
            Spacer(Modifier.width(10.dp))
            Text(
                status.model.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (status.dailyRequestLimit > 0) "${status.requestsUsedToday}/${status.dailyRequestLimit} req"
                else "${status.requestsUsedToday} req",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GoogleDriveCard(
    isConnected: Boolean,
    userEmail: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Google Drive",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (isConnected) "Connected • $userEmail"
                        else "Not linked — connect your account for backups",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) DettleGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isConnected) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Pair(Icons.Outlined.Description, "Overnight run summaries"),
                        Pair(Icons.Outlined.Save, "Conversation export logs"),
                        Pair(Icons.Outlined.DesignServices, "Architecture documentation"),
                        Pair(Icons.Outlined.FolderCopy, "Repository snapshots")
                    ).forEach { (icon, item) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                item,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Disconnect Drive", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Connect Google Drive", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Scope: drive.file (only files Dettle creates — not full Drive access)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun VoiceTypingSettingsCard(
    selectedEngine: String,
    openWhisprUrl: String,
    onEngineSelected: (String) -> Unit,
    onUrlChanged: (String) -> Unit
) {
    var showBenchmarks by remember { mutableStateOf(false) }
    var urlInput by remember(openWhisprUrl) { mutableStateOf(openWhisprUrl) }

    SettingsSection(
        title = "Voice Typing & OpenWhispr",
        subtitle = "On-device speech recognition and OpenWhispr Whisper integration"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Engine Option 1: On-Device DSP
            Surface(
                onClick = { onEngineSelected("ON_DEVICE_DSP") },
                shape = MaterialTheme.shapes.medium,
                color = if (selectedEngine == "ON_DEVICE_DSP") MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (selectedEngine == "ON_DEVICE_DSP") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Android On-Device DSP",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "Recommended",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Instant streaming • < 25 MB RAM • 0 KB storage • Hardware-accelerated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Engine Option 2: OpenWhispr / LAN Server
            Surface(
                onClick = { onEngineSelected("OPENWHISPR_SERVER") },
                shape = MaterialTheme.shapes.medium,
                color = if (selectedEngine == "OPENWHISPR_SERVER") MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (selectedEngine == "OPENWHISPR_SERVER") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "OpenWhispr LAN / Remote Server",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Offloads Whisper to PC/Mac via LAN or Groq/OpenAI Whisper API",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedEngine == "OPENWHISPR_SERVER") {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        onUrlChanged(it)
                    },
                    label = { Text("Server URL (/v1/audio/transcriptions)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Benchmark & Resource Consumption Toggle
            OutlinedButton(
                onClick = { showBenchmarks = !showBenchmarks },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (showBenchmarks) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (showBenchmarks) "Hide Resource Consumption Metrics"
                    else "View OpenWhispr Resource Consumption Metrics"
                )
            }

            if (showBenchmarks) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "OpenWhispr & Speech Engine Benchmarks",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        listOf(
                            Triple("Android DSP (Default)", "0 MB model | 15-25 MB RAM", "< 100ms stream | < 0.3% batt"),
                            Triple("Whisper Tiny (Local)", "75 MB model | 250-350 MB RAM", "0.8s-1.5s lag | ~1.5% batt"),
                            Triple("Whisper Base (Local)", "142 MB model | 450-650 MB RAM", "2.2s-4.0s lag | ~3.0% batt"),
                            Triple("Whisper Small (Local)", "466 MB model | 1.2-1.8 GB RAM", "8s-15s lag | 6.0% batt (hot)"),
                            Triple("OpenWhispr LAN Server", "0 MB on device | 5 MB RAM", "300-800ms | negligible batt")
                        ).forEach { (engine, mem, perf) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    engine,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    mem,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    perf,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

// ─── Multi-Account & Metrics UI Components ─────────────────────────────────

@Composable
fun AggregateMetricsCard(
    metrics: com.dettle.app.domain.model.AggregateAccountMetrics
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Connected Accounts & Token Mathematics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "Total Accounts", value = "${metrics.totalAccounts}")
                MetricItem(label = "Active Keys", value = "${metrics.activeAccounts}")
                MetricItem(label = "Total Requests", value = "${metrics.totalRequests}")
                MetricItem(label = "Total Tokens", value = "${metrics.totalTokens}")
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MultiAccountPoolSection(
    accounts: List<ProviderAccount>,
    onAddAccount: (AIProviderType, String, String) -> Unit,
    onToggleAccount: (String, Boolean) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(AIProviderType.GROQ) }
    var accountLabel by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val supportedProviders = listOf(
        AIProviderType.GROQ,
        AIProviderType.GEMINI,
        AIProviderType.OPENROUTER,
        AIProviderType.SAMBANOVA,
        AIProviderType.GITHUB_MODELS
    )

    SettingsSection(
        title = "Unlimited Multi-Account API Pool",
        subtitle = "Add unlimited accounts/keys per provider. Automatic failover and token load-balancing."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Connected Keys (${accounts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = { showAddDialog = true },
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Account", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (accounts.isEmpty()) {
            Text(
                "No custom accounts added yet. Tap 'Add Account' to link multiple ChatGPT, Claude, Gemini, or Groq keys.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { acc ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    acc.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${acc.provider.displayName} • ${acc.requestsUsed} reqs • ${acc.tokensUsed} tokens",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = acc.isActive,
                                    onCheckedChange = { onToggleAccount(acc.id, it) },
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { onDeleteAccount(acc.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            ElevatedCard(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Add Provider Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    Text("Select Provider:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        supportedProviders.forEach { prov ->
                            val isSelected = prov == selectedProvider
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedProvider = prov }
                            ) {
                                Text(
                                    prov.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    SimpleTextField(label = "Account Nickname", hint = "e.g. Work Account, Plus 2", value = accountLabel, onValueChange = { accountLabel = it })
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key / Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    onAddAccount(selectedProvider, accountLabel, apiKeyInput)
                                    accountLabel = ""
                                    apiKeyInput = ""
                                    showAddDialog = false
                                }
                            },
                            enabled = apiKeyInput.isNotBlank()
                        ) {
                            Text("Save Key")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GitHubMultiAccountSection(
    accounts: List<GitHubAccount>,
    selectedScopes: List<String>,
    onToggleScope: (String) -> Unit,
    onGenerateTokenLink: () -> Unit,
    onAddAccount: (String, String, String?) -> Unit,
    onSetActiveAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
    var labelInput by remember { mutableStateOf("") }
    var patInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    val allScopes = listOf(
        Pair("repo", "Full control of private repositories"),
        Pair("workflow", "Update GitHub Action workflows"),
        Pair("read:org", "Read org and team membership"),
        Pair("gist", "Create and edit gists"),
        Pair("user:email", "Access user email address"),
        Pair("write:packages", "Upload packages to GitHub Registry")
    )

    SettingsSection(
        title = "GitHub Multi-Account & Scope Builder",
        subtitle = "Configure permission scopes and generate customized personal access tokens with 1 tap."
    ) {
        Text(
            "Select Permissions Needed:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            allScopes.forEach { (scope, desc) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleScope(scope) }
                ) {
                    Checkbox(
                        checked = selectedScopes.contains(scope),
                        onCheckedChange = { onToggleScope(scope) }
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(scope, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onGenerateTokenLink,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Generate Scoped Token on GitHub (1-Click)")
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(12.dp))

        Text(
            "Add GitHub Account",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        SimpleTextField(label = "Account Nickname", hint = "e.g. Personal, Work, Organization", value = labelInput, onValueChange = { labelInput = it })
        Spacer(Modifier.height(8.dp))
        SimpleTextField(label = "Username (Optional)", hint = "octocat", value = usernameInput, onValueChange = { usernameInput = it })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = patInput,
            onValueChange = { patInput = it },
            label = { Text("GitHub Token (ghp_... or github_pat_...)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.small
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                if (patInput.isNotBlank()) {
                    onAddAccount(labelInput, patInput, usernameInput)
                    labelInput = ""
                    patInput = ""
                    usernameInput = ""
                }
            },
            enabled = patInput.isNotBlank(),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add GitHub Account to Pool")
        }

        if (accounts.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Connected GitHub Accounts (${accounts.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                accounts.forEach { acc ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (acc.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (acc.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(acc.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    if (acc.isActive) {
                                        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primary) {
                                            Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                if (!acc.username.isNullOrBlank()) {
                                    Text("@${acc.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!acc.isActive) {
                                    TextButton(onClick = { onSetActiveAccount(acc.id) }) {
                                        Text("Set Active", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                IconButton(onClick = { onDeleteAccount(acc.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

