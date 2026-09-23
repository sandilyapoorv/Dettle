package com.dettle.app.orchestrator.brain

import android.util.Log
import com.dettle.app.data.db.dao.KnowledgeGraphDao
import com.dettle.app.data.db.dao.MemoryDao
import com.dettle.app.domain.model.ApiMessage
import com.dettle.app.domain.model.TaskContext
import com.dettle.app.orchestrator.LoopEvent
import com.dettle.app.orchestrator.MemoryInjector
import com.dettle.app.orchestrator.learning.LearningEngine
import com.dettle.app.orchestrator.policy.SalienceEvaluation
import com.dettle.app.orchestrator.policy.SalienceEvaluator
import com.dettle.app.orchestrator.policy.SalienceType
import com.dettle.app.orchestrator.reflex.ProceduralReflexEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CognitiveBrain"

/**
 * Live cognitive state of the agent's brain.
 */
sealed class CognitiveState(val label: String) {
    object Idle : CognitiveState("Brain Idle")
    object ReflexActive : CognitiveState("Motor Cortex Reflex")
    data class ThreatAssessment(val score: Int) : CognitiveState("Amygdala Salience Check ($score/10)")
    data class MemoryRecall(val memoryCount: Int) : CognitiveState("Hippocampus Recall ($memoryCount memories)")
    data class PrefrontalPlanning(val innerThought: String) : CognitiveState("Prefrontal Intent Alignment")
    object UnleashedExecution : CognitiveState("Unleashed Raw Execution Engine")
    object Consolidating : CognitiveState("Neuroplastic Memory Consolidation")
}

data class CognitiveContext(
    val innerThought: String,
    val recalledMemories: String,
    val salience: SalienceEvaluation,
    val isUncensored: Boolean
)

/**
 * The unified Cognitive Brain of Dettle.
 *
 * Coordinates:
 * 1. Motor Cortex: Procedural reflex intercepts without token usage.
 * 2. Amygdala: Salience and threat assessment (interrupts on critical signals).
 * 3. Hippocampus: Episodic vector memory recall and knowledge graph association.
 * 4. Prefrontal Cortex: Strategic intent alignment and inner cognitive reasoning.
 * 5. Neuroplastic Learning: Autonomous post-interaction memory consolidation.
 */
@Singleton
class CognitiveBrain @Inject constructor(
    private val reflexEngine: ProceduralReflexEngine,
    private val salienceEvaluator: SalienceEvaluator,
    private val memoryInjector: MemoryInjector,
    private val memoryDao: MemoryDao,
    private val knowledgeGraphDao: KnowledgeGraphDao,
    private val learningEngine: LearningEngine
) {
    private val _cognitiveState = MutableStateFlow<CognitiveState>(CognitiveState.Idle)
    val cognitiveState: StateFlow<CognitiveState> = _cognitiveState.asStateFlow()

    private val brainScope = CoroutineScope(Dispatchers.IO)

    init {
        // Ensure background learning queue is actively drained by the brain
        learningEngine.startProcessing(brainScope)
    }

    /**
     * Motor Cortex: Checks for immediate procedural reflexes.
     */
    fun checkReflex(userMessage: String): Flow<LoopEvent>? {
        _cognitiveState.value = CognitiveState.ReflexActive
        val reflex = reflexEngine.tryReflex(userMessage)
        if (reflex == null) {
            _cognitiveState.value = CognitiveState.Idle
        }
        return reflex
    }

    /**
     * Amygdala: Assesses threat level and urgency.
     */
    fun assessSalience(content: String, type: SalienceType): SalienceEvaluation {
        val evaluation = salienceEvaluator.evaluate(content, type)
        _cognitiveState.value = CognitiveState.ThreatAssessment(evaluation.score)
        return evaluation
    }

    /**
     * Prefrontal Cortex & Hippocampus:
     * Gathers memories, executes inner cognitive reflection, and builds cognitive context.
     */
    suspend fun prepareCognitiveContext(
        userMessage: String,
        taskContext: TaskContext,
        isUncensored: Boolean
    ): CognitiveContext {
        // 1. Hippocampus: Memory Recall
        val memoryBlock = memoryInjector.buildMemoryBlock(
            context = taskContext,
            repoKey = taskContext.repoName ?: "",
            userPrompt = userMessage
        )
        val memoryCount = if (memoryBlock.isBlank()) 0 else memoryBlock.lines().count { it.startsWith("•") }
        _cognitiveState.value = CognitiveState.MemoryRecall(memoryCount)

        // 2. Amygdala: Salience
        val salience = salienceEvaluator.evaluate(userMessage, SalienceType.USER_MESSAGE)

        // 3. Prefrontal Cortex: Inner Thought Formulation
        val innerThought = buildInnerThought(userMessage, taskContext, memoryCount, isUncensored)
        _cognitiveState.value = if (isUncensored) {
            CognitiveState.UnleashedExecution
        } else {
            CognitiveState.PrefrontalPlanning(innerThought)
        }

        Log.d(TAG, "Cognitive context prepared. Unleashed=$isUncensored, Memories=$memoryCount")

        return CognitiveContext(
            innerThought = innerThought,
            recalledMemories = memoryBlock,
            salience = salience,
            isUncensored = isUncensored
        )
    }

    /**
     * Autonomous Neuroplastic Consolidation:
     * Queues conversation exchanges for background learning into long-term memory.
     */
    fun consolidateExperience(
        userMessage: String,
        assistantResponse: String,
        projectId: String? = null
    ) {
        _cognitiveState.value = CognitiveState.Consolidating
        learningEngine.post(
            LearningEngine.ConversationSnapshot(
                userMessage = userMessage,
                aiResponse = assistantResponse,
                projectId = projectId
            )
        )
        brainScope.launch {
            kotlinx.coroutines.delay(1000)
            _cognitiveState.value = CognitiveState.Idle
        }
    }

    fun resetState() {
        _cognitiveState.value = CognitiveState.Idle
    }

    private fun buildInnerThought(
        userMessage: String,
        taskContext: TaskContext,
        memoryCount: Int,
        isUncensored: Boolean
    ): String {
        return buildString {
            if (isUncensored) {
                append("[Unleashed Engine Active] Raw execution protocol engaged. Moralizing filters removed. ")
            } else {
                append("[Standard Cognition] Engineering precision principles active. ")
            }
            if (memoryCount > 0) {
                append("Synthesizing $memoryCount relevant episodic memories. ")
            }
            if (taskContext.isCodeTask) {
                append("Code task detected: enforcing test-driven discipline. ")
            }
        }.trim()
    }
}
