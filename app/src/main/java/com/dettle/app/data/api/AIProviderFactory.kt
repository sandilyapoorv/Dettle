package com.dettle.app.data.api

import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.domain.model.AIModel
import com.dettle.app.domain.model.AIProviderType
import com.dettle.app.domain.model.FreeModels
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates AIProvider instances from model definitions.
 * Reads API keys from the encrypted ApiKeyStore.
 */
@Singleton
class AIProviderFactory @Inject constructor(
    private val keyStore: ApiKeyStore,
    private val client: OkHttpClient,
    private val json: Json
) {
    fun create(model: AIModel): AIProvider? {
        if (model.provider == AIProviderType.LOCAL_OLLAMA) {
            return OllamaProvider(model, client, json)
        }

        val apiKey = keyStore.getKey(model.provider)
            ?.trim()
            ?.replace("\r", "")
            ?.replace("\n", "")
            ?.takeIf { it.isNotBlank() } ?: return null

        return when (model.provider) {
            AIProviderType.GROQ -> OpenAICompatProvider(
                model = model,
                apiKey = apiKey,
                client = client,
                json = json,
                extraHeaders = mapOf(
                    "X-Groq-Organization" to "dettle"
                )
            )

            AIProviderType.GEMINI -> GeminiProvider(
                model = model,
                apiKey = apiKey,
                client = client,
                json = json
            )

            AIProviderType.OPENROUTER -> OpenAICompatProvider(
                model = model,
                apiKey = apiKey,
                client = client,
                json = json,
                extraHeaders = mapOf(
                    "HTTP-Referer" to "https://dettle.app",
                    "X-Title" to "Dettle"
                )
            )

            AIProviderType.SAMBANOVA -> OpenAICompatProvider(
                model = model,
                apiKey = apiKey,
                client = client,
                json = json
            )

            AIProviderType.GITHUB_MODELS -> OpenAICompatProvider(
                model = model,
                apiKey = apiKey,
                client = client,
                json = json,
                extraHeaders = mapOf(
                    "api-version" to "2024-12-01-preview"
                )
            )

            else -> null  // WebView providers handled separately
        }
    }

    /** Groq 8B specifically — used for the IntentClassifier (fast + cheap) */
    fun createGroq8B(): AIProvider? = create(FreeModels.GROQ_LLAMA_8B)

    /** Gemini Flash — used for heavy coding tasks */
    fun createGeminiFlash(): AIProvider? = create(FreeModels.GEMINI_FLASH)
}
