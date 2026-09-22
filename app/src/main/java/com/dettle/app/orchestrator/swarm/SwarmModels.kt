package com.dettle.app.orchestrator.swarm

import kotlinx.serialization.Serializable

/**
 * Data structures for passing context between agents in the Swarm.
 */

@Serializable
data class SwarmSubTask(
    val id: String,
    val description: String,
    val targetFile: String,
    val requiredDependencies: List<String> = emptyList()
)

@Serializable
data class SwarmArchitecturePlan(
    val overallArchitecture: String,
    val subTasks: List<SwarmSubTask>
)

data class WorkerResult(
    val subTask: SwarmSubTask,
    val generatedCode: String,
    var status: ResultStatus = ResultStatus.PENDING,
    var criticFeedback: String? = null
)

enum class ResultStatus {
    PENDING,
    APPROVED,
    REJECTED
}
