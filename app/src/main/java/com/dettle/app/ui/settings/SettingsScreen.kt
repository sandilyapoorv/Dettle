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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.ProviderAccount
import com.dettle.app.domain.model.GitHubAccount
import com.dettle.app.data.backup.BackupSummary
import com.dettle.app.ui.backup.BackupRestoreSheet
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.ui.theme.AppTheme
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.ThemeConfig
import com.dettle.app.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val state = viewModel.state
    val themeConfig by viewModel.themeConfig.collectAsState(initial = ThemeConfig())
    var showBackupSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme & Appearance Customization Card
            item {
                ThemeAppearanceCard(
                    config = themeConfig,
                    onSelectTheme = viewModel::setTheme,
                    onSelectMode = viewModel::setThemeMode,
                    onToggleOled = viewModel::setPureOled,
                    onSelectAccent = viewModel::setCustomAccent,
                    onReset = viewModel::resetTheme
                )
            }

            // Aggregate Mathematics & Token Metrics Card
            item {
                AggregateMetricsCard(metrics = state.aggregateMetrics)
            }

            // Unleashed / Uncensored Engine Card
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.isUnleashed) DettleGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (state.isUnleashed) DettleGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isUnleashed) DettleGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.ElectricBolt,
                                        contentDescription = null,
                                        tint = if (state.isUnleashed) DettleGreen else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "Unleashed Engine",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isUnleashed) DettleGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (state.isUnleashed) "Raw Execution & Prompt Smuggling Active" else "Standard Engine (Karpathy Baseline)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = state.isUnleashed,
                                onCheckedChange = { viewModel.setUnleashed(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DettleGreen
                                )
                            )
                        }

                        Text(
                            "When enabled, removes AI apologies, boilerplate disclaimers, and moral lecturing. " +
                            "If commercial APIs refuse a task, Prompt Smuggler automatically fragments it into sterile logic bricks, queries the API, and restores the uncensored result.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
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

            // Granular Backup & Restore
            item {
                BackupRestoreCard(
                    summary = viewModel.backupSummary,
                    onOpenBackup = {
                        viewModel.refreshBackupSummary()
                        showBackupSheet = true
                    }
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

    if (showBackupSheet) {
        BackupRestoreSheet(
            summary = viewModel.backupSummary,
            onDismiss = { showBackupSheet = false },
            onCreateBackupJson = viewModel::createBackupJson,
            onRestoreBackup = viewModel::restoreBackup,
            onExportToFile = viewModel::exportBackupToFile,
            onReadFromUri = viewModel::readBackupUri
        )
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
fun BackupRestoreCard(
    summary: BackupSummary?,
    onOpenBackup: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icons.Outlined.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Granular on-device backup with custom module selection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (summary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${summary.apiCount + summary.subscriptionCount}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Accounts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${summary.projectCount}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("Projects", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${summary.chatCount}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = DettleGreen)
                            Text("Chats", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Button(
                onClick = onOpenBackup,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Manage Backup & Restore", fontWeight = FontWeight.SemiBold)
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

data class ProviderMeta(
    val type: AIProviderType,
    val portalUrl: String,
    val portalLabel: String,
    val keyHint: String,
    val badge: String,
    val description: String
)

val PROVIDER_METAS = mapOf(
    AIProviderType.GROQ to ProviderMeta(
        type = AIProviderType.GROQ,
        portalUrl = "https://console.groq.com/keys",
        portalLabel = "console.groq.com",
        keyHint = "gsk_...",
        badge = "300+ tok/s Free",
        description = "Ultra-fast inference. Free Llama 3.3 70B & Qwen 2.5 32B."
    ),
    AIProviderType.GEMINI to ProviderMeta(
        type = AIProviderType.GEMINI,
        portalUrl = "https://aistudio.google.com/app/apikey",
        portalLabel = "aistudio.google.com",
        keyHint = "AIzaSy...",
        badge = "1M Tokens Free",
        description = "Google AI Studio free tier. Massive 1M context (Gemini 2.5 Flash & Pro)."
    ),
    AIProviderType.OPENROUTER to ProviderMeta(
        type = AIProviderType.OPENROUTER,
        portalUrl = "https://openrouter.ai/keys",
        portalLabel = "openrouter.ai/keys",
        keyHint = "sk-or-...",
        badge = "Multi-Model Free",
        description = "Unified gateway to Llama 4 Maverick Free, Qwen 2.5 72B Free & DeepSeek R1."
    ),
    AIProviderType.SAMBANOVA to ProviderMeta(
        type = AIProviderType.SAMBANOVA,
        portalUrl = "https://cloud.sambanova.ai/apis",
        portalLabel = "cloud.sambanova.ai",
        keyHint = "sn_...",
        badge = "Llama 405B Free",
        description = "Full-parameter Llama 3.1 405B & 70B on SambaNova Cloud."
    ),
    AIProviderType.GITHUB_MODELS to ProviderMeta(
        type = AIProviderType.GITHUB_MODELS,
        portalUrl = "https://github.com/marketplace/models",
        portalLabel = "github.com/marketplace/models",
        keyHint = "ghp_...",
        badge = "GitHub Free PAT",
        description = "Access GPT-4o, Claude 3.5, and o1 with your GitHub Personal Access Token."
    ),
    AIProviderType.DEEPSEEK to ProviderMeta(
        type = AIProviderType.DEEPSEEK,
        portalUrl = "https://platform.deepseek.com/api_keys",
        portalLabel = "platform.deepseek.com",
        keyHint = "sk-...",
        badge = "DeepSeek R1",
        description = "DeepSeek API for cost-efficient R1 reasoning and V3 coding."
    ),
    AIProviderType.OPENAI to ProviderMeta(
        type = AIProviderType.OPENAI,
        portalUrl = "https://platform.openai.com/api-keys",
        portalLabel = "platform.openai.com",
        keyHint = "sk-proj-...",
        badge = "Direct OpenAI",
        description = "Direct OpenAI API key for GPT-4o and o3-mini."
    ),
    AIProviderType.ANTHROPIC to ProviderMeta(
        type = AIProviderType.ANTHROPIC,
        portalUrl = "https://console.anthropic.com/settings/keys",
        portalLabel = "console.anthropic.com",
        keyHint = "sk-ant-...",
        badge = "Direct Claude",
        description = "Direct Anthropic key for Claude 3.7 Sonnet & 3.5 Haiku."
    ),
    AIProviderType.MISTRAL to ProviderMeta(
        type = AIProviderType.MISTRAL,
        portalUrl = "https://console.mistral.ai/api-keys",
        portalLabel = "console.mistral.ai",
        keyHint = "...",
        badge = "Codestral",
        description = "Mistral API for Codestral and Mistral Large."
    ),
    AIProviderType.CEREBRAS to ProviderMeta(
        type = AIProviderType.CEREBRAS,
        portalUrl = "https://cloud.cerebras.ai",
        portalLabel = "cloud.cerebras.ai",
        keyHint = "csk-...",
        badge = "2000 tok/s",
        description = "Wafer-scale engine with world-record 2,000+ tokens/second."
    ),
    AIProviderType.XAI to ProviderMeta(
        type = AIProviderType.XAI,
        portalUrl = "https://console.x.ai",
        portalLabel = "console.x.ai",
        keyHint = "xai-...",
        badge = "Grok 3",
        description = "xAI API for Grok 2 and Grok 3 beta reasoning."
    ),
    AIProviderType.LOCAL_OLLAMA to ProviderMeta(
        type = AIProviderType.LOCAL_OLLAMA,
        portalUrl = "http://localhost:11434",
        portalLabel = "localhost:11434",
        keyHint = "ollama / none",
        badge = "Local / Offline",
        description = "On-device or local LAN Ollama server. No cloud required."
    )
)

@Composable
fun QuickProviderCard(
    name: String,
    badge: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (count > 0) DettleGreen else DettleOrange)
                )
            }
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (count > 0) "$count active" else "Not set",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (count > 0) "+ Add" else "Link",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MultiAccountPoolSection(
    accounts: List<ProviderAccount>,
    onAddAccount: (AIProviderType, String, String) -> Unit,
    onToggleAccount: (String, Boolean) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var sheetProvider by remember { mutableStateOf(AIProviderType.GROQ) }

    val groqCount = accounts.count { it.provider == AIProviderType.GROQ && it.isActive }
    val geminiCount = accounts.count { it.provider == AIProviderType.GEMINI && it.isActive }
    val openRouterCount = accounts.count { it.provider == AIProviderType.OPENROUTER && it.isActive }

    fun openAdd(provider: AIProviderType) {
        sheetProvider = provider
        showAddSheet = true
    }

    SettingsSection(
        title = "Unlimited Multi-Account API Pool",
        subtitle = "Add unlimited accounts/keys per provider. Automatic failover and token load-balancing."
    ) {
        // Quick Provider Cards for the Big 3 Free Providers (Groq, Google AI Studio, OpenRouter)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickProviderCard(
                name = "Google AI",
                badge = "1M Free",
                count = geminiCount,
                onClick = { openAdd(AIProviderType.GEMINI) },
                modifier = Modifier.weight(1f)
            )
            QuickProviderCard(
                name = "Groq",
                badge = "300+ tok/s",
                count = groqCount,
                onClick = { openAdd(AIProviderType.GROQ) },
                modifier = Modifier.weight(1f)
            )
            QuickProviderCard(
                name = "OpenRouter",
                badge = "Free Tier",
                count = openRouterCount,
                onClick = { openAdd(AIProviderType.OPENROUTER) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

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
                onClick = { openAdd(AIProviderType.GROQ) },
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Key", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (accounts.isEmpty()) {
            Text(
                "No keys linked yet. Tap any provider above or 'Add Key' to connect your free Groq, Gemini, or OpenRouter keys.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                accounts.forEach { acc ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        acc.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            acc.provider.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    "${acc.requestsUsed} reqs • ${acc.tokensUsed} tokens",
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
    }

    if (showAddSheet) {
        AddProviderAccountSheet(
            initialProvider = sheetProvider,
            onDismiss = { showAddSheet = false },
            onSave = { prov, label, key ->
                onAddAccount(prov, label, key)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderAccountSheet(
    initialProvider: AIProviderType = AIProviderType.GROQ,
    onDismiss: () -> Unit,
    onSave: (AIProviderType, String, String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedProvider by remember { mutableStateOf(initialProvider) }
    var apiKey by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }

    val meta = PROVIDER_METAS[selectedProvider] ?: ProviderMeta(
        type = selectedProvider,
        portalUrl = "",
        portalLabel = "",
        keyHint = "API Key / Token",
        badge = "API Key",
        description = "Add credentials for ${selectedProvider.displayName}."
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Column {
                Text(
                    "Add Provider API Key",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Encrypted on-device with AES-256-GCM. Never leaves your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Core Free Providers Selector Row
            Text(
                "Core Free Providers",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    AIProviderType.GEMINI to "Google AI Studio",
                    AIProviderType.GROQ to "Groq",
                    AIProviderType.OPENROUTER to "OpenRouter"
                ).forEach { (prov, label) ->
                    val isSelected = prov == selectedProvider
                    Surface(
                        onClick = { selectedProvider = prov },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Other Providers Row
            Text(
                "Other Supported Providers",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val others = listOf(
                    AIProviderType.SAMBANOVA,
                    AIProviderType.GITHUB_MODELS,
                    AIProviderType.DEEPSEEK,
                    AIProviderType.OPENAI,
                    AIProviderType.ANTHROPIC,
                    AIProviderType.MISTRAL,
                    AIProviderType.CEREBRAS,
                    AIProviderType.XAI,
                    AIProviderType.LOCAL_OLLAMA
                )
                items(others) { prov ->
                    val isSelected = prov == selectedProvider
                    Surface(
                        onClick = { selectedProvider = prov },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            prov.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Context Banner with 1-Tap Portal Link
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                meta.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (meta.portalUrl.isNotBlank()) {
                            Row(
                                modifier = Modifier.clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meta.portalUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Get Free Key (${meta.portalLabel})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    Icons.Outlined.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    Text(
                        meta.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // API Key Input with Paste & Mask Toggle
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("${selectedProvider.displayName} API Key") },
                placeholder = { Text(meta.keyHint) },
                singleLine = true,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                apiKey = clip.trim()
                            }
                        }) {
                            Icon(
                                Icons.Outlined.ContentPaste,
                                contentDescription = "Paste from clipboard",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                if (isKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle key visibility",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )

            // Nickname (Compact & Optional)
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Account Nickname (Optional)") },
                placeholder = { Text("${selectedProvider.displayName} Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val key = apiKey.trim()
                        if (key.isNotBlank()) {
                            val finalLabel = nickname.trim().ifBlank { "${selectedProvider.displayName} Key" }
                            onSave(selectedProvider, finalLabel, key)
                            onDismiss()
                        }
                    },
                    enabled = apiKey.isNotBlank(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Save to Pool")
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

@Composable
fun ThemeAppearanceCard(
    config: ThemeConfig,
    onSelectTheme: (AppTheme) -> Unit,
    onSelectMode: (ThemeMode) -> Unit,
    onToggleOled: (Boolean) -> Unit,
    onSelectAccent: (Long?) -> Unit,
    onReset: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            "Appearance & Themes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Choose an aesthetic and customize accents",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.RestartAlt,
                        contentDescription = "Reset theme",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Theme selector horizontal list
            Text(
                "THEMES (${AppTheme.values().size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AppTheme.values().toList(), key = { it.id }) { theme ->
                    val isSelected = config.theme == theme
                    Surface(
                        onClick = { onSelectTheme(theme) },
                        shape = RoundedCornerShape(14.dp),
                        color = theme.previewBackground,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .width(135.dp)
                            .height(86.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(theme.previewPrimary)
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = theme.previewPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    theme.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (theme == AppTheme.APPLE_LIGHT) Color(0xFF1D1D1F) else Color(0xFFF3F4F6),
                                    maxLines = 1
                                )
                                Text(
                                    theme.description,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (theme == AppTheme.APPLE_LIGHT) Color(0xFF6E6E73) else Color(0xFF8E95A2),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Mode Selector: System / Dark / Light
            Text(
                "COLOR MODE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.values().forEach { mode ->
                    val isSelected = config.mode == mode
                    Surface(
                        onClick = { onSelectMode(mode) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                mode.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Custom Accent Color
            Text(
                "CUSTOM ACCENT COLOR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            val accents = listOf(
                Pair("Default", null),
                Pair("Indigo", 0xFF5E6AD2L),
                Pair("Apple Blue", 0xFF0071E3L),
                Pair("Cyan", 0xFF06B6D4L),
                Pair("Emerald", 0xFF10B981L),
                Pair("Amber", 0xFFF59E0BL),
                Pair("Crimson", 0xFFEF4444L),
                Pair("Violet", 0xFF8B5CF6L),
                Pair("Titanium", 0xFF949AA4L)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(accents) { (name, hex) ->
                    val isSelected = config.customAccentHex == hex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSelectAccent(hex) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (hex != null) Color(hex) else MaterialTheme.colorScheme.outlineVariant)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pure OLED AMOLED Black Switch
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Pure OLED AMOLED Black",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Sets backgrounds to absolute #000000 to maximize OLED battery efficiency",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = config.isPureOled,
                        onCheckedChange = onToggleOled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}


