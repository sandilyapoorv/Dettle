package com.dettle.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testParseFencedCodeBlocks_closed() {
        val input = "Here is some code:\n```kotlin\nval x = 42\n```\nAnd more text."
        val blocks = MarkdownParser.parseBlocks(input)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Text)
        assertTrue(blocks[1] is MarkdownBlock.Code)
        assertTrue(blocks[2] is MarkdownBlock.Text)

        val codeBlock = blocks[1] as MarkdownBlock.Code
        assertEquals("kotlin", codeBlock.language)
        assertEquals("val x = 42", codeBlock.code.trim())
    }

    @Test
    fun testParseFencedCodeBlocks_unclosedStreaming() {
        // AI is currently streaming code; closing ``` has not arrived yet
        val input = "Before code:\n```python\ndef hello():\n    print('world')"
        val blocks = MarkdownParser.parseBlocks(input)
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Text)
        assertTrue(blocks[1] is MarkdownBlock.Code)

        val codeBlock = blocks[1] as MarkdownBlock.Code
        assertEquals("python", codeBlock.language)
        assertTrue(codeBlock.code.contains("print('world')"))
    }

    @Test
    fun testParseFencedCodeBlocks_noLanguage() {
        val input = "```\njust plain text code\n```"
        val blocks = MarkdownParser.parseBlocks(input)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Code)
        val codeBlock = blocks[0] as MarkdownBlock.Code
        assertEquals("", codeBlock.language)
        assertEquals("just plain text code", codeBlock.code.trim())
    }

    @Test
    fun testParsePlainTextOnly() {
        val input = "Hello this is a simple text with no code."
        val blocks = MarkdownParser.parseBlocks(input)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Text)
        assertEquals(input, (blocks[0] as MarkdownBlock.Text).content)
    }
}
