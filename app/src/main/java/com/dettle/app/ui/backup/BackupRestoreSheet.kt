package com.dettle.app.ui.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dettle.app.data.backup.BackupOptions
import com.dettle.app.data.backup.BackupSummary
import com.dettle.app.data.backup.RestoreResult
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreSheet(
    summary: BackupSummary?,
    onDismiss: () -> Unit,
    onCreateBackupJson: suspend (BackupOptions) -> String,
    onRestoreBackup: suspend (String, BackupOptions) -> RestoreResult,
    onExportToFile: (String) -> android.net.Uri,
    onReadFromUri: (android.net.Uri) -> String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Backup, 1 = Restore

    // Granular checkboxes
    var includeApis by remember { mutableStateOf(true) }
    var includeSubscriptions by remember { mutableStateOf(true) }
    var includeProjects by remember { mutableStateOf(true) }
    var includeChatsAndLogs by remember { mutableStateOf(true) }
    var includeAppSettings by remember { mutableStateOf(true) }

    val options = remember(includeApis, includeSubscriptions, includeProjects, includeChatsAndLogs, includeAppSettings) {
        BackupOptions(
            includeApis = includeApis,
            includeSubscriptions = includeSubscriptions,
            includeProjects = includeProjects,
            includeChatsAndLogs = includeChatsAndLogs,
            includeAppSettings = includeAppSettings
        )
    }

    // State for Restore
    var restoreInputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var restoreResult by remember { mutableStateOf<RestoreResult?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val content = onReadFromUri(uri)
                restoreInputText = content
                Toast.makeText(context, "Backup file loaded", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Backup & Restore",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Granular on-device backup of credentials, workspaces, and logs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "Done",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Mode Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; restoreResult = null },
                    text = { Text("Create Backup", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; restoreResult = null },
                    text = { Text("Restore Data", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) }
                )
            }

            // Granular Module Selector Card
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Select Modules to Include",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val allSelected = options.isAllSelected
                        OutlinedButton(
                            onClick = {
                                val target = !allSelected
                                includeApis = target
                                includeSubscriptions = target
                                includeProjects = target
                                includeChatsAndLogs = target
                                includeAppSettings = target
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (allSelected) "Deselect All" else "Select All", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    ModuleCheckRow(
                        label = "API Keys & Accounts",
                        subtitle = "${summary?.apiCount ?: 0} accounts configured",
                        icon = Icons.Outlined.Key,
                        checked = includeApis,
                        onCheckedChange = { includeApis = it }
                    )

                    ModuleCheckRow(
                        label = "Subscription Providers",
                        subtitle = "${summary?.subscriptionCount ?: 0} ChatGPT / Claude / Web accounts",
                        icon = Icons.Outlined.Refresh,
                        checked = includeSubscriptions,
                        onCheckedChange = { includeSubscriptions = it }
                    )

                    ModuleCheckRow(
                        label = "Projects & Workspaces",
                        subtitle = "${summary?.projectCount ?: 0} projects configured",
                        icon = Icons.Outlined.FolderCopy,
                        checked = includeProjects,
                        onCheckedChange = { includeProjects = it }
                    )

                    ModuleCheckRow(
                        label = "Chat Histories & Task Logs",
                        subtitle = "${summary?.chatCount ?: 0} conversations, ${summary?.logCount ?: 0} task logs",
                        icon = Icons.Outlined.Description,
                        checked = includeChatsAndLogs,
                        onCheckedChange = { includeChatsAndLogs = it }
                    )

                    ModuleCheckRow(
                        label = "App Settings & Endpoints",
                        subtitle = "GitHub repo, custom selector endpoints",
                        icon = Icons.Outlined.Settings,
                        checked = includeAppSettings,
                        onCheckedChange = { includeAppSettings = it }
                    )
                }
            }

            // Tab Content: Backup vs Restore
            if (selectedTab == 0) {
                // Export Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (options.isNoneSelected) {
                                Toast.makeText(context, "Select at least one module", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                isProcessing = true
                                try {
                                    val json = onCreateBackupJson(options)
                                    val uri = onExportToFile(json)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Save or Share Backup"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = !isProcessing && !options.isNoneSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export & Share Backup (.json)", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (options.isNoneSelected) {
                                Toast.makeText(context, "Select at least one module", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            scope.launch {
                                isProcessing = true
                                try {
                                    val json = onCreateBackupJson(options)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Dettle Backup", json))
                                    Toast.makeText(context, "Backup JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Copy failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = !isProcessing && !options.isNoneSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Backup JSON to Clipboard")
                    }
                }
            } else {
                // Restore Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("application/json") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pick File (.json)")
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val item = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!item.isNullOrBlank()) {
                                    restoreInputText = item
                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Paste JSON")
                        }
                    }

                    OutlinedTextField(
                        value = restoreInputText,
                        onValueChange = { restoreInputText = it; restoreResult = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        label = { Text("Backup JSON Content") },
                        placeholder = { Text("Paste valid JSON backup string or select file above") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 6
                    )

                    Button(
                        onClick = {
                            if (restoreInputText.isBlank()) {
                                Toast.makeText(context, "Provide backup JSON first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                isProcessing = true
                                try {
                                    restoreResult = onRestoreBackup(restoreInputText.trim(), options)
                                } catch (e: Exception) {
                                    restoreResult = RestoreResult(success = false, message = e.message ?: "Unknown error")
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = !isProcessing && restoreInputText.isNotBlank() && !options.isNoneSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DettleGreen)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Restore Selected Modules", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Result Banner
                    restoreResult?.let { res ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (res.success) DettleGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (res.success) DettleGreen else MaterialTheme.colorScheme.error)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (res.success) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = if (res.success) DettleGreen else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (res.success) "Restore Successful" else "Restore Failed",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    res.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (res.success) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Restored: ${res.apisRestored} APIs, ${res.subscriptionsRestored} subscriptions, ${res.projectsRestored} projects, ${res.chatsRestored} chats, ${res.logsRestored} logs",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModuleCheckRow(
    label: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
