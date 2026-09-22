package com.dettle.app.domain.model

data class TaskContext(
    val isCodeTask: Boolean = false,
    val isLongRunning: Boolean = false,
    val isOvernightRun: Boolean = false,
    val repoOwner: String? = null,
    val repoName: String? = null,
    val language: String = "kotlin",
    val taskType: TaskType = TaskType.CHAT
)

enum class TaskType { CHAT, CODE_WRITE, CODE_REVIEW, DEPLOY, RESEARCH }
