package org.dylanneve1.aicorechat.data.image

import android.app.Application
import android.graphics.Bitmap

/**
 * ImageDescriptionService placeholder. Replace with ML Kit GenAI Image Description when available.
 */
class ImageDescriptionService(app: Application) {
    suspend fun describe(bitmap: Bitmap): String {
        // TODO: Integrate ML Kit GenAI Image Description API when available on the device/CI
        return "An image provided by the user."
    }
    fun close() { /* no-op */ }
} 