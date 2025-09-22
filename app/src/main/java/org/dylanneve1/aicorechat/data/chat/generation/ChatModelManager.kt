package org.dylanneve1.aicorechat.data.chat.generation

import android.app.Application
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig

class ChatModelManager(private val application: Application) {

    private var generativeModel: GenerativeModel? = null

    suspend fun reinitialize(temperature: Float, topK: Int): Result<Unit> = runCatching {
        generativeModel?.close()
        val config = generationConfig {
            context = application.applicationContext
            this.temperature = temperature
            this.topK = topK
        }
        generativeModel = GenerativeModel(generationConfig = config).also { it.prepareInferenceEngine() }
    }

    fun getModel(): GenerativeModel? = generativeModel

    fun close() {
        generativeModel?.close()
        generativeModel = null
    }
}
