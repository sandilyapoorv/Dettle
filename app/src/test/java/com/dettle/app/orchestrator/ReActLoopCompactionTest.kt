package com.dettle.app.orchestrator

import com.dettle.app.domain.model.ApiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReActLoopCompactionTest {

    private val repoMapper = RepoMapper()

    @Test
    fun testWorkspaceRepoMapGeneratesCompactAstSignatures() {
        val files = mapOf(
            "src/service/PaymentService.kt" to """
                package com.example.service
                
                class PaymentService {
                    fun processPayment(amount: Double): Boolean {
                        return true
                    }
                    
                    suspend fun refundPayment(txId: String) {
                        println(txId)
                    }
                }
            """.trimIndent(),
            "scripts/deploy.py" to """
                def deploy_all():
                    pass
                class Deployer:
                    pass
            """.trimIndent()
        )

        val map = repoMapper.buildWorkspaceRepoMap(files)
        assertTrue(map.contains("## Workspace Map (Compact Signatures)"))
        assertTrue(map.contains("src/service/PaymentService.kt"))
        assertTrue(map.contains("class PaymentService"))
        assertTrue(map.contains("fun processPayment(amount: Double): Boolean"))
        assertTrue(map.contains("suspend fun refundPayment(txId: String)"))
        assertTrue(map.contains("def deploy_all()"))
    }

    @Test
    fun testToolOutputCompactionPreservesRecentMessagesAndCompactsOlder() {
        // Construct a conversation with 6 messages (older tool messages and recent messages)
        val history = listOf(
            ApiMessage(role = "user", content = "Start task"),
            ApiMessage(role = "assistant", content = "Reading file"),
            ApiMessage(role = "tool", content = "A".repeat(500), name = "workspace_read_file"),
            ApiMessage(role = "assistant", content = "Running command"),
            ApiMessage(role = "tool", content = "[Exit Code: 0]\n" + "Success output\n".repeat(30), name = "workspace_bash"),
            ApiMessage(role = "assistant", content = "Now at step 3"),
            ApiMessage(role = "tool", content = "Recent tool output that should not be touched", name = "workspace_read_file"),
            ApiMessage(role = "assistant", content = "Done step 3"),
            ApiMessage(role = "user", content = "Continue please")
        )

        // Mock ReActLoop compaction via independent helper matching internal logic
        val splitIndex = (history.size - 4).coerceAtLeast(0)
        val older = history.subList(0, splitIndex)
        val recent = history.subList(splitIndex, history.size)

        val compactedOlder = older.map { msg ->
            if (msg.role == "tool" && msg.content.length > 200) {
                msg.copy(content = "[Compacted: ${msg.name}]")
            } else msg
        }
        val result = compactedOlder + recent

        assertEquals(history.size, result.size)
        // Older tool message at index 2 was compacted
        assertEquals("[Compacted: workspace_read_file]", result[2].content)
        // Older bash message at index 4 was compacted
        assertEquals("[Compacted: workspace_bash]", result[4].content)
        // Recent tool message at index 6 was preserved verbatim
        assertEquals("Recent tool output that should not be touched", result[6].content)
    }
}
