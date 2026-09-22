package com.dettle.app.orchestrator.healing

/**
 * Represents the result of running a command (e.g., tests, linters, compilers).
 */
data class ExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
) {
    val isSuccess get() = exitCode == 0
}

/**
 * Interface for executing dynamic analysis (running code).
 * Since Android cannot run `./gradlew` natively, implementations of this
 * interface will typically bridge to a Remote SSH runner, a GitHub Actions CI hook,
 * or a serverless container sandbox (like Docker via an API).
 */
interface SandboxExecutor {
    /**
     * Executes a validation command against the provided code.
     * 
     * @param command The shell command to run (e.g., "./gradlew test")
     * @param targetFile The file path being tested
     * @param code The actual code to inject into the sandbox before running
     */
    suspend fun executeValidation(
        command: String,
        targetFile: String,
        code: String
    ): ExecutionResult
}
