package org.dylanneve1.aicorechat.data.mediapipe

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import org.dylanneve1.aicorechat.data.model.Model
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LlmManager"

@Singleton
class LlmManager @Inject constructor(@ApplicationContext private val context: Context) {
    private var llmInference: LlmInference? = null
    private var session: LlmInferenceSession? = null
    var isInitialized = false
        private set

    fun initialize(model: Model, temperature: Float, topK: Int): String {
        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(model.getPath(context))
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            session = LlmInferenceSession.createFromOptions(
                llmInference!!,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(topK)
                    .setTemperature(temperature)
                    .build()
            )
            isInitialized = true
            ""
        } catch (e: Exception) {
            isInitialized = false
            Log.e(TAG, "Failed to initialize LlmInference", e)
            e.message ?: "Unknown MediaPipe error"
        }
    }

    fun generateResponse(prompt: String, resultListener: (partialResult: String, done: Boolean) -> Unit) {
        val currentSession = session
        if (llmInference == null || currentSession == null || !isInitialized) {
            resultListener("MediaPipe model is not initialized.", true)
            return
        }

        try {
            currentSession.addQueryChunk(prompt)
            currentSession.generateResponseAsync { chunk, done ->
                resultListener(chunk ?: "", done)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during generateResponseAsync", e)
            resultListener("Error: ${e.message}", true)
        }
    }

    fun stopGeneration() {
        session?.cancelGenerateResponseAsync()
    }

    fun close() {
        session?.close()
        session = null
        llmInference?.close()
        llmInference = null
        isInitialized = false
    }
}
