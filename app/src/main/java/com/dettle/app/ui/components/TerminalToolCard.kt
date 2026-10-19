package com.dettle.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dettle.app.domain.model.ChatMessage
import com.dettle.app.ui.theme.CodeBlockStyle
import com.dettle.app.ui.theme.DettleGreen

fun getToolIcon(name: String): ImageVector = when {
    name.contains("command", ignoreCase = true) || name.contains("bash", ignoreCase = true) -> Icons.Outlined.Terminal
    name.contains("file", ignoreCase = true) || name.contains("write", ignoreCase = true) || name.contains("replace", ignoreCase = true) -> Icons.Outlined.Code
    name.contains("read", ignoreCase = true) || name.contains("dir", ignoreCase = true) -> Icons.Outlined.FolderOpen
    name.contains("search", ignoreCase = true) || name.contains("grep", ignoreCase = true) -> Icons.Outlined.Search
    name.contains("deploy", ignoreCase = true) || name.contains("cloudflare", ignoreCase = true) -> Icons.Outlined.CloudUpload
    else -> Icons.Outlined.Description
}

fun isDiffContent(content: String): Boolean {
    val lines = content.lines()
    val hasDiffHeader = lines.any { it.startsWith("diff --git") || it.startsWith("--- a/") }
    val hasDiffChunks = lines.any { it.startsWith("@@ ") }
    return hasDiffHeader || (hasDiffChunks && lines.any { it.startsWith("+") || it.startsWith("-") })
}

@Composable
fun TerminalToolCallCard(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val toolCall = message.toolCall
    val toolName = toolCall?.name ?: "Executing Tool"
    val icon = remember(toolName) { getToolIcon(toolName) }
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF101014),
        border = BorderStroke(1.dp, Color(0xFF272733))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = toolName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "RUNNING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 1.5.dp
                )
            }

            // Arguments preview
            val args = toolCall?.arguments
            if (!args.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    args.forEach { (k, v) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF181820),
                            border = BorderStroke(0.5.dp, Color(0xFF2E2E3C))
                        ) {
                            Text(
                                text = "$k: ${v.take(50)}${if (v.length > 50) "..." else ""}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontFamily = CodeBlockStyle.fontFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalToolResultCard(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val result = message.toolResult
    val isError = result?.isError == true
    val content = result?.content ?: message.content
    val toolName = result?.toolName ?: "Tool Result"
    val icon = remember(toolName) { getToolIcon(toolName) }
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(false) }

    // If result contains a Git diff, render dedicated GitDiffCard
    if (isDiffContent(content)) {
        GitDiffCard(
            diffText = content,
            filePath = toolName,
            modifier = modifier
        )
        return
    }

    val lines = remember(content) { content.lines() }
    val isLongOutput = lines.size > 8 || content.length > 400

    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f) else Color(0xFF101014),
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.45f) else Color(0xFF272733)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) MaterialTheme.colorScheme.error else DettleGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = toolName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isError) "FAILED" else "SUCCESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isError) MaterialTheme.colorScheme.error else DettleGreen
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(content)) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy output",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (isLongOutput) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = "Toggle output length",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else Color(0xFF1E1E28),
                thickness = 0.5.dp
            )

            // Stdout content
            val displayLines = if (!isExpanded && isLongOutput) lines.take(8) else lines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isExpanded) Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()) else Modifier)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    displayLines.forEach { line ->
                        Text(
                            text = line,
                            style = CodeBlockStyle.copy(fontSize = 12.sp, lineHeight = 17.sp),
                            color = if (isError) MaterialTheme.colorScheme.error else Color(0xFFD4D4D8)
                        )
                    }
                    if (!isExpanded && isLongOutput) {
                        Text(
                            text = "... (${lines.size - 8} more lines, tap expand to view)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { isExpanded = true }
                        )
                    }
                }
            }
        }
    }
}
