package com.dettle.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isStreaming: Boolean = false,
    streamingCursor: @Composable (() -> Unit)? = null
) {
    val blocks = remember(content) {
        MarkdownParser.parseBlocks(content)
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEachIndexed { index, block ->
            val isLast = index == blocks.size - 1
            when (block) {
                is MarkdownBlock.Code -> {
                    CodeBlockCard(
                        code = block.code,
                        language = block.language,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    if (isLast && isStreaming && streamingCursor != null) {
                        streamingCursor()
                    }
                }
                is MarkdownBlock.Text -> {
                    val formattedText = remember(block.content, primaryColor) {
                        formatInlineMarkdown(block.content, primaryColor)
                    }
                    if (isLast && isStreaming && streamingCursor != null) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formattedText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                ),
                                color = color,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            streamingCursor()
                        }
                    } else {
                        Text(
                            text = formattedText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            ),
                            color = color
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-performance inline formatter for bold (**text**), italics (*text*), and inline code (`code`).
 */
fun formatInlineMarkdown(text: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length
        while (i < len) {
            // Check inline code `...`
            if (text[i] == '`') {
                val nextTick = text.indexOf('`', i + 1)
                if (nextTick != -1) {
                    val codeSpan = text.substring(i + 1, nextTick)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.5.sp,
                            background = Color(0x28888888),
                            color = primaryColor
                        )
                    ) {
                        append(" $codeSpan ")
                    }
                    i = nextTick + 1
                    continue
                }
            }

            // Check bold **...**
            if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                val nextStar = text.indexOf("**", i + 2)
                if (nextStar != -1) {
                    val boldSpan = text.substring(i + 2, nextStar)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldSpan)
                    }
                    i = nextStar + 2
                    continue
                }
            }

            // Regular char
            append(text[i])
            i++
        }
    }
}
