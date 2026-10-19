package com.dettle.app.ui.components

sealed interface MarkdownBlock {
    data class Text(val content: String) : MarkdownBlock
    data class Code(val language: String, val code: String) : MarkdownBlock
}

object MarkdownParser {
    fun parseBlocks(content: String): List<MarkdownBlock> {
        if (content.isBlank()) return emptyList()

        val blocks = mutableListOf<MarkdownBlock>()
        var cursor = 0
        val len = content.length

        while (cursor < len) {
            val codeStart = content.indexOf("```", cursor)
            if (codeStart == -1) {
                val textPart = content.substring(cursor)
                if (textPart.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Text(textPart))
                }
                break
            }

            // Leading text before the code block
            if (codeStart > cursor) {
                val leadingText = content.substring(cursor, codeStart)
                if (leadingText.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Text(leadingText))
                }
            }

            val afterFence = codeStart + 3
            val firstNewline = content.indexOf('\n', afterFence)

            if (firstNewline == -1) {
                val lang = content.substring(afterFence).trim()
                blocks.add(MarkdownBlock.Code(language = lang, code = ""))
                break
            }

            val lang = content.substring(afterFence, firstNewline).trim()
            val codeBodyStart = firstNewline + 1

            val codeEnd = content.indexOf("```", codeBodyStart)
            if (codeEnd == -1) {
                // Streaming or unclosed code block
                val codeBody = content.substring(codeBodyStart)
                blocks.add(MarkdownBlock.Code(language = lang, code = codeBody))
                break
            } else {
                val codeBody = content.substring(codeBodyStart, codeEnd)
                blocks.add(MarkdownBlock.Code(language = lang, code = codeBody))
                cursor = codeEnd + 3
                if (cursor < len && content[cursor] == '\n') {
                    cursor++
                }
            }
        }

        return blocks
    }
}
