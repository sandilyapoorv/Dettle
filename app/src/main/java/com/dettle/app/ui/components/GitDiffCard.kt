package com.dettle.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dettle.app.ui.theme.CodeBlockStyle
import com.dettle.app.ui.theme.DettleGreen
import kotlinx.coroutines.delay

@Composable
fun GitDiffCard(
    diffText: String,
    filePath: String? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000L)
            isCopied = false
        }
    }

    val lines = remember(diffText) { diffText.lines() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0C0C0F),
        border = BorderStroke(1.dp, Color(0xFF272732))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Diff Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Difference,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = filePath ?: "GIT DIFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                AnimatedContent(
                    targetState = isCopied,
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    label = "copyDiffAnim"
                ) { copied ->
                    if (copied) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Copied",
                                tint = DettleGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Copied",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = DettleGreen
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(diffText))
                                isCopied = true
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy diff",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1E1E28), thickness = 0.5.dp)

            // Diff Lines with Color Highlighting
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp)
            ) {
                Column {
                    lines.forEach { line ->
                        val (textColor, bgColor) = when {
                            line.startsWith("+") && !line.startsWith("+++") ->
                                Pair(Color(0xFF4ADE80), Color(0x1F22C55E))
                            line.startsWith("-") && !line.startsWith("---") ->
                                Pair(Color(0xFFF87171), Color(0x1FEF4444))
                            line.startsWith("@@") ->
                                Pair(Color(0xFF38BDF8), Color(0x1438BDF8))
                            line.startsWith("diff ") || line.startsWith("index ") || line.startsWith("---") || line.startsWith("+++") ->
                                Pair(Color(0xFF71717A), Color.Transparent)
                            else ->
                                Pair(Color(0xFFD4D4D8), Color.Transparent)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .padding(horizontal = 10.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = line,
                                style = CodeBlockStyle.copy(fontSize = 12.sp, lineHeight = 17.sp),
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
