package com.dettle.app.domain.model

/** Unified provider identity */
enum class AIProviderType(
    val displayName: String,
    val baseUrl: String,
    val isWebView: Boolean = false,
    val isFree: Boolean = true
) {
    // ── Track A: Free official APIs ───────────────────────────────────────
    GROQ("Groq", "https://api.groq.com/openai/v1"),
    GEMINI("Google AI Studio", "https://generativelanguage.googleapis.com/v1beta"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1"),
    SAMBANOVA("SambaNova", "https://api.sambanova.ai/v1"),
    GITHUB_MODELS("GitHub Models", "https://models.inference.ai.azure.com"),
    LOCAL_OLLAMA("Ollama (Local/Custom)", "http://10.0.2.2:11434/api"),

    // ── Track B: WebView bridge (user's own subscriptions) ────────────────
    CHATGPT_WEB("ChatGPT", "https://chatgpt.com", isWebView = true),
    CLAUDE_WEB("Claude", "https://claude.ai", isWebView = true),
    DEEPSEEK_WEB("DeepSeek", "https://chat.deepseek.com", isWebView = true),
    GROK_WEB("Grok", "https://grok.com", isWebView = true),
    GEMINI_WEB("Gemini Web", "https://gemini.google.com", isWebView = true),
    KIMI_WEB("Kimi", "https://kimi.ai", isWebView = true),
    MISTRAL_WEB("Mistral Chat", "https://chat.mistral.ai", isWebView = true),
    QWEN_WEB("Qwen", "https://chat.qwenlm.ai", isWebView = true);

    companion object {
        fun fromId(id: String) = values().firstOrNull { it.name == id }
    }
}

/** A single AI model available from a provider */
data class AIModel(
    val provider: AIProviderType,
    val modelId: String,          // e.g. "llama-3.1-8b-instant"
    val displayName: String,
    val contextWindow: Int,       // tokens
    val dailyTokenLimit: Int,     // -1 = unlimited/unknown
    val dailyRequestLimit: Int,   // -1 = unlimited/unknown
    val rpmLimit: Int,            // requests per minute
    val isFree: Boolean = true,
    val supportsToolCalling: Boolean = true,
    val bestFor: String = ""      // human-readable description of ideal use case
)

/** All free models available in the system */
object FreeModels {

    // Groq — ultra-fast, great for routing decisions
    val GROQ_LLAMA_8B = AIModel(
        provider = AIProviderType.GROQ,
        modelId = "llama-3.1-8b-instant",
        displayName = "Llama 3.1 8B (Groq)",
        contextWindow = 128_000,
        dailyTokenLimit = 500_000,
        dailyRequestLimit = 14_400,
        rpmLimit = 30,
        bestFor = "Fast routing decisions, agent inner loops, intent classification"
    )

    val GROQ_LLAMA_70B = AIModel(
        provider = AIProviderType.GROQ,
        modelId = "llama-3.3-70b-versatile",
        displayName = "Llama 3.3 70B (Groq)",
        contextWindow = 128_000,
        dailyTokenLimit = 100_000,
        dailyRequestLimit = 1_000,
        rpmLimit = 30,
        bestFor = "Complex reasoning, orchestrator decisions"
    )

    // Uncensored / Local Models (Ollama Network)
    val OLLAMA_DOLPHIN_UNCENSORED = AIModel(
        provider = AIProviderType.LOCAL_OLLAMA,
        modelId = "dolphin-mixtral",
        displayName = "Dolphin Mixtral (Uncensored Network)",
        contextWindow = 32_000,
        dailyTokenLimit = -1,
        dailyRequestLimit = -1,
        rpmLimit = -1,
        bestFor = "Raw execution without any safety guardrails or apologies."
    )

    // Note: We intentionally removed ON_DEVICE local models to protect the Moto Edge's 
    // battery and thermal limits. All heavy compute is offloaded to APIs.

    // Google AI Studio — best overall free tier
    val GEMINI_FLASH = AIModel(
        provider = AIProviderType.GEMINI,
        modelId = "gemini-2.0-flash",
        displayName = "Gemini 2.0 Flash",
        contextWindow = 1_000_000,
        dailyTokenLimit = -1,
        dailyRequestLimit = 1_500,
        rpmLimit = 15,
        bestFor = "Heavy coding tasks, large codebase reading, prompt caching"
    )

    val GEMINI_FLASH_THINKING = AIModel(
        provider = AIProviderType.GEMINI,
        modelId = "gemini-2.0-flash-thinking-exp",
        displayName = "Gemini 2.0 Flash Thinking",
        contextWindow = 1_000_000,
        dailyTokenLimit = -1,
        dailyRequestLimit = 500,
        rpmLimit = 10,
        bestFor = "Deep reasoning, complex debugging, code review"
    )

    // OpenRouter free models
    val OPENROUTER_LLAMA = AIModel(
        provider = AIProviderType.OPENROUTER,
        modelId = "meta-llama/llama-4-maverick:free",
        displayName = "Llama 4 Maverick (OpenRouter Free)",
        contextWindow = 128_000,
        dailyTokenLimit = -1,
        dailyRequestLimit = 1_000,
        rpmLimit = 20,
        bestFor = "General fallback when Groq is rate-limited"
    )

    val OPENROUTER_QWEN = AIModel(
        provider = AIProviderType.OPENROUTER,
        modelId = "qwen/qwen-2.5-72b-instruct:free",
        displayName = "Qwen 2.5 72B (OpenRouter Free)",
        contextWindow = 128_000,
        dailyTokenLimit = -1,
        dailyRequestLimit = 1_000,
        rpmLimit = 20,
        bestFor = "Code generation, Chinese-language tasks"
    )

    // SambaNova — great for massive context
    val SAMBANOVA_LLAMA_70B = AIModel(
        provider = AIProviderType.SAMBANOVA,
        modelId = "Meta-Llama-3.3-70B-Instruct",
        displayName = "Llama 3.3 70B (SambaNova)",
        contextWindow = 128_000,
        dailyTokenLimit = 200_000,
        dailyRequestLimit = -1,
        rpmLimit = 60,
        bestFor = "Massive codebase reads, large context windows"
    )

    // GitHub Models — best quality, most restricted
    val GITHUB_GPT4O = AIModel(
        provider = AIProviderType.GITHUB_MODELS,
        modelId = "gpt-4o",
        displayName = "GPT-4o (GitHub Models)",
        contextWindow = 128_000,
        dailyTokenLimit = 200_000,
        dailyRequestLimit = 50,
        rpmLimit = 10,
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
        bestFor = "Complex reasoning, planning, structured decomposition"
    )

    /** Default waterfall order — most generous/fastest first */
    val WATERFALL_ORDER: List<AIModel> = listOf(
        GROQ_LLAMA_8B,       // Primary: fast routing
        GEMINI_FLASH,        // Heavy lifting
        SAMBANOVA_LLAMA_70B, // Large context reads
        GROQ_LLAMA_70B,      // Complex reasoning
        OPENROUTER_LLAMA,    // Fallback
        OPENROUTER_QWEN,     // Fallback 2
        GITHUB_GPT4O,        // Best quality (save for last)
        GITHUB_DEEPSEEK_R1   // Best reasoning (save for last)
    )
}

/**
 * Curated model sets per use-case.
 * Each [AgentMode] picks a set — [KeyPoolManager] tries them in order, failing over as needed.
 */
object ModelSets {
    /** Intent classification, inner loop decisions, fast turns */
    val FAST: List<AIModel> = listOf(
        FreeModels.GEMINI_FLASH, // Replaced local model with Gemini
        FreeModels.GROQ_LLAMA_8B,
        FreeModels.OPENROUTER_LLAMA,
        FreeModels.GROQ_LLAMA_70B
    )

    /** Reading large codebases, web pages, long documents */
    val LARGE_CONTEXT: List<AIModel> = listOf(
        FreeModels.GEMINI_FLASH,        // 1M context
        FreeModels.SAMBANOVA_LLAMA_70B, // 128k
        FreeModels.OPENROUTER_LLAMA
    )

    /** Structured planning, deep reasoning, architectural decisions */
    val DEEP_REASONING: List<AIModel> = listOf(
        FreeModels.GEMINI_FLASH_THINKING, // Using Cloud reasoning to save battery
        FreeModels.GITHUB_DEEPSEEK_R1,
        FreeModels.GROQ_LLAMA_70B
    )

    /** Highest quality code generation — used sparingly (tight daily limits) */
    val BEST_QUALITY: List<AIModel> = listOf(
        FreeModels.GITHUB_GPT4O,
        FreeModels.GEMINI_FLASH_THINKING,
        FreeModels.GEMINI_FLASH
    )

    /** Code review — reasoning + reading ability combined */
    val CODE_REVIEW: List<AIModel> = listOf(
        FreeModels.GEMINI_FLASH_THINKING,
        FreeModels.GITHUB_DEEPSEEK_R1,
        FreeModels.GEMINI_FLASH
    )

    /** Full waterfall — used by Goal mode (needs everything) */
    val ALL: List<AIModel> = FreeModels.WATERFALL_ORDER
}

/** All known models by ID — used for resolving waterfall model IDs back to AIModel objects */
val ALL_KNOWN_MODELS: Map<String, AIModel> = listOf(
    FreeModels.GROQ_LLAMA_8B,
    FreeModels.GROQ_LLAMA_70B,
    FreeModels.GEMINI_FLASH,
    FreeModels.GEMINI_FLASH_THINKING,
    FreeModels.OPENROUTER_LLAMA,
    FreeModels.OPENROUTER_QWEN,
    FreeModels.SAMBANOVA_LLAMA_70B,
    FreeModels.GITHUB_GPT4O,
    FreeModels.GITHUB_DEEPSEEK_R1,
    FreeModels.OLLAMA_DOLPHIN_UNCENSORED
).associateBy { it.modelId }
