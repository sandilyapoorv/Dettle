package com.dettle.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TraceStatus {
    SUCCESS,
    RUNNING,
    WARNING,
    ERROR
}

@Serializable
data class TraceStep(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val offsetMs: Long = 0L,
    val icon: String = "⏱️",
    val title: String,
    val description: String? = null,
    val status: TraceStatus = TraceStatus.SUCCESS
)

@Serializable
data class ExecutionTrace(
    val startTimeMs: Long = System.currentTimeMillis(),
    val endTimeMs: Long? = null,
    val totalDurationMs: Long? = null,
    val modelUsed: String? = null,
    val steps: List<TraceStep> = emptyList()
)
