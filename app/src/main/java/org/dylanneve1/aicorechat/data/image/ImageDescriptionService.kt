package org.dylanneve1.aicorechat.data.image

import android.app.Application
import android.graphics.Bitmap
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.imagedescription.ImageDescriber
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import com.google.mlkit.genai.imagedescription.ImageDescription
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * ImageDescriptionService wraps ML Kit GenAI Image Description API to produce
 * a concise description for a Bitmap.
 */
class ImageDescriptionService(app: Application) {
    private val imageDescriber: ImageDescriber = ImageDescription.getClient(
        ImageDescriberOptions.builder(app).build(),
    )

    suspend fun describe(bitmap: Bitmap): String {
        ensureFeatureReady()
        val request = ImageDescriptionRequest.builder(bitmap).build()
        val buffer = StringBuilder()
        imageDescriber.runInference(request) { outputText ->
            buffer.append(outputText)
        }.await()
        return buffer.toString().trim()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun ensureFeatureReady() {
        val status = try {
            imageDescriber.checkFeatureStatus().await()
        } catch (e: Exception) {
            FeatureStatus.UNAVAILABLE
        }
        when (status) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> suspendCancellableCoroutine { cont ->
                imageDescriber.downloadFeature(object : DownloadCallback {
                    override fun onDownloadStarted(bytesToDownload: Long) { }
                    override fun onDownloadProgress(bytesDownloaded: Long) { }
                    override fun onDownloadCompleted() {
                        if (cont.isActive) cont.resume(Unit, onCancellation = null)
                    }
                    override fun onDownloadFailed(exception: GenAiException) {
                        if (cont.isActive) cont.cancel(exception)
                    }
                })
            }
            FeatureStatus.UNAVAILABLE -> error("Image description feature unavailable on this device")
        }
    }

    fun close() {
        imageDescriber.close()
    }
}
