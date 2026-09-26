package com.dettle.app.ui.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

data class ChatAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uri: Uri,
    val textSnippet: String? = null,
    val isImage: Boolean = false
)

object AttachmentHelper {
    private const val TAG = "AttachmentHelper"
    private const val MAX_TEXT_BYTES = 100_000 // 100KB max per document snippet

    fun resolveAttachment(context: Context, uri: Uri): ChatAttachment {
        var fileName = "attachment"
        var fileSize = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve OpenableColumns for $uri", e)
        }

        val resolvedMime = context.contentResolver.getType(uri) ?: guessMimeType(fileName)
        val isImg = resolvedMime.startsWith("image/")

        var extractedText: String? = null
        if (!isImg && isTextReadable(fileName, resolvedMime)) {
            extractedText = readTextContent(context, uri)
        }

        return ChatAttachment(
            name = fileName,
            mimeType = resolvedMime,
            sizeBytes = fileSize,
            uri = uri,
            textSnippet = extractedText,
            isImage = isImg
        )
    }

    fun formatAttachmentsForPrompt(userPrompt: String, attachments: List<ChatAttachment>): String {
        if (attachments.isEmpty()) return userPrompt

        val sb = StringBuilder()
        sb.append("=== ATTACHED CONTEXT (${attachments.size} files) ===\n\n")

        attachments.forEachIndexed { index, att ->
            sb.append("--- Attachment ${index + 1}: ${att.name} (${formatFileSize(att.sizeBytes)}, ${att.mimeType}) ---\n")
            if (att.isImage) {
                sb.append("[Photo/Image: ${att.name} (${formatFileSize(att.sizeBytes)})]\n")
            } else if (!att.textSnippet.isNullOrBlank()) {
                sb.append(att.textSnippet).append("\n")
            } else {
                sb.append("[Document File: ${att.name}]\n")
            }
            sb.append("\n")
        }

        sb.append("=== USER PROMPT ===\n")
        sb.append(userPrompt.ifBlank { "Please inspect and analyze the attached files." })
        return sb.toString()
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.1f MB", mb)
    }

    private fun isTextReadable(name: String, mime: String): Boolean {
        if (mime.startsWith("text/")) return true
        if (mime in listOf("application/json", "application/xml", "application/javascript", "application/x-yaml", "application/pdf")) return true
        val lower = name.lowercase()
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".json") ||
                lower.endsWith(".kt") || lower.endsWith(".java") || lower.endsWith(".py") ||
                lower.endsWith(".js") || lower.endsWith(".ts") || lower.endsWith(".tsx") ||
                lower.endsWith(".html") || lower.endsWith(".css") || lower.endsWith(".csv") ||
                lower.endsWith(".xml") || lower.endsWith(".gradle") || lower.endsWith(".kts") ||
                lower.endsWith(".sh") || lower.endsWith(".log") || lower.endsWith(".yaml") ||
                lower.endsWith(".yml") || lower.endsWith(".sql") || lower.endsWith(".c") ||
                lower.endsWith(".cpp") || lower.endsWith(".h")
    }

    private fun readTextContent(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                val buffer = CharArray(1024)
                val sb = StringBuilder()
                var totalRead = 0
                var readCount: Int
                while (reader.read(buffer).also { readCount = it } != -1) {
                    sb.append(buffer, 0, readCount)
                    totalRead += readCount
                    if (totalRead >= MAX_TEXT_BYTES) {
                        sb.append("\n... [truncated for token budget] ...")
                        break
                    }
                }
                sb.toString()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading text from $uri", e)
            null
        }
    }

    private fun guessMimeType(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".txt") || lower.endsWith(".md") -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
