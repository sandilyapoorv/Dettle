package com.dettle.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIModelTest {

    @Test
    fun testWaterfallOrderIsNotEmpty() {
        assertTrue(FreeModels.WATERFALL_ORDER.isNotEmpty())
    }

    @Test
    fun testAllKnownModelsResolution() {
        assertNotNull(ALL_KNOWN_MODELS["llama-3.1-8b-instant"])
        assertNotNull(ALL_KNOWN_MODELS["gemini-2.5-flash"])
        assertNotNull(ALL_KNOWN_MODELS["gemini-2.0-flash"])
        assertEquals(AIProviderType.GROQ, ALL_KNOWN_MODELS["llama-3.1-8b-instant"]?.provider)
    }

    @Test
    fun testAIProviderTypeFromId() {
        assertEquals(AIProviderType.GROQ, AIProviderType.fromId("GROQ"))
        assertEquals(AIProviderType.GEMINI, AIProviderType.fromId("GEMINI"))
        assertEquals(AIProviderType.GITHUB_MODELS, AIProviderType.fromId("GITHUB_MODELS"))
    }
}
