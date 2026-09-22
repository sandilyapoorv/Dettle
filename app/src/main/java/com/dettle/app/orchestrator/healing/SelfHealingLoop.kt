package com.dettle.app.orchestrator.healing

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.api.StreamChunk
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.ModelSets
import com.dettle.app.orchestrator.swarm.SwarmSubTask
import com.dettle.app.orchestrator.swarm.WorkerResult
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SelfHealingLoop"

/**
 * The Self-Healing Loop takes generated code and forces it to prove it works.
 * If static analysis (Critic) is the "theory", this is the "practice".
 * It runs tests, parses the crash trace, and loops until the build is green.
 */
@Singleton
class SelfHealingLoop @Inject constructor(
    private val keyPoolManager: KeyPoolManager,
    private val sandboxExecutor: SandboxExecutor,
    private val agentLogger: com.dettle.app.orchestrator.telemetry.AgentLogger
) {
    companion object {
        private const val MAX_HEALING_RETRIES = 3
    }

    /**
     * Takes a WorkerResult (from the Swarm) and verifies it dynamically.
     * Returns a new WorkerResult with either verified code, or the final broken state
     * if it exhausted all retries.
     */
    suspend fun enforceAndHeal(
        workerResult: WorkerResult,
        validationCommand: String
    ): WorkerResult {
        Log.d(TAG, "Starting Self-Healing Loop for ${workerResult.subTask.targetFile}")
        
        var currentCode = workerResult.generatedCode
        var attempt = 1

        while (attempt <= MAX_HEALING_RETRIES) {
            Log.d(TAG, "Validation Attempt $attempt/$MAX_HEALING_RETRIES")
            
            // Execute the code in the sandbox (e.g., run CI tests)
            val result = sandboxExecutor.executeValidation(
                command = validationCommand,
                targetFile = workerResult.subTask.targetFile,
                code = currentCode
            )

            if (result.isSuccess) {
                Log.d(TAG, "Validation passed! Exit code 0.")
                agentLogger.logHealingEvent(workerResult.subTask.targetFile, 0, "Validation Passed")
                return workerResult.copy(
                    generatedCode = currentCode,
                    criticFeedback = "Verified by Sandbox execution: PASS"
                )
            }

            Log.w(TAG, "Validation failed with exit code ${result.exitCode}. Initiating autonomous fix.")
            agentLogger.logHealingEvent(workerResult.subTask.targetFile, result.exitCode, result.stderr)
            
            // Feed the exact error trace back to a deep reasoning model
            currentCode = generateFix(
                task = workerResult.subTask,
                brokenCode = currentCode,
                errorTrace = result.stderr.ifBlank { result.stdout }
            )
            
            attempt++
        }

        Log.e(TAG, "Self-Healing exhausted $MAX_HEALING_RETRIES retries. Code remains broken.")
        return workerResult.copy(
            generatedCode = currentCode,
            criticFeedback = "FAILED DYNAMIC VALIDATION. Exceeded max retries."
        )
    }

    private suspend fun generateFix(
        task: SwarmSubTask,
        brokenCode: String,
        errorTrace: String
    ): String {
        val prompt = buildString {
            appendLine("## SELF-HEALING PROTOCOL INITIATED")
            appendLine("You are an expert debugger. Your code failed to compile or pass tests.")
            appendLine()
            appendLine("### Task Context")
            appendLine(task.description)
            appendLine()
            appendLine("### The Broken Code (${task.targetFile})")
            appendLine("```kotlin\n$brokenCode\n```")
            appendLine()
            appendLine("### The Compiler/Test Error Trace")
            appendLine("```log\n${errorTrace.takeLast(2000)}\n```")
            appendLine()
            appendLine("### Instructions")
            appendLine("1. Read the error trace carefully. The exact line and reason for failure is there.")
            appendLine("2. Do not apologize. Do not explain.")
            appendLine("3. Output the COMPLETE, FIXED CODE block. No markdown wrapper needed if it's the only output, but ensure it is 100% syntactically correct.")
        }

        var fixedCode = ""
        
        // Use a Deep Reasoning model for debugging (they are significantly better at parsing stack traces)
        keyPoolManager.chat(
            messages = listOf(ApiMessage("user", prompt)),
            systemPrompt = "You are an autonomous debugging agent. Output only the fixed code.",
            preferModel = ModelSets.DEEP_REASONING.first()
        ).collect { chunk ->
            if (chunk is StreamChunk.Token) fixedCode += chunk.text
        }

        // Clean up markdown block if the model included it
        val cleaned = fixedCode.substringAfter("```kotlin").substringAfter("```").substringBeforeLast("```").trim()
        return if (cleaned.isNotEmpty()) cleaned else fixedCode.trim()
    }
}
