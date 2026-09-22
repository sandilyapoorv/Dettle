package com.dettle.app.orchestrator.config

import kotlinx.serialization.Serializable

/**
 * Typed representation of this project's agent configuration.
 *
 * This is the code equivalent of the old AGENTS.md — but enforced by [PolicyEngine]
 * rather than just hoped-for by humans reading a markdown file.
 *
 * Loaded from .agents/config.json via [AgentConfigLoader] at session start.
 * Falls back to [DettleAgentConfig.DEFAULT] if the remote file is unavailable.
 */
@Serializable
data class DettleAgentConfig(
    val project: String = "Dettle",
    val version: String = "1.0",
    val android: AndroidStackConfig = AndroidStackConfig(),
    val delivery: DeliveryPolicy = DeliveryPolicy(),
    val backend: BackendStackConfig = BackendStackConfig(),
    val security: SecurityPolicy = SecurityPolicy(),
    val freeTier: FreeTierBudget = FreeTierBudget()
) {
    companion object {
        /**
         * Hard-coded default that matches the project's current requirements.
         * Used when .agents/config.json cannot be fetched from GitHub.
         */
        val DEFAULT = DettleAgentConfig()
    }
}

// ─── Android ──────────────────────────────────────────────────────────────────

@Serializable
data class AndroidStackConfig(
    val minSdk: Int = 26,
    val targetSdk: Int = 35,
    val compileSdk: Int = 35,
    val agp: String = "8.5",
    val kotlin: String = "2.0",
    val jdk: String = "17",
    val compose: Boolean = true,
    val material3: Boolean = true,
    val dependencyInjection: String = "hilt",
    val buildSystem: String = "gradle-kotlin-dsl",
    val applicationId: String = "com.dettle.app",
    val versionCodeMinimum: Int = 1
)

// ─── Delivery ─────────────────────────────────────────────────────────────────

@Serializable
data class DeliveryPolicy(
    val requireCI: Boolean = true,
    val requireAPK: Boolean = true,
    val requireTests: Boolean = true,
    val requireLint: Boolean = true,
    val defaultBranch: String = "main",
    val branchPrefix: String = "dettle/",
    val requirePR: Boolean = true,
    val neverPushDirectlyToMain: Boolean = true,
    val requireProductionCriticBeforeCompletion: Boolean = true,
    val releaseWorkflow: String = "release.yml",
    val debugWorkflow: String = "build.yml"
)

// ─── Backend ──────────────────────────────────────────────────────────────────

@Serializable
data class BackendStackConfig(
    val auth: String = "firebase-auth",
    val authProviders: List<String> = listOf("email-password", "google-sign-in", "anonymous"),
    val database: String = "firestore",
    val realtimeSync: String = "firebase-rtdb",
    val media: String = "imagekit",
    val edgeFunctions: String = "cloudflare-workers",
    val staticHosting: String = "cloudflare-pages",
    val requireSecurityRules: Boolean = true,
    val neverClientOnlyAuthz: Boolean = true
)

// ─── Security ─────────────────────────────────────────────────────────────────

@Serializable
data class SecurityPolicy(
    /**
     * Regex patterns that must NEVER appear in committed code.
     * [PolicyEngine] checks these before any github_create_branch_pr call.
     */
    val forbiddenPatterns: List<String> = listOf(
        "ANDROID_KEYSTORE",
        "-----BEGIN.*PRIVATE KEY-----",
        "-----BEGIN.*CERTIFICATE-----",
        """password\s*[:=]\s*\S+""",
        """api_key\s*[:=]\s*\S+""",
        """apiKey\s*[:=]\s*\S+""",
        """secret\s*[:=]\s*\S+""",
        """access_token\s*[:=]\s*\S+"""
    ),
    val signingSecretsStorage: String = "github_actions_secrets",
    val githubActionsSecrets: List<String> = listOf(
        "ANDROID_KEYSTORE_BASE64",
        "ANDROID_KEYSTORE_PASSWORD",
        "ANDROID_KEY_ALIAS",
        "ANDROID_KEY_PASSWORD"
    ),
    val neverInDrive: Boolean = true,
    val neverInCode: Boolean = true,
    val neverInCommit: Boolean = true
) {
    /** Compiled regex patterns — computed once, cached here */
    val compiledForbiddenPatterns: List<Regex> by lazy {
        forbiddenPatterns.map { Regex(it, RegexOption.IGNORE_CASE) }
    }
}

// ─── Free Tier Budgets ────────────────────────────────────────────────────────

@Serializable
data class FreeTierBudget(
    val cloudflare: CloudflareQuota = CloudflareQuota(),
    val firebase: FirebaseQuota = FirebaseQuota(),
    val github: GitHubQuota = GitHubQuota(),
    val imagekit: ImageKitQuota = ImageKitQuota()
)

@Serializable
data class CloudflareQuota(
    val workersRequestsPerDay: Int = 100_000,
    val workerCpuMsPerInvocation: Int = 10,
    val pagesRequestsPerMonth: Int = -1   // -1 = effectively unlimited on free
)

@Serializable
data class FirebaseQuota(
    val firestoreReadsPerDay: Int = 50_000,
    val firestoreWritesPerDay: Int = 20_000,
    val firestoreDeletesPerDay: Int = 20_000,
    val realtimeDatabaseGbStored: Int = 1
)

@Serializable
data class GitHubQuota(
    val actionsMinutesPerMonth: Int = 2_000,
    val artifactStorageGb: Float = 0.5f
)

@Serializable
data class ImageKitQuota(
    val bandwidthGbPerMonth: Int = 20,
    val mediaStorageGb: Int = 20
)
