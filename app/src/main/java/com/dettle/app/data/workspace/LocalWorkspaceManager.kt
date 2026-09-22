package com.dettle.app.data.workspace

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalWorkspaceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val rootDir: File = File(context.getExternalFilesDir(null), "workspace").apply {
        if (!exists()) mkdirs()
    }

    private fun resolve(path: String): File {
        val file = File(rootDir, path).canonicalFile
        if (!file.path.startsWith(rootDir.canonicalPath)) {
            throw SecurityException("Path traversal attempt: $path")
        }
        return file
    }

    fun writeFile(path: String, content: String) {
        val file = resolve(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun readFile(path: String): String {
        val file = resolve(path)
        if (!file.exists()) throw Exception("File not found: $path")
        return file.readText()
    }

    fun listFiles(path: String = ""): List<String> {
        val dir = resolve(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.walkTopDown().maxDepth(5).filter { it.isFile }.map {
            it.relativeTo(rootDir).path.replace("\\", "/")
        }.toList()
    }

    fun deleteFile(path: String): Boolean {
        val file = resolve(path)
        return file.delete()
    }
}
