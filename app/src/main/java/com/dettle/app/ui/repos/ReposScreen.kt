package com.dettle.app.ui.repos

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.data.github.RepoFile
import com.dettle.app.ui.theme.DettleCard
import com.dettle.app.ui.theme.DettleCardBorder
import com.dettle.app.ui.theme.DettleCyan
import com.dettle.app.ui.theme.DettleCyanDim
import com.dettle.app.ui.theme.DettleDark
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.DettleSurface
import com.dettle.app.ui.theme.DettleSurfaceVariant
import com.dettle.app.ui.theme.DettleTextMuted
import com.dettle.app.ui.theme.DettleTextSecondary

/**
 * Repos screen — browse and read GitHub repositories.
 *
 * Two modes:
 * 1. FILE TREE: Shows the full repo structure, file types, tap to open
 * 2. FILE VIEW: Monospace code viewer for the selected file
 *
 * The repo map (token-compressed summary of all signatures) is shown
 * at the top — this is what the agent actually uses for context.
 */
@Composable
fun ReposScreen(
    viewModel: ReposViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.selectedFile != null) {
        FileViewerScreen(
            file = state.selectedFile!!,
            onClose = viewModel::closeFile
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DettleDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────
        item {
            Text(
                "Repositories",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Browse repos · Read files · Give context to agent",
                style = MaterialTheme.typography.bodySmall,
                color = DettleTextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
        }

        // ── Repo input ───────────────────────────────────────────────────
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DettleTextField(
                    value = state.ownerInput,
                    onValueChange = viewModel::setOwnerInput,
                    placeholder = "owner",
                    modifier = Modifier.weight(1f)
                )
                Text("/", color = DettleTextMuted, style = MaterialTheme.typography.titleMedium)
                DettleTextField(
                    value = state.repoInput,
                    onValueChange = viewModel::setRepoInput,
                    placeholder = "repo",
                    modifier = Modifier.weight(2f),
                    onDone = { viewModel.loadRepo() }
                )
                Button(
                    onClick = viewModel::loadRepo,
                    enabled = state.ownerInput.isNotBlank() && state.repoInput.isNotBlank() && !state.isLoading,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DettleCyan, contentColor = DettleDark)
                ) {
                    Text("Load", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Loading indicator ────────────────────────────────────────────
        if (state.isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DettleCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Loading ${state.currentOwner}/${state.currentRepo}...", color = DettleCyan, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── Error ────────────────────────────────────────────────────────
        state.error?.let { error ->
            item {
                Text(
                    "❌ $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color(0xFFFF4444),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color(0xFFFF4444).copy(alpha = 0.1f))
                        .padding(12.dp)
                )
            }
        }

        // ── Repo map summary ─────────────────────────────────────────────
        if (state.repoSummary.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DettleSurface)
                        .border(1.dp, DettleCyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DettleCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("AGENT MAP", style = MaterialTheme.typography.labelSmall, color = DettleCyan)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("${state.files.size} files", style = MaterialTheme.typography.labelSmall, color = DettleTextMuted)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.repoSummary,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = DettleTextSecondary,
                        maxLines = 8
                    )
                }
            }
        }

        // ── File tree ────────────────────────────────────────────────────
        if (state.files.isNotEmpty()) {
            item {
                Text(
                    "FILES — ${state.currentOwner}/${state.currentRepo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DettleTextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
            items(state.files) { file ->
                RepoFileRow(
                    file = file,
                    onClick = { viewModel.openFile(state.currentOwner, state.currentRepo, file.path) }
                )
            }
        } else if (!state.isLoading && state.currentRepo.isBlank()) {
            item {
                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📁", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Enter owner/repo above", style = MaterialTheme.typography.titleMedium, color = DettleTextSecondary)
                        Text("Browse any public or private repo you have access to", style = MaterialTheme.typography.bodySmall, color = DettleTextMuted)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── File tree row ──────────────────────────────────────────────────────────

@Composable
fun RepoFileRow(file: RepoFile, onClick: () -> Unit) {
    val ext = file.path.substringAfterLast('.', "")
    val (icon, color) = when (ext.lowercase()) {
        "kt" -> "🟣" to DettleCyan
        "java" -> "☕" to DettleOrange
        "py" -> "🐍" to DettleGreen
        "js", "ts", "jsx", "tsx" -> "📜" to DettleOrange
        "json" -> "📋" to DettleTextSecondary
        "md" -> "📝" to DettleTextSecondary
        "gradle", "toml" -> "⚙️" to DettleTextMuted
        "xml" -> "📄" to DettleTextMuted
        "go" -> "🔵" to DettleCyan
        "rs" -> "🦀" to DettleOrange
        else -> "📄" to DettleTextMuted
    }

    val depth = file.path.count { it == '/' }
    val fileName = file.path.substringAfterLast('/')

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(DettleCard)
            .clickable(onClick = onClick)
            .padding(start = (8 + depth * 12).dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(8.dp))
        Text(
            fileName,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = color,
            modifier = Modifier.weight(1f)
        )
        Text(
            file.path.substringBeforeLast('/').let { if (it == file.path) "" else it },
            style = MaterialTheme.typography.labelSmall,
            color = DettleTextMuted,
            maxLines = 1
        )
    }
}

// ── File viewer ────────────────────────────────────────────────────────────

@Composable
fun FileViewerScreen(file: RepoFileView, onClose: () -> Unit) {
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
            Text(
                "← Back",
                style = MaterialTheme.typography.bodyMedium,
                color = DettleCyan,
                modifier = Modifier.clickable { onClose() }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                file.path.substringAfterLast('/'),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${file.content.lines().size} lines",
                style = MaterialTheme.typography.labelSmall,
                color = DettleTextMuted
            )
        }

        // Code content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val lines = file.content.lines()
            items(lines.indices.toList()) { idx ->
                Row(modifier = Modifier.padding(vertical = 1.dp)) {
                    Text(
                        text = "%4d".format(idx + 1),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = DettleTextMuted,
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        text = lines[idx],
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = DettleTextSecondary
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Shared TextField ──────────────────────────────────────────────────────

@Composable
fun DettleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = DettleTextMuted) },
        singleLine = true,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = DettleSurfaceVariant,
            unfocusedContainerColor = DettleSurface,
            focusedTextColor = Color.White,
            unfocusedTextColor = DettleTextSecondary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = DettleCyan
        ),
        keyboardOptions = KeyboardOptions(imeAction = if (onDone != null) ImeAction.Go else ImeAction.Next),
        keyboardActions = KeyboardActions(onGo = { onDone?.invoke() })
    )
}
