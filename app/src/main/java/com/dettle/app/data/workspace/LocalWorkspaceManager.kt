package com.dettle.app.data.workspace

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Surgical workspace manager providing Claude Code and Codex primitives:
 * - Surgical FileEdit with strict string uniqueness & mtime state tracking
 * - FileRead with line offset/limit & 1-indexed line numbering
 * - Fast Grep with files_with_matches (0 token bloat), content with context, and count
 * - Glob pattern matching
 * - Bash execution with head/tail truncation and persistent disk logs
 */
@Singleton
class LocalWorkspaceManager(
    val rootDir: File,
    val fileStateTracker: FileStateTracker
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        fileStateTracker: FileStateTracker
    ) : this(
        File(context.getExternalFilesDir(null), "workspace"),
        fileStateTracker
    )

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    val logsDir: File = File(rootDir, ".logs").apply {
        if (!exists()) mkdirs()
    }

    fun resolve(path: String): File {
        val file = File(rootDir, path).canonicalFile
        if (!file.path.startsWith(rootDir.canonicalPath)) {
            throw SecurityException("Path traversal attempt: $path")
        }
        return file
    }

    /**
     * Reads a file, records its mtime in FileStateTracker, and returns
     * numbered lines between offset (1-indexed) and offset + limit - 1.
     */
    fun readFile(path: String, offset: Int = 1, limit: Int = 2000): String {
        val file = resolve(path)
        if (!file.exists()) throw Exception("File not found: $path")
        if (!file.isFile) throw Exception("Path is a directory, not a file: $path")

        fileStateTracker.recordRead(path, file.lastModified())

        val lines = file.readLines()
        if (lines.isEmpty()) return "[File $path is empty (0 lines)]"

        val startLine = offset.coerceAtLeast(1)
        val endLine = (startLine + limit - 1).coerceAtMost(lines.size)

        if (startLine > lines.size) {
            return "File $path has ${lines.size} lines. Offset $offset is beyond end of file."
        }

        val sb = StringBuilder()
        sb.appendLine("File: $path (${lines.size} total lines, showing $startLine to $endLine):")
        for (i in startLine..endLine) {
            val lineContent = lines[i - 1]
            sb.appendLine(String.format("%4d | %s", i, lineContent))
        }
        return sb.toString().trimEnd()
    }

    /** Simple raw read without line numbers (for internal use by git/deploy/backups) */
    fun readRawFile(path: String): String {
        val file = resolve(path)
        if (!file.exists()) throw Exception("File not found: $path")
        fileStateTracker.recordRead(path, file.lastModified())
        return file.readText()
    }

    /**
     * Surgical FileEdit contract (Claude Code style):
     * Replaces exact oldString with newString.
     * Enforces uniqueness and freshness checks.
     */
    fun editFile(path: String, oldString: String, newString: String, replaceAll: Boolean = false): String {
        val file = resolve(path)
        if (!file.exists()) throw Exception("File not found: $path. Cannot edit non-existent file.")
        if (!file.isFile) throw Exception("Path is a directory: $path")

        val currentMtime = file.lastModified()
        if (fileStateTracker.isStale(path, currentMtime)) {
            throw IllegalStateException("File $path was modified since you last read it. Please read the file first to inspect current contents before editing.")
        }

        val content = file.readText()
        if (oldString.isEmpty()) {
            throw IllegalArgumentException("old_string cannot be empty.")
        }

        // Count occurrences
        var count = 0
        var idx = 0
        while (true) {
            val found = content.indexOf(oldString, idx)
            if (found == -1) break
            count++
            idx = found + oldString.length
        }

        if (count == 0) {
            throw IllegalArgumentException("old_string was not found in $path. Use file_read to check exact characters and indentation.")
        }

        if (count > 1 && !replaceAll) {
            throw IllegalArgumentException("old_string is not unique in $path (found $count matches). Please include additional lines of surrounding context to disambiguate the target location, or set replace_all=true.")
        }

        val updatedContent = if (replaceAll) {
            content.replace(oldString, newString)
        } else {
            val firstIdx = content.indexOf(oldString)
            content.substring(0, firstIdx) + newString + content.substring(firstIdx + oldString.length)
        }

        val oldLines = oldString.lines().size
        val newLines = newString.lines().size
        file.writeText(updatedContent)
        fileStateTracker.recordModified(path, file.lastModified())

        return "Successfully edited $path (replaced $count occurrence${if (count > 1) "s" else ""}, -$oldLines lines, +$newLines lines)."
    }

    /**
     * Create brand new file or completely overwrite (for non-surgical writes)
     */
    fun writeFile(path: String, content: String) {
        val file = resolve(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
        fileStateTracker.recordModified(path, file.lastModified())
    }

    /**
     * Fast regex search across files (Claude Code Grep style)
     * Modes:
     * - "files_with_matches": returns matching relative paths (0 token bloat)
     * - "content": returns file path, line numbers, and matching lines with -C context
     * - "count": returns total matches
     */
    fun grep(
        pattern: String,
        subPath: String = "",
        mode: String = "content",
        isRegex: Boolean = true,
        caseSensitive: Boolean = true,
        contextLines: Int = 2
    ): String {
        val target = resolve(subPath)
        val files = if (target.isFile) {
            listOf(target)
        } else {
            target.walkTopDown().maxDepth(8).filter { it.isFile && !it.path.contains("/.logs/") && !it.path.contains("/.git/") }.toList()
        }

        val regex = try {
            val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            if (isRegex) Regex(pattern, options) else Regex(Regex.escape(pattern), options)
        } catch (e: Exception) {
            return "Invalid regex pattern: ${e.message}"
        }

        return when (mode.lowercase()) {
            "files_with_matches" -> {
                val matches = files.filter { file ->
                    runCatching { regex.containsMatchIn(file.readText()) }.getOrDefault(false)
                }.map { it.relativeTo(rootDir).path.replace("\\", "/") }

                if (matches.isEmpty()) "No matching files found for: $pattern"
                else matches.joinToString("\n")
            }
            "count" -> {
                var total = 0
                val fileCounts = mutableListOf<String>()
                files.forEach { file ->
                    val lines = runCatching { file.readLines() }.getOrNull() ?: return@forEach
                    var count = 0
                    lines.forEach { line ->
                        if (regex.containsMatchIn(line)) count++
                    }
                    if (count > 0) {
                        total += count
                        val relPath = file.relativeTo(rootDir).path.replace("\\", "/")
                        fileCounts.add("$relPath: $count")
                    }
                }
                if (total == 0) "No matches found for: $pattern"
                else "Found $total match(es) across ${fileCounts.size} file(s):\n" + fileCounts.joinToString("\n")
            }
            else -> { // "content"
                val results = mutableListOf<String>()
                files.forEach { file ->
                    val lines = runCatching { file.readLines() }.getOrNull() ?: return@forEach
                    val matchingIndices = lines.indices.filter { regex.containsMatchIn(lines[it]) }
                    if (matchingIndices.isNotEmpty()) {
                        val relPath = file.relativeTo(rootDir).path.replace("\\", "/")
                        results.add("=== $relPath ===")
                        val printedIndices = mutableSetOf<Int>()
                        matchingIndices.forEach { matchIdx ->
                            val start = (matchIdx - contextLines).coerceAtLeast(0)
                            val end = (matchIdx + contextLines).coerceAtMost(lines.size - 1)
                            for (i in start..end) {
                                if (printedIndices.add(i)) {
                                    val prefix = if (i == matchIdx) ">" else " "
                                    results.add(String.format("%s%4d | %s", prefix, i + 1, lines[i]))
                                }
                            }
                        }
                        results.add("")
                    }
                }
                if (results.isEmpty()) "No matches found for: $pattern"
                else results.joinToString("\n").trimEnd()
            }
        }
    }

    /**
     * Glob pattern matching across workspace files
     */
    fun glob(pattern: String, subPath: String = ""): List<String> {
        val target = resolve(subPath)
        val files = if (target.isFile) {
            listOf(target)
        } else {
            target.walkTopDown().maxDepth(8).filter { it.isFile && !it.path.contains("/.logs/") && !it.path.contains("/.git/") }.toList()
        }

        val regexPattern = globToRegex(pattern)
        val regex = Regex(regexPattern)

        return files.map { it.relativeTo(rootDir).path.replace("\\", "/") }
            .filter { regex.matches(it) || regex.matches(it.substringAfterLast('/')) }
            .sorted()
    }

    private fun globToRegex(glob: String): String {
        val sb = StringBuilder("^")
        var i = 0
        while (i < glob.length) {
            when (val c = glob[i]) {
                '*' -> {
                    if (i + 1 < glob.length && glob[i + 1] == '*') {
                        sb.append(".*")
                        i++
                    } else {
                        sb.append("[^/]*")
                    }
                }
                '?' -> sb.append("[^/]")
                '.', '(', ')', '+', '|', '^', '$', '@', '%', '{', '}', '[', ']' -> {
                    sb.append('\\').append(c)
                }
                else -> sb.append(c)
            }
            i++
        }
        sb.append("$")
        return sb.toString()
    }

    /**
     * Local Bash execution with deterministic output truncation and disk persistence
     */
    fun executeBash(command: String, timeoutSeconds: Long = 60): String {
        val timestamp = System.currentTimeMillis()
        val logFile = File(logsDir, "bash-$timestamp.log")

        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .directory(rootDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                logFile.writeText(output + "\n[COMMAND TIMED OUT AFTER $timeoutSeconds SECONDS]")
                return "[Exit: Timeout] Command exceeded ${timeoutSeconds}s limit.\nOutput before timeout:\n${output.takeLast(1000)}"
            }

            val exitCode = process.exitValue()
            logFile.writeText(output)

            val lines = output.lines()
            val totalLines = lines.size

            if (totalLines <= 40) {
                "[Exit Code: $exitCode]\n$output".trimEnd()
            } else {
                val head = lines.take(10).joinToString("\n")
                val tail = lines.takeLast(25).joinToString("\n")
                val relativeLogPath = logFile.relativeTo(rootDir).path.replace("\\", "/")
                """
                |[Exit Code: $exitCode]
                |Output truncated ($totalLines lines). Showing first 10 and last 25 lines:
                |--- Head ---
                |$head
                |--- Tail ---
                |$tail
                |[Full log ($totalLines lines) saved to: $relativeLogPath]
                """.trimMargin()
            }
        } catch (e: Exception) {
            logFile.writeText("Execution failure: ${e.message}\n${e.stackTraceToString()}")
            "[Execution Error]: ${e.message}"
        }
    }

    fun listFiles(path: String = ""): List<String> {
        val dir = resolve(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.walkTopDown().maxDepth(5).filter { it.isFile && !it.path.contains("/.logs/") }.map {
            it.relativeTo(rootDir).path.replace("\\", "/")
        }.toList()
    }

    fun deleteFile(path: String): Boolean {
        val file = resolve(path)
        return file.delete()
    }

    /** Reads all files in the workspace directory into a Map of relativePath -> content */
    fun readAllFiles(subDir: String = ""): Map<String, String> {
        val files = listFiles(subDir)
        return files.associateWith { readRawFile(it) }
    }
}
