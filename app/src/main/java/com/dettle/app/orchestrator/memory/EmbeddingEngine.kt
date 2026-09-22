package com.dettle.app.orchestrator.memory

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class EmbeddingEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EmbeddingEngine"
        private const val MODEL_NAME = "universal_sentence_encoder.tflite"
    }

    private var textEmbedder: TextEmbedder? = null
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_NAME)
                .build()

            val options = TextEmbedder.TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .build()

            textEmbedder = TextEmbedder.createFromOptions(context, options)
            isInitialized = true
            Log.d(TAG, "Successfully initialized MediaPipe TextEmbedder")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextEmbedder. Ensure $MODEL_NAME is in src/main/assets/", e)
        }
    }

    suspend fun generateEmbedding(text: String): FloatArray? = withContext(Dispatchers.Default) {
        if (!isInitialized) initialize()
        if (textEmbedder == null) return@withContext null

        try {
            val result = textEmbedder?.embed(text)
            val embedding = result?.embeddingResult()?.embeddings()?.firstOrNull()
            return@withContext embedding?.floatEmbedding()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embedding: ${e.message}")
            return@withContext null
        }
    }

    suspend fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float = withContext(Dispatchers.Default) {
        if (v1.size != v2.size) return@withContext 0f
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        if (normA == 0f || normB == 0f) return@withContext 0f
        return@withContext (dotProduct / (sqrt(normA.toDouble()) * sqrt(normB.toDouble()))).toFloat()
    }
}
