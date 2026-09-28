package com.dettle.app.orchestrator.mode

import android.util.Log
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.orchestrator.memory.EmbeddingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ModeRouter"

/**
 * On-Device Typed Decision Router.
 *
 * Inspired by Laya's typed decision architecture:
 * 1. Tier 1: Fast-path regex & slash commands (<0.1ms)
 * 2. Tier 2: On-device semantic decision engine via MediaPipe / TFLite Universal Sentence Encoder (~10-15ms)
 * 3. Tier 3: Zero-lag graceful fallback (Never hangs or stalls user replies)
 */
@Singleton
class ModeRouter @Inject constructor(
    private val keyPoolManager: KeyPoolManager,
    private val embeddingEngine: EmbeddingEngine
) {
    data class ClassificationResult(
        val modeId: ModeId,
        val isAmbiguous: Boolean = false,
        val confidence: String = "HIGH"   // HIGH / MEDIUM / LOW
    )

    // Fast-path patterns for common conversational greetings and confirmations (0ms)
    private val casualGreetingsPattern = Regex(
        """^(hi|hello|hey|heya|howdy|sup|yo|good\s+morning|good\s+evening|good\s+afternoon|thanks|thank\s+you|ok|okay|cool|nice|test|ping)[!.,? ]*$""",
        RegexOption.IGNORE_CASE
    )

    // Mode prototypes used by the on-device semantic decision engine
    private val modePrototypes = mapOf(
        ModeId.CHAT to "casual question, general discussion, greeting, chit chat, how are you, personal question, tell me a joke, conversational",
        ModeId.CODE to "write code, fix bug, implement feature, refactor function, kotlin, java, python, javascript, create pull request, git commit, programming, solve error, compile",
        ModeId.PLAN to "create implementation plan, architecture design, technical spec, break down tasks, design document, system roadmap, plan next steps",
        ModeId.RESEARCH to "investigate codebase, explain library, how does this work, deep dive architecture, technical explanation, search codebase, explore documentation",
        ModeId.GOAL to "autonomous overnight goal, long multi-step task, complete without interruption, run background agent, execute checklist, automated goal",
        ModeId.WEB to "search the web, find online information, latest documentation, current news, lookup url, google search, internet lookup",
        ModeId.REVIEW to "review code, review pull request, critique diff, code audit, spot bugs, security analysis, verify patch, pull request review",
        ModeId.DEPLOY to "build release apk, trigger github actions workflow, deploy cloudflare worker, publish to pages, ci cd pipeline, assemble release",
        ModeId.SWARM to "launch swarm, parallel agents, teamwork, many models working together, consensus, collaborative problem solving, brainstorm with multiple agents"
    )

    private val prototypeVectors = mutableMapOf<ModeId, FloatArray>()
    private val initMutex = Mutex()
    private var isPrototypesInitialized = false

    private suspend fun ensurePrototypesInitialized() {
        if (isPrototypesInitialized) return
        initMutex.withLock {
            if (isPrototypesInitialized) return
            embeddingEngine.initialize()
            for ((mode, text) in modePrototypes) {
                val vec = embeddingEngine.generateEmbedding(text)
                if (vec != null) {
                    prototypeVectors[mode] = vec
                }
            }
            if (prototypeVectors.isNotEmpty()) {
                isPrototypesInitialized = true
                Log.d(TAG, "Initialized ${prototypeVectors.size} on-device mode prototype vectors")
            }
        }
    }

    suspend fun classify(
        userMessage: String,
        history: List<ApiMessage> = emptyList()
    ): ClassificationResult = withContext(Dispatchers.Default) {
        val trimmed = userMessage.trim()
        val startMs = System.currentTimeMillis()

        // ── Tier 1: Fast-Path Slash Commands & Heuristics (<0.1ms) ──────────
        when {
            trimmed.startsWith("/code") -> {
                Log.d(TAG, "⚡ Fast-path slash command: CODE")
                return@withContext ClassificationResult(ModeId.CODE, confidence = "HIGH")
            }
            trimmed.startsWith("/plan") -> {
                Log.d(TAG, "⚡ Fast-path slash command: PLAN")
                return@withContext ClassificationResult(ModeId.PLAN, confidence = "HIGH")
            }
            trimmed.startsWith("/research") -> {
                Log.d(TAG, "⚡ Fast-path slash command: RESEARCH")
                return@withContext ClassificationResult(ModeId.RESEARCH, confidence = "HIGH")
            }
            trimmed.startsWith("/goal") -> {
                Log.d(TAG, "⚡ Fast-path slash command: GOAL")
                return@withContext ClassificationResult(ModeId.GOAL, confidence = "HIGH")
            }
            trimmed.startsWith("/web") -> {
                Log.d(TAG, "⚡ Fast-path slash command: WEB")
                return@withContext ClassificationResult(ModeId.WEB, confidence = "HIGH")
            }
            trimmed.startsWith("/review") -> {
                Log.d(TAG, "⚡ Fast-path slash command: REVIEW")
                return@withContext ClassificationResult(ModeId.REVIEW, confidence = "HIGH")
            }
            trimmed.startsWith("/deploy") -> {
                Log.d(TAG, "⚡ Fast-path slash command: DEPLOY")
                return@withContext ClassificationResult(ModeId.DEPLOY, confidence = "HIGH")
            }
            trimmed.startsWith("/swarm") -> {
                Log.d(TAG, "⚡ Fast-path slash command: SWARM")
                return@withContext ClassificationResult(ModeId.SWARM, confidence = "HIGH")
            }
            trimmed.startsWith("/chat") -> {
                Log.d(TAG, "⚡ Fast-path slash command: CHAT")
                return@withContext ClassificationResult(ModeId.CHAT, confidence = "HIGH")
            }
            casualGreetingsPattern.matches(trimmed) -> {
                Log.d(TAG, "⚡ Fast-path casual greeting matched: CHAT in ${System.currentTimeMillis() - startMs}ms")
                return@withContext ClassificationResult(ModeId.CHAT, isAmbiguous = false, confidence = "HIGH")
            }
        }

        // ── Tier 2: On-Device Semantic Decision Engine (~10-15ms) ───────────
        try {
            ensurePrototypesInitialized()
            val promptVector = embeddingEngine.generateEmbedding(trimmed)
            if (promptVector != null && prototypeVectors.isNotEmpty()) {
                var bestMode = ModeId.CHAT
                var highestSimilarity = -1f
                var secondSimilarity = -1f

                for ((mode, protoVec) in prototypeVectors) {
                    val sim = embeddingEngine.cosineSimilarity(promptVector, protoVec)
                    if (sim > highestSimilarity) {
                        secondSimilarity = highestSimilarity
                        highestSimilarity = sim
                        bestMode = mode
                    } else if (sim > secondSimilarity) {
                        secondSimilarity = sim
                    }
                }

                val duration = System.currentTimeMillis() - startMs
                val confidence = when {
                    highestSimilarity >= 0.45f && (highestSimilarity - secondSimilarity) >= 0.04f -> "HIGH"
                    highestSimilarity >= 0.35f -> "MEDIUM"
                    else -> "LOW"
                }

                val isAmbiguous = confidence == "LOW" || (highestSimilarity < 0.38f)
                val finalMode = if (isAmbiguous) ModeId.CHAT else bestMode

                Log.d(
                    TAG,
                    "🧠 On-Device Semantic decision: $finalMode (sim=${"%.3f".format(highestSimilarity)}, 2nd=${"%.3f".format(secondSimilarity)}, conf=$confidence) in ${duration}ms"
                )

                return@withContext ClassificationResult(finalMode, isAmbiguous = isAmbiguous, confidence = confidence)
            }
        } catch (e: Exception) {
            Log.w(TAG, "On-device semantic decision error: ${e.message}", e)
        }

        // ── Tier 3: Zero-Lag Fallback Heuristics (<1ms) ───────────────────────
        val lower = trimmed.lowercase()
        val fallbackMode = when {
            lower.contains("fix ") || lower.contains("bug") || lower.contains("implement") ||
                    lower.contains("code") || lower.contains("function") || lower.contains("error") ||
                    lower.contains("compile") || lower.contains("pull request") || lower.contains("commit") -> ModeId.CODE

            lower.contains("plan ") || lower.contains("architecture") || lower.contains("roadmap") -> ModeId.PLAN
            lower.contains("review") || lower.contains("audit") -> ModeId.REVIEW
            lower.contains("deploy") || lower.contains("build apk") || lower.contains("release") -> ModeId.DEPLOY
            lower.contains("search ") || lower.contains("browse") -> ModeId.WEB
            else -> ModeId.CHAT
        }

        Log.d(TAG, "🔄 Fast-path fallback decision: $fallbackMode in ${System.currentTimeMillis() - startMs}ms")
        return@withContext ClassificationResult(fallbackMode, isAmbiguous = (fallbackMode == ModeId.CHAT), confidence = "MEDIUM")
    }
}
