package com.dettle.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.ui.theme.DettleCard
import com.dettle.app.ui.theme.DettleCardBorder
import com.dettle.app.ui.theme.DettleCyan
import com.dettle.app.ui.theme.DettleDark
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleRed
import com.dettle.app.ui.theme.DettleSurface
import com.dettle.app.ui.theme.DettleTextMuted
import com.dettle.app.ui.theme.DettleTextSecondary
import com.dettle.app.ui.theme.DettleTextPrimary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state = viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DettleDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // ── Free API Keys ────────────────────────────────────────────────
        SettingsSection(title = "Free API Keys", subtitle = "Add keys for each provider. All stored encrypted on-device.") {
            ApiKeyField(
                label = "Groq API Key",
                hint = "gsk_...",
                value = state.groqKey,
                isSaved = state.groqSaved,
                onSave = viewModel::saveGroqKey,
                link = "console.groq.com"
            )
            Spacer(Modifier.height(8.dp))
            ApiKeyField(
                label = "Google AI Studio (Gemini)",
                hint = "AIza...",
                value = state.geminiKey,
                isSaved = state.geminiSaved,
                onSave = viewModel::saveGeminiKey,
                link = "aistudio.google.com"
            )
            Spacer(Modifier.height(8.dp))
            ApiKeyField(
                label = "OpenRouter API Key",
                hint = "sk-or-...",
                value = state.openRouterKey,
                isSaved = state.openRouterSaved,
                onSave = viewModel::saveOpenRouterKey,
                link = "openrouter.ai/keys"
            )
            Spacer(Modifier.height(8.dp))
            ApiKeyField(
                label = "SambaNova API Key",
                hint = "sn-...",
                value = state.sambaNovaKey,
                isSaved = state.sambaNovaSaved,
                onSave = viewModel::saveSambaNovaKey,
                link = "cloud.sambanova.ai"
            )
            Spacer(Modifier.height(8.dp))
            ApiKeyField(
                label = "GitHub Models Token (PAT)",
                hint = "ghp_...",
                value = state.githubModelsKey,
                isSaved = state.githubModelsSaved,
                onSave = viewModel::saveGitHubModelsKey,
                link = "github.com/settings/tokens"
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── GitHub Integration ───────────────────────────────────────────
        SettingsSection(title = "GitHub Integration", subtitle = "PAT scoped to: contents, pull_requests, actions (read/write)") {
            ApiKeyField(
                label = "GitHub Personal Access Token",
                hint = "ghp_... or github_pat_...",
                value = state.githubPat,
                isSaved = state.githubPatSaved,
                onSave = viewModel::saveGithubPat,
                link = "github.com/settings/tokens"
            )
            Spacer(Modifier.height(8.dp))
            SimpleTextField(
                label = "Default GitHub Owner/Org",
                hint = "your-username-or-org",
                value = state.githubOwner,
                onValueChange = viewModel::setGithubOwner
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Cloudflare ───────────────────────────────────────────────────
        SettingsSection(title = "Cloudflare", subtitle = "Free tier: 500 builds/month, 100K Worker requests/day") {
            ApiKeyField(
                label = "Cloudflare API Token",
                hint = "...",
                value = state.cloudflareToken,
                isSaved = state.cloudflareSaved,
                onSave = viewModel::saveCloudflareToken,
                link = "dash.cloudflare.com/profile/api-tokens"
            )
            Spacer(Modifier.height(8.dp))
            SimpleTextField(
                label = "Cloudflare Account ID",
                hint = "32-character hex string",
                value = state.cloudflareAccountId,
                onValueChange = viewModel::setCloudflareAccountId
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Google Drive ─────────────────────────────────────────────────
        GoogleDriveCard(
            isConnected = state.driveConnected,
            userEmail = state.driveUserEmail,
            onConnect = {
                // Launch Google Sign-In — handled in the screen via Credential Manager
                // The ViewModel's onDriveSignInResult() is called after success
            },
            onDisconnect = viewModel::disconnectDrive
        )

        Spacer(Modifier.height(16.dp))

        // ── Provider Status ──────────────────────────────────────────────
        SettingsSection(title = "Provider Budget Today", subtitle = "Resets at midnight") {
            state.providerStatuses.forEach { status ->
                ProviderStatusRow(status)
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DettleCard)
            .border(1.dp, DettleCardBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DettleTextMuted)
        Spacer(Modifier.height(12.dp))
        content()
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

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = DettleTextSecondary)
            Spacer(Modifier.weight(1f))
            if (isSaved && !editing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, null, tint = DettleGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Saved", style = MaterialTheme.typography.labelSmall, color = DettleGreen)
                }
            }
        }
        if (link.isNotEmpty()) {
            Text("↗ $link", style = MaterialTheme.typography.labelSmall, color = DettleCyan.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(4.dp))

        if (editing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(hint, color = DettleTextMuted, style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(8.dp),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = DettleTextMuted)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DettleCyan.copy(alpha = 0.5f),
                        unfocusedBorderColor = DettleCardBorder,
                        focusedContainerColor = DettleSurface,
                        unfocusedContainerColor = DettleSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(text); editing = false },
                    enabled = text.isNotBlank() && text != "••••••••••••",
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DettleCyan, contentColor = DettleDark)
                ) {
                    Text("Save", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DettleSurface)
                    .border(1.dp, DettleCardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Key, null, tint = DettleTextMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("••••••••••••", color = DettleTextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(
                    "Change",
                    style = MaterialTheme.typography.labelSmall,
                    color = DettleCyan,
                    modifier = androidx.compose.ui.Modifier.clickable { editing = true; text = "" }
                )
            }
        }
    }
}

@Composable
fun SimpleTextField(label: String, hint: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = DettleTextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(hint, color = DettleTextMuted, style = MaterialTheme.typography.bodySmall) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DettleCyan.copy(alpha = 0.5f),
                unfocusedBorderColor = DettleCardBorder,
                focusedContainerColor = DettleSurface,
                unfocusedContainerColor = DettleSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ProviderStatusRow(status: com.dettle.app.data.api.ProviderStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(when {
                    !status.isAvailable -> DettleRed
                    status.requestsPercentUsed > 0.8f -> DettleOrange
                    else -> DettleGreen
                })
        )
        Spacer(Modifier.width(8.dp))
        Text(
            status.model.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = DettleTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (status.dailyRequestLimit > 0) "${status.requestsUsedToday}/${status.dailyRequestLimit} req"
            else "${status.requestsUsedToday} req",
            style = MaterialTheme.typography.labelSmall,
            color = DettleTextMuted
        )
    }
}

private val DettleOrange = Color(0xFFFF6B35)

// ─── Google Drive Card ─────────────────────────────────────────────────────

@Composable
fun GoogleDriveCard(
    isConnected: Boolean,
    userEmail: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DettleCard)
            .border(
                1.dp,
                if (isConnected) DettleGreen.copy(alpha = 0.4f) else DettleCardBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Google Drive icon (using text placeholder — replace with actual icon)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A73E8).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("▲", color = Color(0xFF1A73E8), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Google Drive",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isConnected) "Connected • $userEmail"
                    else "Not connected — tap to link your Gmail account",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) DettleGreen else DettleTextMuted
                )
            }
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DettleGreen)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // What Dettle syncs to Drive
        if (isConnected) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(
                    "📄 Overnight run summaries",
                    "💾 Conversation export logs",
                    "📐 Architecture documentation",
                    "🗂️ Repo snapshots"
                ).forEach { item ->
                    Text(item, style = MaterialTheme.typography.bodySmall, color = DettleTextSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DettleRed.copy(alpha = 0.15f),
                        contentColor = DettleRed
                    )
                ) {
                    Text("Disconnect", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A73E8),
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Connect Google Drive",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Scope: drive.file (only files Dettle creates — not full Drive access)",
                style = MaterialTheme.typography.labelSmall,
                color = DettleTextMuted
            )
        }
    }
}
