package com.dettle.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A record of one agent task — what was requested, what happened, outcome. */
@Entity(tableName = "task_logs")
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userRequest: String,
    val taskType: String,           // CHAT, CODE_WRITE, DEPLOY, etc.
    val repoKey: String = "",
    val steps: List<String> = emptyList(),   // Tool calls made (serialized)
    val outcome: String = "",       // SUCCESS, FAILED, PARTIAL
    val summary: String = "",       // Human-readable summary of what was done
    val tokensUsed: Int = 0,
    val providerUsed: String = "",
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/** A Cloudflare Pages/Workers deployment record. */
@Entity(tableName = "deployments")
data class DeploymentEntity(
    @PrimaryKey val id: String,
    val projectName: String,
    val type: String,               // PAGES or WORKER
    val url: String,
    val productionUrl: String,
    val status: String = "active",
    val fileCount: Int = 0,
    val commitMessage: String = "",
    val repoKey: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
