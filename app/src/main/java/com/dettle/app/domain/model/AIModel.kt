package com.dettle.app.domain.model

/**
 * Unified provider identity covering official APIs (Track A) and WebView bridge (Track B).
 */
enum class AIProviderType(
    val displayName: String,
    val baseUrl: String,
    val isWebView: Boolean = false,
    val isFree: Boolean = true,
    val loginUrl: String = baseUrl
) {
    // ── Track A: Official API Providers ──────────────────────────────────
    GROQ("Groq", "https://api.groq.com/openai/v1"),
    GEMINI("Google AI Studio", "https://generativelanguage.googleapis.com/v1beta"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1"),
    SAMBANOVA("SambaNova", "https://api.sambanova.ai/v1"),
    GITHUB_MODELS("GitHub Models", "https://models.inference.ai.azure.com"),
    LOCAL_OLLAMA("Ollama (Local/Custom)", "http://10.0.2.2:11434/api"),

    // Direct official API additions
    OPENAI("OpenAI", "https://api.openai.com/v1", isFree = false),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1", isFree = false),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", isFree = false),
    MISTRAL("Mistral AI", "https://api.mistral.ai/v1", isFree = false),
    CEREBRAS("Cerebras", "https://api.cerebras.ai/v1", isFree = false),
    XAI("xAI (Grok)", "https://api.x.ai/v1", isFree = false),

    // ── Track B: WebView bridge (user's own subscriptions) ────────────────
    CHATGPT_WEB("ChatGPT", "https://chatgpt.com", isWebView = true, loginUrl = "https://chatgpt.com/auth/login"),
    CLAUDE_WEB("Claude", "https://claude.ai", isWebView = true, loginUrl = "https://claude.ai/login"),
    DEEPSEEK_WEB("DeepSeek", "https://chat.deepseek.com", isWebView = true, loginUrl = "https://chat.deepseek.com/sign_in"),
    GROK_WEB("Grok", "https://grok.com", isWebView = true, loginUrl = "https://grok.com"),
    GEMINI_WEB("Gemini Web", "https://gemini.google.com", isWebView = true, loginUrl = "https://gemini.google.com"),
    KIMI_WEB("Kimi", "https://kimi.ai", isWebView = true, loginUrl = "https://kimi.ai"),
    MISTRAL_WEB("Mistral Chat", "https://chat.mistral.ai", isWebView = true, loginUrl = "https://chat.mistral.ai/auth/login"),
    QWEN_WEB("Qwen", "https://chat.qwenlm.ai", isWebView = true, loginUrl = "https://chat.qwenlm.ai");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
    }
}

/**
 * Protocol mechanism used to configure thinking / reasoning depth.
 */
enum class ThinkingType {
    NONE,
    GEMINI_BUDGET,           // Google AI Studio thinkingConfig.thinkingBudget (int)
    GEMINI_LEVEL,            // Google AI Studio thinkingConfig.thinkingLevel ("minimal"|"low"|"medium"|"high")
    OPENAI_EFFORT,           // OpenAI reasoning_effort ("low"|"medium"|"high")
    ANTHROPIC_BUDGET,        // Anthropic thinking.budget_tokens (int)
    REASONING_CONTENT_DELTA  // Streamed via choices[0].delta.reasoning_content (DeepSeek R1, Groq, Ollama)
}

/**
 * Functional workload role for optimal model selection.
 */
enum class ModelTier {
    FAST_ROUTING,
    HEAVY_CODING,
    DEEP_REASONING,
    LARGE_CONTEXT,
    FLAGSHIP,
    BALANCED,
    UNCENSORED
}

/**
 * Complete, rich specification of an AI model across any provider.
 */
data class AIModel(
    val provider: AIProviderType,
    val modelId: String,          // e.g. "gemini-3.8-flash"
    val displayName: String,
    val contextWindow: Int,       // tokens
    val dailyTokenLimit: Int = -1,     // -1 = unlimited/unknown
    val dailyRequestLimit: Int = -1,   // -1 = unlimited/unknown
    val rpmLimit: Int = -1,            // requests per minute
    val isFree: Boolean = true,
    val supportsToolCalling: Boolean = true,
    val bestFor: String = "",      // human-readable description of ideal use case
    val maxOutputTokens: Int = 4096,
    val supportsThinking: Boolean = false,
    val thinkingType: ThinkingType = ThinkingType.NONE,
    val defaultThinkingBudget: Int = 0,
    val defaultThinkingLevel: String = "",
    val supportsVision: Boolean = false,
    val supportsSystemPrompt: Boolean = true,
    val supportsTemperature: Boolean = true,
    val tier: ModelTier = ModelTier.BALANCED
)

// ══════════════════════════════════════════════════════════════════════════════
// PROVIDER SPECIFIC CATALOGS
// ══════════════════════════════════════════════════════════════════════════════

/** Google AI Studio (Gemini) models */
object GeminiModels {
    val GEMINI_3_8_FLASH = AIModel(
        provider = AIProviderType.GEMINI,
        modelId = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        contextWindow = 1_048_576,
        maxOutputTokens = 65_536,
        dailyRequestLimit = 1_500,
        rpmLimit = 15,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.GEMINI_LEVEL,
        defaultThinkingLevel = "medium",
        supportsVision = true,
        tier = ModelTier.HEAVY_CODING,
        bestFor = "Primary high-throughput agent loops, heavy coding, prompt caching"
    )

    val GEMINI_3_5_FLASH_LITE = AIModel(
        provider = AIProviderType.GEMINI,
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        contextWindow = 1_048_576,
        maxOutputTokens = 65_536,
        dailyRequestLimit = 1_500,
        rpmLimit = 30,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.GEMINI_LEVEL,
        defaultThinkingLevel = "minimal",
        supportsVision = true,
        tier = ModelTier.FAST_ROUTING,
        bestFor = "Fast subagent dispatch, intent routing, document parsing, low-latency turns"
    )

    val GEMINI_3_1_PRO = AIModel(
        provider = AIProviderType.GEMINI,
        modelId = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro",
        contextWindow = 2_097_152,
        maxOutputTokens = 65_536,
        dailyRequestLimit = 50,
        rpmLimit = 5,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.GEMINI_LEVEL,
        defaultThinkingLevel = "high",
        supportsVision = true,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Deep reasoning, architectural planning, multi-file refactoring, code review"
    )

    val GEMINI_3_FLASH_PREVIEW = AIModel(
        provider = AIProviderType.GEMINI,
        modelId = "gemini-3-flash-preview",
        displayName = "Gemini 3 Flash Preview",
        contextWindow = 1_048_576,
        maxOutputTokens = 65_536,
        dailyRequestLimit = 500,
        rpmLimit = 10,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.GEMINI_LEVEL,
        defaultThinkingLevel = "medium",
        supportsVision = true,
        tier = ModelTier.BALANCED,
        bestFor = "Frontier experimental Flash preview"
    )

    val GEMINI_2_5_FLASH = AIModel(
        provider = AIProviderType.GEMINI,
        modelId = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        contextWindow = 1_048_576,
        maxOutputTokens = 8_192,
        dailyRequestLimit = 1_500,
        rpmLimit = 15,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.GEMINI_BUDGET,
        defaultThinkingBudget = 2048,
        supportsVision = true,
        tier = ModelTier.HEAVY_CODING,
        bestFor = "Stable fallback coding, large codebase reading"
    )

    val GEMINI_2_5_PRO = AIModel(
        provider = AIProviderType.GEMINI,
        modelId = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        contextWindow = 2_097_152,
        maxOutputTokens = 8_192,
        dailyRequestLimit = 50,
        rpmLimit = 5,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.GEMINI_BUDGET,
        defaultThinkingBudget = 4096,
        supportsVision = true,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Stable 2M context deep analysis and reasoning"
    )
}

/** Groq LPU Ultra-Fast Models */
object GroqModels {
    val LLAMA_3_1_8B_INSTANT = AIModel(
        provider = AIProviderType.GROQ,
        modelId = "llama-3.1-8b-instant",
        displayName = "Llama 3.1 8B (Groq)",
        contextWindow = 131_072,
        maxOutputTokens = 131_072,
        dailyTokenLimit = 500_000,
        dailyRequestLimit = 14_400,
        rpmLimit = 30,
        supportsToolCalling = true,
        tier = ModelTier.FAST_ROUTING,
        bestFor = "Fast routing decisions, agent inner loops, intent classification (< 400ms)"
    )

    val LLAMA_3_3_70B_VERSATILE = AIModel(
        provider = AIProviderType.GROQ,
        modelId = "llama-3.3-70b-versatile",
        displayName = "Llama 3.3 70B (Groq)",
        contextWindow = 131_072,
        maxOutputTokens = 32_768,
        dailyTokenLimit = 100_000,
        dailyRequestLimit = 1_000,
        rpmLimit = 30,
        supportsToolCalling = true,
        tier = ModelTier.HEAVY_CODING,
        bestFor = "High-speed reasoning, orchestrator decisions, code generation"
    )

    val GPT_OSS_120B = AIModel(
        provider = AIProviderType.GROQ,
        modelId = "openai/gpt-oss-120b",
        displayName = "GPT OSS 120B (Groq)",
        contextWindow = 131_072,
        maxOutputTokens = 32_768,
        rpmLimit = 30,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.REASONING_CONTENT_DELTA,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Open-weights frontier reasoning on LPU hardware"
    )
}

/** Direct OpenAI Official Models */
object OpenAIModels {
    val GPT_5 = AIModel(
        provider = AIProviderType.OPENAI,
        modelId = "gpt-5",
        displayName = "GPT-5 (OpenAI)",
        contextWindow = 256_000,
        maxOutputTokens = 32_768,
        isFree = false,
        supportsToolCalling = true,
        supportsVision = true,
        tier = ModelTier.FLAGSHIP,
        bestFor = "Flagship unified multimodal agentic reasoning and synthesis"
    )

    val O3_MINI = AIModel(
        provider = AIProviderType.OPENAI,
        modelId = "o3-mini",
        displayName = "o3-mini (OpenAI)",
        contextWindow = 200_000,
        maxOutputTokens = 100_000,
        isFree = false,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.OPENAI_EFFORT,
        defaultThinkingLevel = "medium",
        supportsTemperature = false,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Competitive coding, math, unit test generation, algorithmic reasoning"
    )

    val O1 = AIModel(
        provider = AIProviderType.OPENAI,
        modelId = "o1",
        displayName = "o1 (OpenAI)",
        contextWindow = 200_000,
        maxOutputTokens = 100_000,
        isFree = false,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.OPENAI_EFFORT,
        defaultThinkingLevel = "medium",
        supportsTemperature = false,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Complex architecture design, deep multi-step debugging"
    )

    val GPT_4O = AIModel(
        provider = AIProviderType.OPENAI,
        modelId = "gpt-4o",
        displayName = "GPT-4o (OpenAI)",
        contextWindow = 128_000,
        maxOutputTokens = 16_384,
        isFree = false,
        supportsToolCalling = true,
        supportsVision = true,
        tier = ModelTier.BALANCED,
        bestFor = "Multimodal coding, dependable instruction following"
    )

    val GPT_4O_MINI = AIModel(
        provider = AIProviderType.OPENAI,
        modelId = "gpt-4o-mini",
        displayName = "GPT-4o Mini (OpenAI)",
        contextWindow = 128_000,
        maxOutputTokens = 16_384,
        isFree = false,
        supportsToolCalling = true,
        supportsVision = true,
        tier = ModelTier.FAST_ROUTING,
        bestFor = "Fast, low-cost everyday coding and utility transforms"
    )
}

/** Direct Anthropic Claude Models */
object AnthropicModels {
    val CLAUDE_SONNET_5 = AIModel(
        provider = AIProviderType.ANTHROPIC,
        modelId = "claude-sonnet-5",
        displayName = "Claude Sonnet 5",
        contextWindow = 1_000_000,
        maxOutputTokens = 64_000,
        isFree = false,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.ANTHROPIC_BUDGET,
        defaultThinkingBudget = 4096,
        supportsVision = true,
        tier = ModelTier.FLAGSHIP,
        bestFor = "Gold-standard agentic coding, autonomous refactoring, deep reasoning"
    )

    val CLAUDE_3_7_SONNET = AIModel(
        provider = AIProviderType.ANTHROPIC,
        modelId = "claude-3-7-sonnet",
        displayName = "Claude 3.7 Sonnet",
        contextWindow = 200_000,
        maxOutputTokens = 64_000,
        isFree = false,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.ANTHROPIC_BUDGET,
        defaultThinkingBudget = 4096,
        supportsVision = true,
        tier = ModelTier.HEAVY_CODING,
        bestFor = "Precision code synthesis, comprehensive PR review"
    )

    val CLAUDE_HAIKU_4_5 = AIModel(
        provider = AIProviderType.ANTHROPIC,
        modelId = "claude-haiku-4.5",
        displayName = "Claude Haiku 4.5",
        contextWindow = 200_000,
        maxOutputTokens = 8_192,
        isFree = false,
        supportsToolCalling = true,
        supportsVision = true,
        tier = ModelTier.FAST_ROUTING,
        bestFor = "Low-latency subagent tasks, quick linting, fast summaries"
    )

    val CLAUDE_OPUS_5_5 = AIModel(
        provider = AIProviderType.ANTHROPIC,
        modelId = "claude-opus-5.5",
        displayName = "Claude Opus 5.5",
        contextWindow = 1_000_000,
        maxOutputTokens = 64_000,
        isFree = false,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.ANTHROPIC_BUDGET,
        defaultThinkingBudget = 8192,
        supportsVision = true,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Long-horizon agent coordination, maximum intelligence"
    )
}

/** Direct DeepSeek Official Models */
object DeepSeekModels {
    val DEEPSEEK_V4_PRO = AIModel(
        provider = AIProviderType.DEEPSEEK,
        modelId = "deepseek-v4-pro",
        displayName = "DeepSeek V4 Pro",
        contextWindow = 128_000,
        maxOutputTokens = 8_192,
        isFree = false,
        supportsToolCalling = true,
        tier = ModelTier.HEAVY_CODING,
        bestFor = "High efficiency coding and structured generation"
    )

    val DEEPSEEK_R1 = AIModel(
        provider = AIProviderType.DEEPSEEK,
        modelId = "deepseek-reasoner",
        displayName = "DeepSeek R1",
        contextWindow = 128_000,
        maxOutputTokens = 8_192,
        isFree = false,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.REASONING_CONTENT_DELTA,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Frontier open reasoning, algorithmic logic, deep verification"
    )

    val DEEPSEEK_V3 = AIModel(
        provider = AIProviderType.DEEPSEEK,
        modelId = "deepseek-chat",
        displayName = "DeepSeek V3",
        contextWindow = 128_000,
        maxOutputTokens = 8_192,
        isFree = false,
        supportsToolCalling = true,
        tier = ModelTier.BALANCED,
        bestFor = "Fast coding, documentation, general conversations"
    )
}

/** Mistral AI Models */
object MistralModels {
    val CODESTRAL = AIModel(
        provider = AIProviderType.MISTRAL,
        modelId = "codestral-latest",
        displayName = "Codestral (Mistral)",
        contextWindow = 256_000,
        maxOutputTokens = 16_384,
        isFree = false,
        supportsToolCalling = true,
        tier = ModelTier.HEAVY_CODING,
        bestFor = "Specialized code generation, fill-in-the-middle, unit tests"
    )

    val MISTRAL_LARGE = AIModel(
        provider = AIProviderType.MISTRAL,
        modelId = "mistral-large-latest",
        displayName = "Mistral Large",
        contextWindow = 128_000,
        maxOutputTokens = 16_384,
        isFree = false,
        supportsToolCalling = true,
        tier = ModelTier.FLAGSHIP,
        bestFor = "Complex reasoning, multilingual capabilities, agentic pipelines"
    )

    val MINISTRAL_8B = AIModel(
        provider = AIProviderType.MISTRAL,
        modelId = "ministral-8b-latest",
        displayName = "Ministral 8B",
        contextWindow = 128_000,
        maxOutputTokens = 8_192,
        isFree = false,
        supportsToolCalling = true,
        tier = ModelTier.FAST_ROUTING,
        bestFor = "Low-latency edge tasks and quick queries"
    )

    val PIXTRAL_LARGE = AIModel(
        provider = AIProviderType.MISTRAL,
        modelId = "pixtral-large-latest",
        displayName = "Pixtral Large",
        contextWindow = 128_000,
        maxOutputTokens = 8_192,
        isFree = false,
        supportsToolCalling = true,
        supportsVision = true,
        tier = ModelTier.BALANCED,
        bestFor = "Multimodal code diagrams, UI screenshots, and visual inspection"
    )
}

/** OpenRouter Aggregated Models */
object OpenRouterModels {
    val LLAMA_3_3_70B_FREE = AIModel(
        provider = AIProviderType.OPENROUTER,
        modelId = "meta-llama/llama-3.3-70b-instruct:free",
        displayName = "Llama 3.3 70B (OpenRouter Free)",
        contextWindow = 131_072,
        dailyRequestLimit = 1_000,
        rpmLimit = 20,
        supportsToolCalling = true,
        tier = ModelTier.HEAVY_CODING,
        bestFor = "General free fallback when primary provider is rate-limited"
    )

    val QWEN_2_5_72B_FREE = AIModel(
        provider = AIProviderType.OPENROUTER,
        modelId = "qwen/qwen-2.5-72b-instruct:free",
        displayName = "Qwen 2.5 72B (OpenRouter Free)",
        contextWindow = 131_072,
        dailyRequestLimit = 1_000,
        rpmLimit = 20,
        supportsToolCalling = true,
        tier = ModelTier.HEAVY_CODING,
        bestFor = "Code generation, algorithmic problems, multi-language tasks"
    )

    val DEEPSEEK_R1_FREE = AIModel(
        provider = AIProviderType.OPENROUTER,
        modelId = "deepseek/deepseek-r1:free",
        displayName = "DeepSeek R1 (OpenRouter Free)",
        contextWindow = 64_000,
        dailyRequestLimit = 500,
        rpmLimit = 10,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.REASONING_CONTENT_DELTA,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Free reasoning fallback with chain-of-thought"
    )

    val NEMOTRON_3_5_FREE = AIModel(
        provider = AIProviderType.OPENROUTER,
        modelId = "nvidia/nemotron-3.5-lightning:free",
        displayName = "Nemotron 3.5 Lightning (Free)",
        contextWindow = 1_000_000,
        dailyRequestLimit = 500,
        rpmLimit = 15,
        supportsToolCalling = true,
        tier = ModelTier.LARGE_CONTEXT,
        bestFor = "Massive 1M context reading at zero API cost"
    )
}

/** SambaNova High-Speed Models */
object SambaNovaModels {
    val SAMBANOVA_LLAMA_3_3_70B = AIModel(
        provider = AIProviderType.SAMBANOVA,
        modelId = "Meta-Llama-3.3-70B-Instruct",
        displayName = "Llama 3.3 70B (SambaNova)",
        contextWindow = 128_000,
        dailyTokenLimit = 200_000,
        rpmLimit = 60,
        supportsToolCalling = true,
        tier = ModelTier.LARGE_CONTEXT,
        bestFor = "Massive codebase reads, high-speed context processing"
    )

    val SAMBANOVA_DEEPSEEK_R1 = AIModel(
        provider = AIProviderType.SAMBANOVA,
        modelId = "DeepSeek-R1",
        displayName = "DeepSeek R1 (SambaNova)",
        contextWindow = 128_000,
        rpmLimit = 30,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.REASONING_CONTENT_DELTA,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "High-throughput deep reasoning on SambaStack"
    )

    val SAMBANOVA_LLAMA_405B = AIModel(
        provider = AIProviderType.SAMBANOVA,
        modelId = "Meta-Llama-3.1-405B-Instruct",
        displayName = "Llama 3.1 405B (SambaNova)",
        contextWindow = 128_000,
        rpmLimit = 20,
        supportsToolCalling = true,
        tier = ModelTier.FLAGSHIP,
        bestFor = "Maximum scale open model reasoning and code architecture"
    )
}

/** Cerebras Wafer-Scale Fast Models */
object CerebrasModels {
    val CEREBRAS_GPT_OSS_120B = AIModel(
        provider = AIProviderType.CEREBRAS,
        modelId = "openai/gpt-oss-120b",
        displayName = "GPT OSS 120B (Cerebras)",
        contextWindow = 131_072,
        maxOutputTokens = 32_768,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.REASONING_CONTENT_DELTA,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Ultra-fast wafer-scale reasoning (~1,500 tokens/sec)"
    )

    val CEREBRAS_LLAMA_3_3_70B = AIModel(
        provider = AIProviderType.CEREBRAS,
        modelId = "llama-3.3-70b",
        displayName = "Llama 3.3 70B (Cerebras)",
        contextWindow = 131_072,
        maxOutputTokens = 32_768,
        supportsToolCalling = true,
        tier = ModelTier.HEAVY_CODING,
        bestFor = "Ultra-fast code generation on CS-3 hardware"
    )
}

/** xAI Grok Models */
object XAIModels {
    val GROK_3 = AIModel(
        provider = AIProviderType.XAI,
        modelId = "grok-3",
        displayName = "Grok 3 (xAI)",
        contextWindow = 131_072,
        maxOutputTokens = 16_384,
        isFree = false,
        supportsToolCalling = true,
        tier = ModelTier.FLAGSHIP,
        bestFor = "Frontier agentic reasoning and truthful answers"
    )

    val GROK_3_MINI = AIModel(
        provider = AIProviderType.XAI,
        modelId = "grok-3-mini",
        displayName = "Grok 3 Mini (xAI)",
        contextWindow = 131_072,
        maxOutputTokens = 16_384,
        isFree = false,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.REASONING_CONTENT_DELTA,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Fast reasoning with thinking traces"
    )

    val GROK_2 = AIModel(
        provider = AIProviderType.XAI,
        modelId = "grok-2",
        displayName = "Grok 2 (xAI)",
        contextWindow = 128_000,
        maxOutputTokens = 8_192,
        isFree = false,
        supportsToolCalling = true,
        tier = ModelTier.BALANCED,
        bestFor = "Reliable general coding and web research"
    )
}

/** Local and Network Ollama Models */
object OllamaModels {
    val DOLPHIN_MIXTRAL_UNCENSORED = AIModel(
        provider = AIProviderType.LOCAL_OLLAMA,
        modelId = "dolphin-mixtral",
        displayName = "Dolphin Mixtral (Uncensored)",
        contextWindow = 32_000,
        supportsToolCalling = true,
        tier = ModelTier.UNCENSORED,
        bestFor = "Raw execution without any safety guardrails or apologies"
    )

    val QWEN_2_5_CODER_32B = AIModel(
        provider = AIProviderType.LOCAL_OLLAMA,
        modelId = "qwen2.5-coder:32b",
        displayName = "Qwen 2.5 Coder 32B (Local)",
        contextWindow = 32_000,
        supportsToolCalling = true,
        tier = ModelTier.HEAVY_CODING,
        bestFor = "Private local code synthesis without cloud dependencies"
    )

    val DEEPSEEK_R1_14B = AIModel(
        provider = AIProviderType.LOCAL_OLLAMA,
        modelId = "deepseek-r1:14b",
        displayName = "DeepSeek R1 14B (Local)",
        contextWindow = 32_000,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.REASONING_CONTENT_DELTA,
        tier = ModelTier.DEEP_REASONING,
        bestFor = "Private local chain-of-thought reasoning"
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// FREE & DEFAULT WATERFALL DEFINITIONS (BACKWARD COMPATIBLE)
// ══════════════════════════════════════════════════════════════════════════════

/** All free models available in the system, maintaining exact backward compatibility */
object FreeModels {

    // Groq — ultra-fast, great for routing decisions
    val GROQ_LLAMA_8B = GroqModels.LLAMA_3_1_8B_INSTANT
    val GROQ_LLAMA_70B = GroqModels.LLAMA_3_3_70B_VERSATILE
    val GROQ_GPT_OSS_120B = GroqModels.GPT_OSS_120B

    // Uncensored / Local Models (Ollama Network)
    val OLLAMA_DOLPHIN_UNCENSORED = OllamaModels.DOLPHIN_MIXTRAL_UNCENSORED

    // Google AI Studio — latest Gemini series
    val GEMINI_3_8_FLASH = GeminiModels.GEMINI_3_8_FLASH
    val GEMINI_3_5_FLASH_LITE = GeminiModels.GEMINI_3_5_FLASH_LITE
    val GEMINI_3_1_PRO = GeminiModels.GEMINI_3_1_PRO

    // Backward-compatible references (auto-mapped to best flash / pro models)
    val GEMINI_FLASH = GeminiModels.GEMINI_3_8_FLASH
    val GEMINI_FLASH_THINKING = GeminiModels.GEMINI_3_1_PRO

    // OpenRouter free models
    val OPENROUTER_LLAMA = OpenRouterModels.LLAMA_3_3_70B_FREE
    val OPENROUTER_QWEN = OpenRouterModels.QWEN_2_5_72B_FREE
    val OPENROUTER_DEEPSEEK_R1 = OpenRouterModels.DEEPSEEK_R1_FREE
    val OPENROUTER_NEMOTRON = OpenRouterModels.NEMOTRON_3_5_FREE

    // SambaNova — great for massive context
    val SAMBANOVA_LLAMA_70B = SambaNovaModels.SAMBANOVA_LLAMA_3_3_70B

    // GitHub Models
    val GITHUB_GPT4O = AIModel(
        provider = AIProviderType.GITHUB_MODELS,
        modelId = "gpt-4o",
        displayName = "GPT-4o (GitHub Models)",
        contextWindow = 128_000,
        dailyTokenLimit = 200_000,
        dailyRequestLimit = 50,
        rpmLimit = 10,
        supportsToolCalling = true,
        bestFor = "Final complex coding tasks where quality matters most"
    )

    val GITHUB_DEEPSEEK_R1 = AIModel(
        provider = AIProviderType.GITHUB_MODELS,
        modelId = "DeepSeek-R1",
        displayName = "DeepSeek R1 (GitHub Models)",
        contextWindow = 64_000,
        dailyTokenLimit = 200_000,
        dailyRequestLimit = 150,
        rpmLimit = 10,
        supportsToolCalling = true,
        supportsThinking = true,
        thinkingType = ThinkingType.REASONING_CONTENT_DELTA,
        bestFor = "Complex reasoning, planning, structured decomposition"
    )

    /** Default waterfall order — most generous/fastest first */
    val WATERFALL_ORDER: List<AIModel> = listOf(
        GROQ_LLAMA_8B,                  // Primary: fast routing (< 400ms)
        GeminiModels.GEMINI_3_8_FLASH,  // Heavy lifting (1M context)
        GeminiModels.GEMINI_3_5_FLASH_LITE, // High throughput agent turns
        GeminiModels.GEMINI_3_1_PRO,    // Deep reasoning & architecture
        GroqModels.LLAMA_3_3_70B_VERSATILE, // Fast reasoning
        SAMBANOVA_LLAMA_70B,            // Large context reads
        OPENROUTER_LLAMA,               // Fallback 1
        OPENROUTER_QWEN,                // Fallback 2
        GITHUB_GPT4O,                   // Best quality
        GITHUB_DEEPSEEK_R1              // Best reasoning
    )
}

/**
 * Curated model sets per use-case.
 * Each [AgentMode] picks a set — [KeyPoolManager] tries them in order, failing over as needed.
 */
object ModelSets {
    /** Intent classification, inner loop decisions, fast turns */
    val FAST: List<AIModel> = listOf(
        FreeModels.GROQ_LLAMA_8B,
        GeminiModels.GEMINI_3_5_FLASH_LITE,
        GeminiModels.GEMINI_3_8_FLASH,
        FreeModels.OPENROUTER_LLAMA,
        FreeModels.GROQ_LLAMA_70B
    )

    /** Reading large codebases, web pages, long documents */
    val LARGE_CONTEXT: List<AIModel> = listOf(
        GeminiModels.GEMINI_3_8_FLASH,        // 1M context
        GeminiModels.GEMINI_3_1_PRO,          // 2M context
        FreeModels.SAMBANOVA_LLAMA_70B,       // 128k context
        OpenRouterModels.NEMOTRON_3_5_FREE,   // 1M context free
        MistralModels.CODESTRAL               // 256k context
    )

    /** Structured planning, deep reasoning, architectural decisions */
    val DEEP_REASONING: List<AIModel> = listOf(
        GeminiModels.GEMINI_3_1_PRO,          // Gemini Deep Think
        OpenAIModels.O3_MINI,                 // o3-mini reasoning effort
        AnthropicModels.CLAUDE_SONNET_5,      // Claude 5 adaptive thinking
        DeepSeekModels.DEEPSEEK_R1,           // DeepSeek R1 Chain-of-thought
        FreeModels.GITHUB_DEEPSEEK_R1,
        FreeModels.GROQ_LLAMA_70B
    )

    /** Highest quality code generation — used sparingly (tight limits or paid) */
    val BEST_QUALITY: List<AIModel> = listOf(
        AnthropicModels.CLAUDE_SONNET_5,
        OpenAIModels.GPT_5,
        GeminiModels.GEMINI_3_1_PRO,
        MistralModels.CODESTRAL,
        FreeModels.GITHUB_GPT4O,
        GeminiModels.GEMINI_3_8_FLASH
    )

    /** Code review — reasoning + reading ability combined */
    val CODE_REVIEW: List<AIModel> = listOf(
        GeminiModels.GEMINI_3_1_PRO,
        AnthropicModels.CLAUDE_3_7_SONNET,
        DeepSeekModels.DEEPSEEK_R1,
        GeminiModels.GEMINI_3_8_FLASH
    )

    /** Full waterfall — used by Goal mode */
    val ALL: List<AIModel> = FreeModels.WATERFALL_ORDER
}

/** All known models by ID — used for resolving waterfall model IDs back to AIModel objects */
val ALL_KNOWN_MODELS: Map<String, AIModel> = listOf(
    // Gemini
    GeminiModels.GEMINI_3_8_FLASH,
    GeminiModels.GEMINI_3_5_FLASH_LITE,
    GeminiModels.GEMINI_3_1_PRO,
    GeminiModels.GEMINI_3_FLASH_PREVIEW,
    GeminiModels.GEMINI_2_5_FLASH,
    GeminiModels.GEMINI_2_5_PRO,

    // Groq
    GroqModels.LLAMA_3_1_8B_INSTANT,
    GroqModels.LLAMA_3_3_70B_VERSATILE,
    GroqModels.GPT_OSS_120B,

    // OpenAI
    OpenAIModels.GPT_5,
    OpenAIModels.O3_MINI,
    OpenAIModels.O1,
    OpenAIModels.GPT_4O,
    OpenAIModels.GPT_4O_MINI,

    // Anthropic
    AnthropicModels.CLAUDE_SONNET_5,
    AnthropicModels.CLAUDE_3_7_SONNET,
    AnthropicModels.CLAUDE_HAIKU_4_5,
    AnthropicModels.CLAUDE_OPUS_5_5,

    // DeepSeek
    DeepSeekModels.DEEPSEEK_V4_PRO,
    DeepSeekModels.DEEPSEEK_R1,
    DeepSeekModels.DEEPSEEK_V3,

    // Mistral
    MistralModels.CODESTRAL,
    MistralModels.MISTRAL_LARGE,
    MistralModels.MINISTRAL_8B,
    MistralModels.PIXTRAL_LARGE,

    // OpenRouter
    OpenRouterModels.LLAMA_3_3_70B_FREE,
    OpenRouterModels.QWEN_2_5_72B_FREE,
    OpenRouterModels.DEEPSEEK_R1_FREE,
    OpenRouterModels.NEMOTRON_3_5_FREE,

    // SambaNova
    SambaNovaModels.SAMBANOVA_LLAMA_3_3_70B,
    SambaNovaModels.SAMBANOVA_DEEPSEEK_R1,
    SambaNovaModels.SAMBANOVA_LLAMA_405B,

    // Cerebras
    CerebrasModels.CEREBRAS_GPT_OSS_120B,
    CerebrasModels.CEREBRAS_LLAMA_3_3_70B,

    // xAI
    XAIModels.GROK_3,
    XAIModels.GROK_3_MINI,
    XAIModels.GROK_2,

    // Ollama
    OllamaModels.DOLPHIN_MIXTRAL_UNCENSORED,
    OllamaModels.QWEN_2_5_CODER_32B,
    OllamaModels.DEEPSEEK_R1_14B,

    // GitHub Models
    FreeModels.GITHUB_GPT4O,
    FreeModels.GITHUB_DEEPSEEK_R1
).associateBy { it.modelId } + mapOf(
    // Backward compatibility & Aliases for deprecated or alternate model IDs
    "gemini-2.0-flash" to GeminiModels.GEMINI_3_8_FLASH,
    "gemini-2.0-flash-exp" to GeminiModels.GEMINI_3_8_FLASH,
    "gemini-2.0-flash-thinking-exp" to GeminiModels.GEMINI_3_1_PRO,
    "gemini-1.5-flash" to GeminiModels.GEMINI_2_5_FLASH,
    "gemini-1.5-pro" to GeminiModels.GEMINI_2_5_PRO,
    "gemini-3-flash" to GeminiModels.GEMINI_3_FLASH_PREVIEW,
    "gemini-3.1-pro" to GeminiModels.GEMINI_3_1_PRO,
    "deepseek-r1" to DeepSeekModels.DEEPSEEK_R1,
    "deepseek-v3" to DeepSeekModels.DEEPSEEK_V3,
    "meta-llama/llama-4-maverick:free" to OpenRouterModels.LLAMA_3_3_70B_FREE,
    "claude-3-5-sonnet" to AnthropicModels.CLAUDE_3_7_SONNET,
    "claude-3-5-haiku" to AnthropicModels.CLAUDE_HAIKU_4_5
)
