package com.dettle.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIModelTest {

    @Test
    fun testWaterfallOrderIsNotEmptyAndContainsTopTierModels() {
        assertTrue(FreeModels.WATERFALL_ORDER.isNotEmpty())
        assertEquals(FreeModels.GROQ_LLAMA_8B, FreeModels.WATERFALL_ORDER.first())
        assertTrue(FreeModels.WATERFALL_ORDER.any { it.modelId == "gemini-3.8-flash" })
        assertTrue(FreeModels.WATERFALL_ORDER.any { it.modelId == "gemini-3.5-flash-lite" })
        assertTrue(FreeModels.WATERFALL_ORDER.any { it.modelId == "gemini-3.1-pro-preview" })
    }

    @Test
    fun testAllKnownModelsResolution() {
        // Gemini
        assertNotNull(ALL_KNOWN_MODELS["gemini-3.8-flash"])
        assertNotNull(ALL_KNOWN_MODELS["gemini-3.5-flash-lite"])
        assertNotNull(ALL_KNOWN_MODELS["gemini-3.1-pro-preview"])
        assertNotNull(ALL_KNOWN_MODELS["gemini-2.5-flash"])
        assertNotNull(ALL_KNOWN_MODELS["gemini-2.0-flash"])
        assertNotNull(ALL_KNOWN_MODELS["gemini-2.0-flash-thinking-exp"])

        // Groq
        assertNotNull(ALL_KNOWN_MODELS["llama-3.1-8b-instant"])
        assertNotNull(ALL_KNOWN_MODELS["llama-3.3-70b-versatile"])
        assertNotNull(ALL_KNOWN_MODELS["openai/gpt-oss-120b"])
        assertEquals(AIProviderType.GROQ, ALL_KNOWN_MODELS["llama-3.1-8b-instant"]?.provider)

        // OpenAI
        assertNotNull(ALL_KNOWN_MODELS["gpt-5"])
        assertNotNull(ALL_KNOWN_MODELS["o3-mini"])
        assertNotNull(ALL_KNOWN_MODELS["o1"])
        assertNotNull(ALL_KNOWN_MODELS["gpt-4o"])
        assertEquals(AIProviderType.OPENAI, ALL_KNOWN_MODELS["gpt-5"]?.provider)

        // Anthropic
        assertNotNull(ALL_KNOWN_MODELS["claude-sonnet-5"])
        assertNotNull(ALL_KNOWN_MODELS["claude-3-7-sonnet"])
        assertNotNull(ALL_KNOWN_MODELS["claude-haiku-4.5"])
        assertNotNull(ALL_KNOWN_MODELS["claude-3-5-sonnet"])
        assertEquals(AIProviderType.ANTHROPIC, ALL_KNOWN_MODELS["claude-sonnet-5"]?.provider)

        // DeepSeek
        assertNotNull(ALL_KNOWN_MODELS["deepseek-v4-pro"])
        assertNotNull(ALL_KNOWN_MODELS["deepseek-reasoner"])
        assertNotNull(ALL_KNOWN_MODELS["deepseek-r1"])
        assertEquals(AIProviderType.DEEPSEEK, ALL_KNOWN_MODELS["deepseek-reasoner"]?.provider)

        // Mistral
        assertNotNull(ALL_KNOWN_MODELS["codestral-latest"])
        assertNotNull(ALL_KNOWN_MODELS["mistral-large-latest"])
        assertEquals(AIProviderType.MISTRAL, ALL_KNOWN_MODELS["codestral-latest"]?.provider)

        // Cerebras & xAI
        assertNotNull(ALL_KNOWN_MODELS["grok-3"])
        assertEquals(AIProviderType.XAI, ALL_KNOWN_MODELS["grok-3"]?.provider)
        assertNotNull(ALL_KNOWN_MODELS["llama-3.3-70b"])
        assertEquals(AIProviderType.CEREBRAS, ALL_KNOWN_MODELS["llama-3.3-70b"]?.provider)
    }

    @Test
    fun testAIProviderTypeFromId() {
        assertEquals(AIProviderType.GROQ, AIProviderType.fromId("GROQ"))
        assertEquals(AIProviderType.GEMINI, AIProviderType.fromId("GEMINI"))
        assertEquals(AIProviderType.OPENAI, AIProviderType.fromId("OPENAI"))
        assertEquals(AIProviderType.ANTHROPIC, AIProviderType.fromId("ANTHROPIC"))
        assertEquals(AIProviderType.DEEPSEEK, AIProviderType.fromId("DEEPSEEK"))
        assertEquals(AIProviderType.MISTRAL, AIProviderType.fromId("MISTRAL"))
        assertEquals(AIProviderType.CEREBRAS, AIProviderType.fromId("CEREBRAS"))
        assertEquals(AIProviderType.XAI, AIProviderType.fromId("XAI"))
        assertEquals(AIProviderType.GITHUB_MODELS, AIProviderType.fromId("GITHUB_MODELS"))
    }

    @Test
    fun testThinkingConfigurations() {
        // Gemini 3.8 Flash uses GEMINI_LEVEL
        val geminiFlash = GeminiModels.GEMINI_3_8_FLASH
        assertTrue(geminiFlash.supportsThinking)
        assertEquals(ThinkingType.GEMINI_LEVEL, geminiFlash.thinkingType)
        assertEquals("medium", geminiFlash.defaultThinkingLevel)

        // Gemini 3.1 Pro uses high level thinking
        val geminiPro = GeminiModels.GEMINI_3_1_PRO
        assertTrue(geminiPro.supportsThinking)
        assertEquals(ThinkingType.GEMINI_LEVEL, geminiPro.thinkingType)
        assertEquals("high", geminiPro.defaultThinkingLevel)

        // OpenAI o3-mini uses OPENAI_EFFORT
        val o3Mini = OpenAIModels.O3_MINI
        assertTrue(o3Mini.supportsThinking)
        assertEquals(ThinkingType.OPENAI_EFFORT, o3Mini.thinkingType)
        assertEquals(false, o3Mini.supportsTemperature)

        // Anthropic Claude Sonnet 5 uses ANTHROPIC_BUDGET
        val claudeSonnet = AnthropicModels.CLAUDE_SONNET_5
        assertTrue(claudeSonnet.supportsThinking)
        assertEquals(ThinkingType.ANTHROPIC_BUDGET, claudeSonnet.thinkingType)
        assertTrue(claudeSonnet.defaultThinkingBudget >= 1024)

        // DeepSeek R1 uses REASONING_CONTENT_DELTA
        val deepseekR1 = DeepSeekModels.DEEPSEEK_R1
        assertTrue(deepseekR1.supportsThinking)
        assertEquals(ThinkingType.REASONING_CONTENT_DELTA, deepseekR1.thinkingType)
    }

    @Test
    fun testModelSetsCuration() {
        assertTrue(ModelSets.FAST.contains(FreeModels.GROQ_LLAMA_8B))
        assertTrue(ModelSets.FAST.contains(GeminiModels.GEMINI_3_5_FLASH_LITE))
        assertTrue(ModelSets.LARGE_CONTEXT.contains(GeminiModels.GEMINI_3_8_FLASH))
        assertTrue(ModelSets.DEEP_REASONING.contains(GeminiModels.GEMINI_3_1_PRO))
        assertTrue(ModelSets.BEST_QUALITY.contains(AnthropicModels.CLAUDE_SONNET_5))
        assertTrue(ModelSets.CODE_REVIEW.contains(GeminiModels.GEMINI_3_1_PRO))
    }
}
