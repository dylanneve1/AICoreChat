package org.dylanneve1.aicorechat.data.chat.media

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.dylanneve1.aicorechat.data.chat.model.ChatUiState
import org.dylanneve1.aicorechat.data.image.ImageDescriptionService

class ChatMediaHandler(
    private val application: Application,
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<ChatUiState>,
    private val imageDescriptionService: ImageDescriptionService,
) {

    fun clearPendingImage() {
        state.update {
            it.copy(
                pendingImageUri = null,
                pendingImageDescription = null,
                isDescribingImage = false,
            )
        }
    }

    fun onImageSelected(uri: Uri) {
        if (!state.value.multimodalEnabled) {
            state.update { it.copy(modelError = "Multimodal is disabled in Settings") }
            return
        }
        state.update {
            it.copy(
                pendingImageUri = uri.toString(),
                isDescribingImage = true,
                modelError = null,
            )
        }
        scope.launch {
            try {
                val description = withContext(Dispatchers.IO) {
                    val ctx = application.applicationContext
                    val source = ImageDecoder.createSource(ctx.contentResolver, uri)
                    val bitmap = ImageDecoder.decodeBitmap(source)
                    imageDescriptionService.describe(bitmap).trim()
                }
                if (description.isBlank()) {
                    state.update {
                        it.copy(
                            isDescribingImage = false,
                            pendingImageDescription = null,
                            pendingImageUri = null,
                            modelError = "Could not generate image description",
                        )
                    }
                } else {
                    state.update { it.copy(isDescribingImage = false, pendingImageDescription = description) }
                }
            } catch (throwable: Exception) {
                state.update {
                    it.copy(
                        isDescribingImage = false,
                        pendingImageDescription = null,
                        pendingImageUri = null,
                        modelError = throwable.message ?: "Failed to describe image",
                    )
                }
            }
        }
    }

    fun onImagePicked(bitmap: Bitmap) {
        scope.launch {
            state.update { it.copy(isDescribingImage = true, modelError = null) }
            try {
                val description = imageDescriptionService.describe(bitmap).trim()
                if (description.isNotBlank()) {
                    state.update { it.copy(isDescribingImage = false, pendingImageDescription = description) }
                } else {
                    state.update { it.copy(isDescribingImage = false, modelError = "Could not describe image.") }
                }
            } catch (throwable: Exception) {
                state.update { it.copy(isDescribingImage = false, modelError = throwable.message) }
            }
        }
    }

    fun close() {
        imageDescriptionService.close()
    }
}
