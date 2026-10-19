package com.dettle.app.data.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalWorkspaceManagerSurgicalTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var workspaceDir: File
    private lateinit var tracker: FileStateTracker
    private lateinit var manager: LocalWorkspaceManager

    @Before
    fun setup() {
        workspaceDir = tempFolder.newFolder("workspace")
        tracker = FileStateTracker()
        manager = LocalWorkspaceManager(workspaceDir, tracker)
    }

    @Test
    fun testReadFileWithLineNumbersAndOffsetLimit() {
        manager.writeFile("test.kt", "line 1\nline 2\nline 3\nline 4\nline 5")

        val output = manager.readFile("test.kt", offset = 2, limit = 2)
        assertTrue(output.contains("   2 | line 2"))
        assertTrue(output.contains("   3 | line 3"))
        assertFalse(output.contains("line 1"))
        assertFalse(output.contains("line 4"))

        assertTrue(tracker.hasBeenRead("test.kt"))
    }

    @Test
    fun testEditFileSurgicalSingleReplacementSuccess() {
        manager.writeFile("main.kt", "fun main() {\n    val x = 1\n    println(x)\n}")
        // Record read
        manager.readFile("main.kt")

        val editResult = manager.editFile(
            path = "main.kt",
            oldString = "val x = 1",
            newString = "val x = 42"
        )

        assertTrue(editResult.contains("Successfully edited main.kt"))
        val updated = manager.readRawFile("main.kt")
        assertEquals("fun main() {\n    val x = 42\n    println(x)\n}", updated)
    }

    @Test
    fun testEditFileThrowsWhenOldStringNotFound() {
        manager.writeFile("app.kt", "class App")
        manager.readFile("app.kt")

        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.editFile(
                path = "app.kt",
                oldString = "class Missing",
                newString = "class New"
            )
        }
        assertTrue(ex.message!!.contains("old_string was not found in app.kt"))
    }

    @Test
    fun testEditFileThrowsWhenOldStringNotUniqueWithoutReplaceAll() {
        manager.writeFile("config.kt", "val timeout = 10\nval retry = 10\nval max = 10")
        manager.readFile("config.kt")

        val ex = assertThrows(IllegalArgumentException::class.java) {
            manager.editFile(
                path = "config.kt",
                oldString = "= 10",
                newString = "= 20",
                replaceAll = false
            )
        }
        assertTrue(ex.message!!.contains("old_string is not unique in config.kt (found 3 matches)"))
    }

    @Test
    fun testEditFileSucceedsWithReplaceAllWhenMultipleMatches() {
        manager.writeFile("config.kt", "val timeout = 10\nval retry = 10")
        manager.readFile("config.kt")

        val result = manager.editFile(
            path = "config.kt",
            oldString = "= 10",
            newString = "= 20",
            replaceAll = true
        )

        assertTrue(result.contains("replaced 2 occurrences"))
        val updated = manager.readRawFile("config.kt")
        assertEquals("val timeout = 20\nval retry = 20", updated)
    }

    @Test
    fun testEditFileDetectsStaleFileModification() {
        val file = File(workspaceDir, "stale.kt")
        manager.writeFile("stale.kt", "initial content")
        manager.readFile("stale.kt")

        // External process alters file
        Thread.sleep(50)
        file.writeText("externally altered content")
        file.setLastModified(System.currentTimeMillis() + 1000)

        val ex = assertThrows(IllegalStateException::class.java) {
            manager.editFile(
                path = "stale.kt",
                oldString = "initial",
                newString = "modified"
            )
        }
        assertTrue(ex.message!!.contains("was modified since you last read it"))
    }

    @Test
    fun testGrepModes() {
        manager.writeFile("src/A.kt", "fun startJob() {\n    println(\"hello\")\n}")
        manager.writeFile("src/B.kt", "fun stopJob() {\n    println(\"world\")\n}")
        manager.writeFile("docs/readme.txt", "No code here")

        // 1. files_with_matches
        val fileMatches = manager.grep(pattern = "Job\\(\\)", mode = "files_with_matches")
        assertTrue(fileMatches.contains("src/A.kt"))
        assertTrue(fileMatches.contains("src/B.kt"))
        assertFalse(fileMatches.contains("docs/readme.txt"))

        // 2. count
        val countOutput = manager.grep(pattern = "println", mode = "count")
        assertTrue(countOutput.contains("Found 2 match(es)"))

        // 3. content
        val contentOutput = manager.grep(pattern = "stopJob", mode = "content")
        assertTrue(contentOutput.contains("=== src/B.kt ==="))
        assertTrue(contentOutput.contains("fun stopJob() {"))
    }

    @Test
    fun testGlobPatterns() {
        manager.writeFile("app/src/main/A.kt", "A")
        manager.writeFile("app/src/test/BTest.kt", "BTest")
        manager.writeFile("package.json", "{}")

        val ktFiles = manager.glob("**/*.kt")
        assertEquals(2, ktFiles.size)
        assertTrue(ktFiles.contains("app/src/main/A.kt"))
        assertTrue(ktFiles.contains("app/src/test/BTest.kt"))

        val jsonFiles = manager.glob("*.json")
        assertEquals(1, jsonFiles.size)
        assertTrue(jsonFiles.contains("package.json"))
    }
}
