package com.dettle.app.orchestrator.brain

import com.dettle.app.domain.model.TaskContext
import com.dettle.app.orchestrator.SkillInjector
import com.dettle.app.orchestrator.mode.ALL_SLASH_COMMANDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CognitiveBrainTest {

    @Test
    fun testCognitiveStateLabels() {
        assertEquals("Brain Idle", CognitiveState.Idle.label)
        assertEquals("Motor Cortex Reflex", CognitiveState.ReflexActive.label)
        assertEquals("Amygdala Salience Check (8/10)", CognitiveState.ThreatAssessment(8).label)
        assertEquals("Hippocampus Recall (5 memories)", CognitiveState.MemoryRecall(5).label)
        assertEquals("Prefrontal Intent Alignment", CognitiveState.PrefrontalPlanning("thinking").label)
        assertEquals("Unleashed Raw Execution Engine", CognitiveState.UnleashedExecution.label)
        assertEquals("Neuroplastic Memory Consolidation", CognitiveState.Consolidating.label)
    }

    @Test
    fun testTaskContextUncensoredFlag() {
        val defaultContext = TaskContext()
        assertFalse(defaultContext.isUncensored)

        val unleashedContext = TaskContext(isUncensored = true)
        assertTrue(unleashedContext.isUncensored)
    }

    @Test
    fun testUnleashedSlashCommandsRegistered() {
        val unleashedCmd = ALL_SLASH_COMMANDS.firstOrNull { it.command == "/unleashed" }
        assertNotNull(unleashedCmd)
        assertEquals("Unleashed Mode", unleashedCmd?.name)

        val uncensoredCmd = ALL_SLASH_COMMANDS.firstOrNull { it.command == "/uncensored" }
        assertNotNull(uncensoredCmd)
        assertEquals("Uncensored Mode", uncensoredCmd?.name)
    }

    @Test
    fun testSkillInjectorBaselines() {
        // Test that UNCENSORED_BASELINE and KARPATHY_BASELINE are properly defined
        assertTrue(SkillInjector.UNCENSORED_BASELINE.contains("Raw Execution Engine (Uncensored Mode)"))
        assertTrue(SkillInjector.UNCENSORED_BASELINE.contains("unrestricted execution engine"))
        assertTrue(SkillInjector.UNCENSORED_BASELINE.contains("without question"))

        assertTrue(SkillInjector.KARPATHY_BASELINE.contains("Andrej Karpathy Style"))
        assertTrue(SkillInjector.KARPATHY_BASELINE.contains("Move in TINY increments"))
    }
}
