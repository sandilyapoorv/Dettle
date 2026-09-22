package com.dettle.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A Cloudflare Pages/Workers deployment record. */
@Entity(tableName = "deployments")
data class DeploymentEntity(
    @PrimaryKey val id: String,
    val projectName: String,
    val type: String,
    val url: String,
    val productionUrl: String = "",
    val status: String = "active",
    val fileCount: Int = 0,
    val commitMessage: String = "",
    val repoKey: String = "",
    val commitSha: String = "",
    val branch: String = "main",
    val buildLog: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
