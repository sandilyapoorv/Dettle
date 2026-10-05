package com.dettle.app.orchestrator.rag

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class RawCodeChunk(
    val filePath: String,
    val symbolName: String,
    val symbolType: String,
    val content: String,
    val startLine: Int,
    val endLine: Int
)

/**
 * Symbol-aware source code chunker.
 * Extracts functions, classes, interfaces, and logical blocks from code files
 * to produce semantic chunks for RAG embedding and FTS retrieval.
 */
@Singleton
class CodebaseChunker @Inject constructor() {

    private val symbolPatterns = listOf(
        // Kotlin / Java
        Regex("""(?m)^[ \t]*(?:public|private|protected|internal|override|open|abstract|suspend|data)?[ \t]*(?:fun|class|interface|object|enum class)[ \t]+([A-Za-z0-9_]+)"""),
        // TypeScript / JavaScript
        Regex("""(?m)^[ \t]*(?:export[ \t]+)?(?:default[ \t]+)?(?:async[ \t]+)?(?:function|class|interface|type)[ \t]+([A-Za-z0-9_]+)"""),
        Regex("""(?m)^[ \t]*(?:export[ \t]+)?(?:const|let|var)[ \t]+([A-Za-z0-9_]+)[ \t]*=[ \t]*(?:async[ \t]+)?(?:\([^)]*\)|[A-Za-z0-9_]+)[ \t]*=>"""),
        // Python
        Regex("""(?m)^[ \t]*(?:async[ \t]+)?(?:def|class)[ \t]+([A-Za-z0-9_]+)"""),
        // Rust / Go
        Regex("""(?m)^[ \t]*(?:pub[ \t]+)?(?:fn|struct|enum|trait|impl)[ \t]+([A-Za-z0-9_]+)"""),
        Regex("""(?m)^[ \t]*func[ \t]+(?:\([^)]+\)[ \t]+)?([A-Za-z0-9_]+)""")
    )

    fun chunkFile(filePath: String, text: String): List<RawCodeChunk> {
        if (text.isBlank()) return emptyList()

        val lines = text.lines()
        if (lines.size <= 30) {
            val fileName = File(filePath).name
            return listOf(
                RawCodeChunk(
                    filePath = filePath,
                    symbolName = fileName,
                    symbolType = "FILE",
                    content = text.trim(),
                    startLine = 1,
                    endLine = lines.size
                )
            )
        }

        val chunks = mutableListOf<RawCodeChunk>()
        var currentStart = 0
        var currentSymbolName = File(filePath).name
        var currentSymbolType = "FILE"

        for (i in lines.indices) {
            val line = lines[i]
            for (pattern in symbolPatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    // If we have an accumulated block, save it
                    if (i > currentStart + 4) {
                        val chunkContent = lines.subList(currentStart, i).joinToString("\n")
                        if (chunkContent.isNotBlank()) {
                            chunks.add(
                                RawCodeChunk(
                                    filePath = filePath,
                                    symbolName = currentSymbolName,
                                    symbolType = currentSymbolType,
                                    content = chunkContent.trim(),
                                    startLine = currentStart + 1,
                                    endLine = i
                                )
                            )
                        }
                    }
                    currentStart = i
                    currentSymbolName = match.groupValues.getOrNull(1) ?: File(filePath).name
                    currentSymbolType = when {
                        line.contains("class") -> "CLASS"
                        line.contains("interface") -> "INTERFACE"
                        line.contains("fun") || line.contains("def") || line.contains("function") || line.contains("fn") -> "FUNCTION"
                        else -> "SYMBOL"
                    }
                    break
                }
            }
        }

        // Add tail chunk
        if (currentStart < lines.size) {
            val tailContent = lines.subList(currentStart, lines.size).joinToString("\n")
            if (tailContent.isNotBlank()) {
                chunks.add(
                    RawCodeChunk(
                        filePath = filePath,
                        symbolName = currentSymbolName,
                        symbolType = currentSymbolType,
                        content = tailContent.trim(),
                        startLine = currentStart + 1,
                        endLine = lines.size
                    )
                )
            }
        }

        return chunks
    }
}
