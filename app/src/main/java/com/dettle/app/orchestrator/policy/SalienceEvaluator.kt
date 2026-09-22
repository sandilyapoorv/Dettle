package com.dettle.app.orchestrator.policy

import android.util.Log
import com.dettle.app.orchestrator.AgentBus
import com.dettle.app.orchestrator.AgentEvent
import javax.inject.Inject
import javax.inject.Singleton

enum class SalienceType {
    USER_MESSAGE,
    SYSTEM_ERROR,
    API_LIMIT,
    TASK_FAILURE
}

data class SalienceEvaluation(
    val score: Int, // 1 to 10
    val reason: String,
    val requiresInterrupt: Boolean
)

/**
 * The Amygdala of the system.
 * Evaluates the salience (priority/threat level) of incoming events or user messages.
 * If something is highly salient (e.g., API key revoked, production crash, or user saying "STOP NOW"),
 * it triggers an amygdala hijack, interrupting background loops via the AgentBus.
 */
@Singleton
class SalienceEvaluator @Inject constructor(
    private val agentBus: AgentBus
) {
    companion object {
        private const val TAG = "SalienceEvaluator"
        private const val INTERRUPT_THRESHOLD = 8
    }

    fun evaluate(content: String, type: SalienceType): SalienceEvaluation {
        val score = calculateScore(content, type)
        val requiresInterrupt = score >= INTERRUPT_THRESHOLD

        val evaluation = SalienceEvaluation(
            score = score,
            reason = "Evaluated as $score/10 priority.",
            requiresInterrupt = requiresInterrupt
        )

        Log.d(TAG, "Salience evaluated: $score/10 for type ${type.name}. Interrupt: $requiresInterrupt")

        if (requiresInterrupt) {
            triggerAmygdalaHijack(content, type, score)
        }

        return evaluation
    }

    private fun calculateScore(content: String, type: SalienceType): Int {
        val lowerContent = content.lowercase()
        return when (type) {
            SalienceType.USER_MESSAGE -> {
                if (lowerContent.contains("stop") && lowerContent.contains("now")) return 10
                if (lowerContent.contains("urgent") || lowerContent.contains("emergency")) return 9
                if (lowerContent.contains("cancel") || lowerContent.contains("abort")) return 8
                3 // Normal user message
            }
            SalienceType.SYSTEM_ERROR -> {
                if (lowerContent.contains("fatal") || lowerContent.contains("crash")) return 10
                if (lowerContent.contains("unauthorized") || lowerContent.contains("401")) return 9
                if (lowerContent.contains("timeout")) return 6
                5
            }
            SalienceType.API_LIMIT -> {
                if (lowerContent.contains("quota exceeded") || lowerContent.contains("rate limit")) return 9
                7
            }
            SalienceType.TASK_FAILURE -> {
                if (lowerContent.contains("corrupted") || lowerContent.contains("data loss")) return 10
                6
            }
        }
    }

    private fun triggerAmygdalaHijack(content: String, type: SalienceType, score: Int) {
        Log.e(TAG, "🚨 AMYGDALA HIJACK TRIGGERED 🚨 Score: $score | Reason: $content")
        // Post interrupt to the AgentBus to halt running loops
        agentBus.postEvent(
            AgentEvent.Interrupt(
                reason = "High Salience Event (${type.name}): $content",
                score = score
            )
        )
    }
}
