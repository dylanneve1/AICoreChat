package org.dylanneve1.aicorechat.data.model

import android.content.Context
import java.io.File

enum class ModelBackend(val id: String, val displayName: String, val isLocal: Boolean) {
    AICORE_GEMINI_NANO("aicore_gemini_nano", "Gemini Nano (AICore)", true),
    MEDIAPIPE_GEMMA_1B("mediapipe_gemma_1b", "Gemma 1B (MediaPipe)", false)
}

data class Model(
    val backend: ModelBackend,
    val modelPath: String, // local file name for mediapipe
    val url: String, // for download
    val sizeInBytes: Long,
) {
    fun getPath(context: Context): String {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(baseDir, modelPath).absolutePath
    }
}

val gemma1B_mediapipe = Model(
    backend = ModelBackend.MEDIAPIPE_GEMMA_1B,
    modelPath = "gemma1b_mediapipe.task",
    url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task?download=true",
    sizeInBytes = 554661246L
)

val availableModels = listOf(
    Model(ModelBackend.AICORE_GEMINI_NANO, "", "", 0),
    gemma1B_mediapipe
)
