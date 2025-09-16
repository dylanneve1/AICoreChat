package org.dylanneve1.aicorechat.data

import android.app.Application
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dylanneve1.aicorechat.data.search.WebSearchService
import org.dylanneve1.aicorechat.data.context.PersonalContextBuilder
import org.dylanneve1.aicorechat.data.image.ImageDescriptionService
import android.graphics.ImageDecoder
import android.provider.MediaStore
import org.dylanneve1.aicorechat.data.prompt.PromptTemplates
import org.dylanneve1.aicorechat.data.MemoryRepository
import org.dylanneve1.aicorechat.data.MemoryEntry
import org.dylanneve1.aicorechat.data.CustomInstruction
import org.dylanneve1.aicorechat.data.BioInformation
import org.dylanneve1.aicorechat.data.MemoryCategory
import org.dylanneve1.aicorechat.data.MemoryImportance
import org.dylanneve1.aicorechat.data.mediapipe.LlmManager
import org.dylanneve1.aicorechat.data.model.ModelBackend
import org.dylanneve1.aicorechat.data.model.gemma1B_mediapipe
import org.dylanneve1.aicorechat.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * ChatViewModel orchestrates sessions, model streaming, tools, and persistence.
 * Heavily IO-bound work is delegated to small services.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val llmManager: LlmManager
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generativeModel: GenerativeModel? = null
    private var generationJob: Job? = null

    private val sharedPreferences =
        application.getSharedPreferences("AICoreChatPrefs", Context.MODE_PRIVATE)

    private val repository = ChatRepository(application.applicationContext)
    private val memoryRepository = MemoryRepository(application.applicationContext)

    // Services
    private val webSearchService = WebSearchService(application)
    private val personalContextBuilder = PersonalContextBuilder(application)
    private val imageDescriptionService = ImageDescriptionService(application)

    private fun deleteLegacyModelFiles(model: org.dylanneve1.aicorechat.data.model.Model) {
        val appContext = getApplication<Application>()
        val legacyLocations = listOf(appContext.filesDir, appContext.cacheDir)
        legacyLocations.forEach { dir ->
            dir?.let {
                val legacyFile = File(it, model.modelPath)
                if (legacyFile.exists()) {
                    val deleted = legacyFile.delete()
                    Log.d(TAG, "Deleted legacy model copy at ${legacyFile.absolutePath}: $deleted")
                }
            }
        }
    }

    companion object {
        private const val TAG = "ChatViewModel"
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_TOP_K = "top_k"
        const val KEY_USER_NAME = "user_name"
        const val KEY_PERSONAL_CONTEXT = "personal_context"
        const val KEY_WEB_SEARCH = "web_search"
        const val KEY_MULTIMODAL = "multimodal"
        const val KEY_MEMORY_CONTEXT = "memory_context"
        const val KEY_CUSTOM_INSTRUCTIONS = "custom_instructions_enabled"
        const val KEY_CUSTOM_INSTRUCTIONS_TEXT = "custom_instructions_text"
        const val KEY_BIO_CONTEXT = "bio_context"
    }

    private fun sanitizeAssistantText(text: String): String {
        return text
            .replace("[WEB_RESULTS]", "")
            .replace("[/WEB_RESULTS]", "")
            .replace("[SEARCH]", "")
            .replace("[/SEARCH]", "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun stripAssistantEndTokens(text: String): String {
        val token = "[/ASSISTANT]"
        var result = text.trimEnd()
        while (result.endsWith(token)) {
            result = result.dropLast(token.length).trimEnd()
        }
        return result
    }

    /**
     * Ensures the assistant response ends with the proper [/ASSISTANT] token to prevent infinite loops.
     * If the response doesn't end with [/ASSISTANT], appends it automatically.
     */
    private fun ensureAssistantEndToken(text: String): String {
        val trimmed = text.trim()

        // Check if response already ends with [/ASSISTANT]
        if (trimmed.endsWith("[/ASSISTANT]")) {
            return trimmed
        }

        // If it ends with any stop token, return as-is
        val stopTokens = listOf("[/ASSISTANT]", "[ASSISTANT]", "[/USER]", "[USER]")
        for (token in stopTokens) {
            if (trimmed.endsWith(token)) {
                return trimmed
            }
        }

        // If response is empty or only whitespace, return a minimal valid response
        if (trimmed.isEmpty()) {
            return "[/ASSISTANT]"
        }

        // Check if response contains [/ASSISTANT] anywhere and ends with another token
        if (trimmed.contains("[/ASSISTANT]")) {
            val lastAssistantIndex = trimmed.lastIndexOf("[/ASSISTANT]")
            val afterLastToken = trimmed.substring(lastAssistantIndex + "[/ASSISTANT]".length).trim()
            if (afterLastToken.isEmpty()) {
                return trimmed
            }
            // If there's content after the last [/ASSISTANT], it might be malformed
            // Return everything up to and including the last [/ASSISTANT]
            return trimmed.substring(0, lastAssistantIndex + "[/ASSISTANT]".length)
        }

        // If no [/ASSISTANT] token found, append it to prevent infinite loops
        Log.w(TAG, "Response missing [/ASSISTANT] token, appending automatically: ${trimmed.take(50)}...")
        return "$trimmed\n\n[/ASSISTANT]"
    }

    private fun finalizeAssistantDisplayText(rawText: String, fallback: String = ""): String {
        if (rawText.isBlank() && fallback.isBlank()) return ""
        val source = if (rawText.isBlank()) fallback else rawText
        val ensured = ensureAssistantEndToken(source)
        val withoutToken = stripAssistantEndTokens(ensured)
        return sanitizeAssistantText(withoutToken)
    }

    init {
        loadSettings()
        loadMemoryData()
        initOrStartNewSession()
        viewModelScope.launch {
            settingsRepository.appSettingsFlow.collect { settings ->
                val previousBackend = _uiState.value.selectedBackend
                _uiState.update {
                    it.copy(
                        selectedBackend = settings.selectedBackend,
                        huggingFaceToken = settings.huggingFaceToken
                    )
                }
                if (previousBackend != settings.selectedBackend) {
                    reinitializeModel()
                }
            }
        }
    }

    private fun loadSettings() {
        val temperature = sharedPreferences.getFloat(KEY_TEMPERATURE, 0.3f)
        val topK = sharedPreferences.getInt(KEY_TOP_K, 40)
        val userName = sharedPreferences.getString(KEY_USER_NAME, "") ?: ""
        val personalContextEnabled = sharedPreferences.getBoolean(KEY_PERSONAL_CONTEXT, false)
        val webSearchEnabled = sharedPreferences.getBoolean(KEY_WEB_SEARCH, false)
        val multimodalEnabled = sharedPreferences.getBoolean(KEY_MULTIMODAL, true)
        val memoryContextEnabled = sharedPreferences.getBoolean(KEY_MEMORY_CONTEXT, true)
        val customInstructionsEnabled = sharedPreferences.getBoolean(KEY_CUSTOM_INSTRUCTIONS, true)
        val customInstructionsText = sharedPreferences.getString(KEY_CUSTOM_INSTRUCTIONS_TEXT, "") ?: ""
        val bioContextEnabled = sharedPreferences.getBoolean(KEY_BIO_CONTEXT, true)

        _uiState.update {
            it.copy(
                temperature = temperature,
                topK = topK,
                userName = userName,
                personalContextEnabled = personalContextEnabled,
                webSearchEnabled = webSearchEnabled,
                multimodalEnabled = multimodalEnabled,
                memoryContextEnabled = memoryContextEnabled,
                customInstructionsEnabled = customInstructionsEnabled,
                customInstructions = customInstructionsText,
                bioContextEnabled = bioContextEnabled
            )
        }
    }

    fun updateUserName(name: String) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply()
        _uiState.update { it.copy(userName = name) }
    }

    fun updatePersonalContextEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PERSONAL_CONTEXT, enabled).apply()
        _uiState.update { it.copy(personalContextEnabled = enabled) }
    }

    fun updateWebSearchEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_WEB_SEARCH, enabled).apply()
        _uiState.update { it.copy(webSearchEnabled = enabled) }
    }

    fun updateMultimodalEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_MULTIMODAL, enabled).apply()
        _uiState.update { it.copy(multimodalEnabled = enabled) }
        if (!enabled) {
            _uiState.update { it.copy(pendingImageUri = null, pendingImageDescription = null, isDescribingImage = false) }
        }
    }

    fun updateSelectedBackend(backend: ModelBackend) {
        viewModelScope.launch {
            try {
                settingsRepository.updateSelectedBackend(backend)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update backend", e)
                _uiState.update { it.copy(modelError = "Failed to switch model: ${e.message}") }
            }
        }
    }

    fun updateHuggingFaceToken(token: String) {
        viewModelScope.launch {
            try {
                settingsRepository.updateHuggingFaceToken(token)
                _uiState.update { it.copy(huggingFaceToken = token) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update Hugging Face token", e)
                _uiState.update { it.copy(modelError = "Failed to save token: ${e.message}") }
            }
        }
    }

    fun downloadGemmaModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    gemmaDownloadStatus = ModelDownloadStatus.DOWNLOADING,
                    gemmaDownloadProgress = 0f,
                    modelError = null
                )
            }
            val model = gemma1B_mediapipe
            val url = URL(model.url)
            var connection: HttpURLConnection? = null
            try {
                deleteLegacyModelFiles(model)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "AICoreChat/${BuildConfig.VERSION_NAME}")
                    val token = _uiState.value.huggingFaceToken.ifBlank { BuildConfig.HUGGING_FACE_TOKEN }
                    if (token.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorHeader = connection.getHeaderField("x-error-message")
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }?.takeIf { it.isNotBlank() }
                    val errorMessage = errorHeader ?: errorBody ?: connection.responseMessage ?: "Unknown error"
                    throw IOException("HTTP $responseCode: $errorMessage")
                }

                val fileLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) connection.contentLengthLong else connection.contentLength.toLong()
                val outputFile = File(model.getPath(getApplication()))
                outputFile.parentFile?.mkdirs()

                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val data = ByteArray(4096)
                        var total = 0L
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0) {
                                val progress = (total.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                                _uiState.update { it.copy(gemmaDownloadProgress = progress) }
                            }
                            output.write(data, 0, count)
                        }
                    }
                }

                _uiState.update { it.copy(gemmaDownloadStatus = ModelDownloadStatus.DOWNLOADED, gemmaDownloadProgress = 1f) }
                deleteLegacyModelFiles(model)
                reinitializeModel()
            } catch (e: Exception) {
                Log.e(TAG, "Gemma download failed", e)
                _uiState.update {
                    it.copy(
                        gemmaDownloadStatus = ModelDownloadStatus.FAILED,
                        modelError = "Gemma download failed: ${e.message ?: "Unknown error"}"
                    )
                }
                val partialFile = File(model.getPath(getApplication()))
                if (partialFile.exists()) {
                    partialFile.delete()
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun loadMemoryData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isMemoryLoading = true, memoryError = null) }
                val customInstructions = sharedPreferences.getString(KEY_CUSTOM_INSTRUCTIONS_TEXT, "") ?: ""
                val memoryEntries = memoryRepository.loadMemoryEntries()
                val bioInformation = memoryRepository.loadBioInformation()

                _uiState.update {
                    it.copy(
                        customInstructions = customInstructions,
                        memoryEntries = memoryEntries,
                        bioInformation = bioInformation,
                        isMemoryLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isMemoryLoading = false,
                        memoryError = "Failed to load memory data: ${e.message}"
                    )
                }
            }
        }
    }

    // Memory Context Settings
    fun updateMemoryContextEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_MEMORY_CONTEXT, enabled).apply()
        _uiState.update { it.copy(memoryContextEnabled = enabled) }
    }

    fun updateCustomInstructionsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_CUSTOM_INSTRUCTIONS, enabled).apply()
        _uiState.update { it.copy(customInstructionsEnabled = enabled) }
    }

    fun updateBioContextEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIO_CONTEXT, enabled).apply()
        _uiState.update { it.copy(bioContextEnabled = enabled) }
    }

    fun updateBioInformation(name: String, age: String, occupation: String, location: String) {
        val bio = if (name.isNotBlank() || age.isNotBlank() || occupation.isNotBlank() || location.isNotBlank()) {
            BioInformation(
                id = "user_bio",
                name = name.takeIf { it.isNotBlank() },
                age = age.toIntOrNull(),
                occupation = occupation.takeIf { it.isNotBlank() },
                location = location.takeIf { it.isNotBlank() }
            )
        } else null

        bio?.let { memoryRepository.saveBioInformation(it) }
        _uiState.update { it.copy(bioInformation = bio) }
    }

    fun updateCustomInstructions(instructions: String, enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_CUSTOM_INSTRUCTIONS, enabled).apply()
        sharedPreferences.edit().putString(KEY_CUSTOM_INSTRUCTIONS_TEXT, instructions).apply()
        _uiState.update {
            it.copy(
                customInstructionsEnabled = enabled,
                customInstructions = instructions
            )
        }
    }

    fun clearPendingImage() {
        _uiState.update { it.copy(pendingImageUri = null, pendingImageDescription = null, isDescribingImage = false) }
    }

    fun onImageSelected(uri: Uri) {
        if (!_uiState.value.multimodalEnabled) {
            _uiState.update { it.copy(modelError = "Multimodal is disabled in Settings") }
            return
        }
        _uiState.update { it.copy(pendingImageUri = uri.toString(), isDescribingImage = true, modelError = null) }
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val ctx = getApplication<Application>().applicationContext
                    if (Build.VERSION.SDK_INT >= 28) {
                        val src = ImageDecoder.createSource(ctx.contentResolver, uri)
                        ImageDecoder.decodeBitmap(src)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(ctx.contentResolver, uri)
                    }
                }
                val desc = imageDescriptionService.describe(bitmap).trim()
                if (desc.isBlank()) {
                    // Detach image on failure/blank
                    _uiState.update { it.copy(isDescribingImage = false, pendingImageDescription = null, pendingImageUri = null, modelError = "Could not generate image description") }
                } else {
                    _uiState.update { it.copy(isDescribingImage = false, pendingImageDescription = desc) }
                }
            } catch (e: Exception) {
                // Detach on error
                _uiState.update { it.copy(isDescribingImage = false, pendingImageDescription = null, pendingImageUri = null, modelError = e.message ?: "Failed to describe image") }
            }
        }
    }

    private fun getBatteryPercent(): Int? {
        val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 1..100) level else null
    }

    private fun getNetworkSummary(): String {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            if (network == null) "Offline/unknown" else {
                val caps = cm.getNetworkCapabilities(network)
                val parts = mutableListOf<String>()
                if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) parts.add("Wi‑Fi")
                if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) parts.add("Cellular")
                if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) parts.add("Ethernet")
                if (parts.isEmpty()) "Unknown" else parts.joinToString(", ")
            }
        } catch (e: Exception) { "Unknown" }
    }

    private suspend fun getLastKnownLocation(): Location? {
        return try {
            val ctx = getApplication<Application>().applicationContext
            val fused = LocationServices.getFusedLocationProviderClient(ctx)
            val hasFine = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) return null
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                fused.lastLocation.addOnSuccessListener { cont.resume(it, onCancellation = null) }
                    .addOnFailureListener { cont.resume(null, onCancellation = null) }
            }
        } catch (e: Exception) { null }
    }

    private fun formatLatLon(loc: Location?): String {
        return if (loc == null) "(not granted)" else "${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}"
    }

    private suspend fun buildPersonalContext(): String {
        val now = java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm z", java.util.Locale.getDefault()).format(java.util.Date())
        val device = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        val locale = java.util.Locale.getDefault().toString()
        val timeZone = java.util.TimeZone.getDefault().id
        val namePart = if (_uiState.value.userName.isNotBlank()) "User Name: ${_uiState.value.userName}\n" else ""
        val battery = getBatteryPercent()?.let { "Battery: ${it}%\n" } ?: ""
        val storageStat = try {
            val stat = android.os.StatFs(Environment.getDataDirectory().path)
            val free = stat.availableBytes / (1024 * 1024)
            val total = stat.totalBytes / (1024 * 1024)
            "Storage: ${free}MB free / ${total}MB total\n"
        } catch (e: Exception) { "" }
        val appVersion = try {
            val pm = getApplication<Application>().packageManager
            val pInfo = pm.getPackageInfo(getApplication<Application>().packageName, 0)
            "App Version: ${pInfo.versionName} (${pInfo.longVersionCode})\n"
        } catch (e: Exception) { "" }
        val network = "Network: ${getNetworkSummary()}\n"
        val location = "Location: ${formatLatLon(getLastKnownLocation())}\n"
        return "[PERSONAL_CONTEXT]\n${namePart}Current Time: ${now}\nDevice: ${device}\nLocale: ${locale}\nTime Zone: ${timeZone}\n${battery}${network}${storageStat}${appVersion}${location}[/PERSONAL_CONTEXT]\n\n"
    }

    private suspend fun prependPersonalContextIfNeeded(promptBuilder: StringBuilder) {
        if (_uiState.value.personalContextEnabled) {
            promptBuilder.append(personalContextBuilder.build(_uiState.value.userName))
        }
    }

    private fun initOrStartNewSession() {
        // Purge all empty chats on app open
        runCatching {
            val existing = repository.loadSessions()
            existing.filter { s -> s.messages.none { it.isFromUser } }
                .forEach { s -> repository.deleteSession(s.id) }
        }
        val sessionsAfterPurge = repository.loadSessions()
        val session = repository.createNewSession()
        _uiState.update {
            it.copy(
                sessions = sessionsAfterPurge.map { s -> ChatSessionMeta(s.id, s.name) }.let { existing ->
                    listOf(ChatSessionMeta(session.id, session.name)) + existing
                },
                currentSessionId = session.id,
                currentSessionName = session.name,
                messages = emptyList()
            )
        }
    }

    private fun reinitializeModel() {
        viewModelScope.launch {
            val backend = _uiState.value.selectedBackend
            _uiState.update { it.copy(isModelSwitching = true, modelError = "Initializing ${backend.displayName}…") }
            try {
                generativeModel?.close()
                generativeModel = null
                llmManager.close()

                when (backend) {
                    ModelBackend.AICORE_GEMINI_NANO -> {
                        val config = generationConfig {
                            context = getApplication<Application>().applicationContext
                            temperature = _uiState.value.temperature
                            topK = _uiState.value.topK
                        }

                        generativeModel = GenerativeModel(generationConfig = config)
                        generativeModel?.prepareInferenceEngine()
                        _uiState.update { it.copy(modelError = null) }
                    }
                    ModelBackend.MEDIAPIPE_GEMMA_1B -> {
                        val modelFile = File(gemma1B_mediapipe.getPath(getApplication()))
                        if (modelFile.exists()) {
                            _uiState.update { it.copy(gemmaDownloadStatus = ModelDownloadStatus.DOWNLOADED) }
                            val error = llmManager.initialize(
                                gemma1B_mediapipe,
                                _uiState.value.temperature,
                                _uiState.value.topK
                            )
                            if (error.isEmpty()) {
                                _uiState.update { it.copy(modelError = null) }
                            } else {
                                _uiState.update { it.copy(modelError = "Gemma init failed: $error") }
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    gemmaDownloadStatus = ModelDownloadStatus.NOT_DOWNLOADED,
                                    modelError = "Gemma 1B model not downloaded."
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing model", e)
                _uiState.update { it.copy(modelError = "Model initialization failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isModelSwitching = false) }
            }
        }
    }

    fun newChat() {
        val session = repository.createNewSession()
        val allSessions = repository.loadSessions()
        _uiState.update {
            it.copy(
                sessions = allSessions.map { s -> ChatSessionMeta(s.id, s.name) },
                currentSessionId = session.id,
                currentSessionName = session.name,
                messages = emptyList()
            )
        }
    }

    fun selectChat(sessionId: Long) {
        val prevId = _uiState.value.currentSessionId
        val prevMessages = _uiState.value.messages
        val sessions = repository.loadSessions()
        val selected = sessions.find { it.id == sessionId } ?: return
        _uiState.update {
            it.copy(
                currentSessionId = selected.id,
                currentSessionName = selected.name,
                messages = selected.messages.toList()
            )
        }
        if (prevId != null && prevId != sessionId && prevMessages.none { it.isFromUser }) {
            repository.deleteSession(prevId)
            val refreshed = repository.loadSessions()
            _uiState.update { state ->
                state.copy(sessions = refreshed.map { s -> ChatSessionMeta(s.id, s.name) })
            }
        }
    }

    fun renameCurrentChat(newName: String) {
        val sessionId = _uiState.value.currentSessionId ?: return
        repository.renameSession(sessionId, newName)
        val updatedSessions = _uiState.value.sessions.map {
            if (it.id == sessionId) it.copy(name = newName) else it
        }
        _uiState.update { it.copy(currentSessionName = newName, sessions = updatedSessions) }
    }

    fun renameChat(sessionId: Long, newName: String) {
        repository.renameSession(sessionId, newName)
        val updatedSessions = _uiState.value.sessions.map {
            if (it.id == sessionId) it.copy(name = newName) else it
        }
        _uiState.update { state ->
            val currentName = if (state.currentSessionId == sessionId) newName else state.currentSessionName
            state.copy(currentSessionName = currentName, sessions = updatedSessions)
        }
    }

    fun deleteChat(sessionId: Long) {
        repository.deleteSession(sessionId)
        val remaining = repository.loadSessions()
        if (remaining.isEmpty()) {
            newChat()
        } else {
            val next = remaining.first()
            _uiState.update {
                it.copy(
                    sessions = remaining.map { s -> ChatSessionMeta(s.id, s.name) },
                    currentSessionId = next.id,
                    currentSessionName = next.name,
                    messages = next.messages.toList()
                )
            }
        }
    }

    fun wipeAllChats() {
        repository.wipeAllSessions()
        newChat()
    }

    fun purgeEmptyChats() {
        val currentId = _uiState.value.currentSessionId
        val sessions = repository.loadSessions()
        var changed = false
        sessions.forEach { s ->
            if (s.id != currentId && s.messages.none { it.isFromUser }) {
                repository.deleteSession(s.id)
                changed = true
            }
        }
        if (changed) {
            val remaining = repository.loadSessions()
            _uiState.update {
                it.copy(
                    sessions = remaining.map { s -> ChatSessionMeta(s.id, s.name) }
                )
            }
        }
    }

    fun generateTitlesForAllChats() {
        if (_uiState.value.isGenerating) {
            _uiState.update { it.copy(modelError = "Please wait for current generation to finish.") }
            return
        }
        val model = generativeModel ?: run {
            _uiState.update { it.copy(modelError = "Model not ready.") }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isBulkTitleGenerating = true, modelError = null) }
                val sessions = repository.loadSessions()
                val currentId = _uiState.value.currentSessionId
                for (s in sessions) {
                    if (s.id != currentId && s.messages.none { it.isFromUser }) {
                        repository.deleteSession(s.id)
                        continue
                    }
                    if (s.messages.none { it.isFromUser }) continue
                    if (s.name != "New Chat") continue
                    val sb = StringBuilder()
                    sb.append("You are to summarize the following chat into a very short, descriptive title.\n")
                    sb.append("Rules: 3-4 words max, no quotes, no punctuation, Title Case, be specific.\n\n")
                    s.messages.forEach { m ->
                        if (m.isFromUser) sb.append("User: ${m.text}\n") else sb.append("Assistant: ${m.text}\n")
                    }
                    sb.append("\nReturn only the title.")
                    val prompt = sb.toString()
                    var result = ""
                    model.generateContentStream(prompt)
                        .onStart { }
                        .catch { e -> Log.e(TAG, "Title gen error for session ${s.id}", e) }
                        .collect { chunk -> result += chunk.text }
                    val cleaned = result
                        .replace("\n", " ")
                        .replace("\"", "")
                        .trim()
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(4)
                        .joinToString(" ")
                        .replace(Regex("[.,!?:;]+$"), "")
                    if (cleaned.isNotBlank()) {
                        repository.renameSession(s.id, cleaned)
                    }
                }
                val refreshed = repository.loadSessions()
                _uiState.update { state ->
                    val currentName = refreshed.find { it.id == state.currentSessionId }?.name ?: state.currentSessionName
                    state.copy(
                        isBulkTitleGenerating = false,
                        sessions = refreshed.map { ChatSessionMeta(it.id, it.name) },
                        currentSessionName = currentName
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBulkTitleGenerating = false, modelError = e.message) }
            }
        }
    }

    fun updateTemperature(temperature: Float) {
        sharedPreferences.edit().putFloat(KEY_TEMPERATURE, temperature).apply()
        _uiState.update { it.copy(temperature = temperature) }
        reinitializeModel()
    }

    fun updateTopK(topK: Int) {
        sharedPreferences.edit().putInt(KEY_TOP_K, topK).apply()
        _uiState.update { it.copy(topK = topK) }
        reinitializeModel()
    }

    fun resetModelSettings() {
        val defaultTemperature = 0.3f
        val defaultTopK = 40
        sharedPreferences.edit()
            .putFloat(KEY_TEMPERATURE, defaultTemperature)
            .putInt(KEY_TOP_K, defaultTopK)
            .apply()
        _uiState.update { it.copy(temperature = defaultTemperature, topK = defaultTopK) }
        reinitializeModel()
    }

    fun clearChat() {
        _uiState.value.currentSessionId?.let { repository.deleteSession(it) }
        newChat()
    }

    fun stopGeneration() {
        generationJob?.cancel()
        if (_uiState.value.selectedBackend == ModelBackend.MEDIAPIPE_GEMMA_1B) {
            llmManager.stopGeneration()
        }
        _uiState.update { current ->
            val updated = current.messages.toMutableList()
            if (updated.isNotEmpty() && updated.last().isStreaming) {
                updated[updated.lastIndex] = updated.last().copy(isStreaming = false)
            }
            current.copy(isGenerating = false, messages = updated)
        }
    }

    fun generateChatTitle() {
        if (_uiState.value.isGenerating) {
            _uiState.update { it.copy(modelError = "Cannot rename while generating.") }
            return
        }
        val sessionId = _uiState.value.currentSessionId
        if (sessionId == null || _uiState.value.messages.none { it.isFromUser }) {
            _uiState.update { it.copy(modelError = "Chat is empty.") }
            return
        }
        val model = generativeModel
        if (model == null) {
            _uiState.update { it.copy(modelError = "Model not ready.") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isTitleGenerating = true, modelError = null) }
                val history = _uiState.value.messages
                val sb = StringBuilder()
                sb.append("You are to summarize the following chat into a very short, descriptive title.\n")
                sb.append("Rules: 3-4 words max, no quotes, no punctuation, Title Case, be specific.\n\n")
                history.forEach { m ->
                    if (m.isFromUser) sb.append("User: ${m.text}\n") else sb.append("Assistant: ${m.text}\n")
                }
                sb.append("\nReturn only the title.")
                val prompt = sb.toString()

                var result = ""
                model.generateContentStream(prompt)
                    .onStart { /* no-op */ }
                    .onCompletion {
                        val cleaned = result
                            .replace("\n", " ")
                            .replace("\"", "")
                            .trim()
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(4)
                            .joinToString(" ")
                            .replace(Regex("[.,!?:;]+$"), "")
                        if (cleaned.isNotBlank()) {
                            renameCurrentChat(cleaned)
                        }
                        _uiState.update { it.copy(isTitleGenerating = false) }
                    }
                    .catch { e ->
                        Log.e(TAG, "Error generating title", e)
                        _uiState.update { it.copy(isTitleGenerating = false, modelError = e.message) }
                    }
                    .collect { chunk ->
                        result += chunk.text
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isTitleGenerating = false, modelError = e.message) }
            }
        }
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                 caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) { false }
    }

    fun onImagePicked(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDescribingImage = true, modelError = null) }
            try {
                val desc = imageDescriptionService.describe(bitmap).trim()
                if (desc.isNotBlank()) {
                    _uiState.update { it.copy(isDescribingImage = false, pendingImageDescription = desc) }
                } else {
                    _uiState.update { it.copy(isDescribingImage = false, modelError = "Could not describe image.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDescribingImage = false, modelError = e.message) }
            }
        }
    }

    fun sendMessage(prompt: String) {
        if (_uiState.value.selectedBackend == ModelBackend.MEDIAPIPE_GEMMA_1B) {
            sendMessageMediaPipe(prompt)
        } else {
            sendMessageAICore(prompt)
        }
    }

    private fun sendMessageMediaPipe(prompt: String) {
        generationJob?.cancel()

        if (!llmManager.isInitialized) {
            _uiState.update { it.copy(modelError = "Gemma model is not initialized yet.") }
            return
        }

        val userMessage = ChatMessage(text = prompt, isFromUser = true)
        _uiState.update { it.copy(messages = it.messages + userMessage) }
        _uiState.value.currentSessionId?.let { repository.appendMessage(it, userMessage) }

        generationJob = viewModelScope.launch {
            var fullResponse = ""
            val assistantMessageId = System.nanoTime()
            val assistantMessage = ChatMessage(id = assistantMessageId, text = "", isFromUser = false, isStreaming = true)
            _uiState.update { it.copy(isGenerating = true, messages = it.messages + assistantMessage) }

            val promptBuilder = StringBuilder()
            promptBuilder.append("<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n")

            llmManager.generateResponse(promptBuilder.toString()) { partialResult, done ->
                fullResponse += partialResult
                _uiState.update { current ->
                    val updatedMessages = current.messages.map {
                        if (it.id == assistantMessageId) it.copy(text = fullResponse) else it
                    }
                    current.copy(messages = updatedMessages)
                }

                if (done) {
                    _uiState.update { current ->
                        val finalMessages = current.messages.map {
                            if (it.id == assistantMessageId) it.copy(isStreaming = false) else it
                        }
                        current.copy(isGenerating = false, messages = finalMessages)
                    }
                    _uiState.value.currentSessionId?.let { sid -> repository.replaceMessages(sid, _uiState.value.messages) }
                }
            }
        }
    }

    private fun sendMessageAICore(prompt: String) {
        generationJob?.cancel()

        if (generativeModel == null) {
            _uiState.update { it.copy(modelError = "Model is not initialized yet.") }
            return
        }

        val currentPendingUri = _uiState.value.pendingImageUri
        val currentPendingDesc = _uiState.value.pendingImageDescription
        val userMessage = ChatMessage(text = prompt, isFromUser = true, imageUri = currentPendingUri, imageDescription = currentPendingDesc)
        _uiState.update { it.copy(messages = it.messages + userMessage, pendingImageUri = null, pendingImageDescription = null) }
        _uiState.value.currentSessionId?.let { repository.appendMessage(it, userMessage) }

        generationJob = viewModelScope.launch {
            try {
                val allowSearchThisTurn = _uiState.value.webSearchEnabled && webSearchService.isOnline()
                val offlineNotice = _uiState.value.webSearchEnabled && !allowSearchThisTurn
                val promptBuilder = StringBuilder()
                promptBuilder.append(PromptTemplates.systemPreamble(allowSearch = allowSearchThisTurn, offlineNotice = offlineNotice))

                // Add custom instructions if enabled
                if (_uiState.value.customInstructionsEnabled && _uiState.value.customInstructions.isNotBlank()) {
                    promptBuilder.append(PromptTemplates.customInstructionsBlock(_uiState.value.customInstructions))
                }

                promptBuilder.append(PromptTemplates.fewShotGeneral())
                if (allowSearchThisTurn) {
                    promptBuilder.append(PromptTemplates.fewShotSearch())
                }
                prependPersonalContextIfNeeded(promptBuilder)

                // Add memory context if enabled
                if (_uiState.value.memoryContextEnabled) {
                    val relevantMemories = PromptTemplates.buildMemoryContextFromQuery(
                        query = prompt,
                        allMemories = _uiState.value.memoryEntries
                    )

                    val bioInfo = if (_uiState.value.bioContextEnabled) _uiState.value.bioInformation else null
                    promptBuilder.append(PromptTemplates.memoryContextBlock(relevantMemories, bioInfo))
                }

                // Include prior image descriptions as context blocks
                if (_uiState.value.multimodalEnabled) {
                    _uiState.value.messages.forEach { msg ->
                        if (!msg.imageDescription.isNullOrBlank()) {
                            promptBuilder.append("[IMAGE_DESCRIPTION]\n${msg.imageDescription}\n[/IMAGE_DESCRIPTION]\n\n")
                        }
                    }
                }

                val history = _uiState.value.messages.takeLast(10)
                history.forEach { message ->
                    if (message.id == userMessage.id) return@forEach
                    if (message.isFromUser) promptBuilder.append("[USER]\n${message.text}\n[/USER]\n")
                    else promptBuilder.append("[ASSISTANT]\n${message.text}\n[/ASSISTANT]\n")
                }
                promptBuilder.append("[USER]\n$prompt\n[/USER]\n")
                promptBuilder.append("[ASSISTANT]\n")

                val fullPrompt = promptBuilder.toString()
                Log.d(TAG, "Sending prompt:\n$fullPrompt")

                var fullResponse = ""
                val stopTokens = listOf("[/ASSISTANT]", "[ASSISTANT]", "[/USER]", "[USER]")
                var searchTriggered = false
                var searchStartDetected = false
                var searchStartIndex = -1
                val streamStartMs = SystemClock.elapsedRealtime()

                generativeModel!!
                    .generateContentStream(fullPrompt)
                    .onStart {
                        _uiState.update {
                            it.copy(
                                isGenerating = true,
                                messages = it.messages + ChatMessage(text = "", isFromUser = false, isStreaming = true)
                            )
                        }
                    }
                    .onCompletion {
                        if (searchStartDetected && !searchTriggered) {
                            _uiState.update { it.copy(isSearchInProgress = false, currentSearchQuery = null) }
                        }
                        if (!searchTriggered) {
                            _uiState.update { currentState ->
                                val updated = currentState.messages.toMutableList()
                                if (updated.isNotEmpty() && updated.last().isStreaming) {
                                    val finalText = finalizeAssistantDisplayText(fullResponse, updated.last().text)
                                    updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                                }
                                currentState.copy(isGenerating = false, messages = updated)
                            }
                            _uiState.value.currentSessionId?.let { sid -> repository.replaceMessages(sid, _uiState.value.messages) }
                        }
                    }
                    .catch { e ->
                        Log.e(TAG, "Error generating content", e)
                        _uiState.update { currentState ->
                            val updated = currentState.messages.toMutableList()
                            if (updated.isNotEmpty() && updated.last().isStreaming) {
                                val errorText = "Error: ${e.message}"
                                val finalText = finalizeAssistantDisplayText(errorText, updated.last().text)
                                updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                            }
                            currentState.copy(isGenerating = false, messages = updated)
                        }
                        _uiState.value.currentSessionId?.let { sid -> repository.replaceMessages(sid, _uiState.value.messages) }
                    }
                    .collect { chunk ->
                        fullResponse += chunk.text

                        if (allowSearchThisTurn) {
                            val firstNonWs = fullResponse.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) Int.MAX_VALUE else it }
                            val searchToken = "[SEARCH]"
                            if (!searchStartDetected && firstNonWs != Int.MAX_VALUE) {
                                val after = fullResponse.substring(firstNonWs)
                                if (after.isNotEmpty() && after.length < searchToken.length && searchToken.startsWith(after)) {
                                    searchStartDetected = true
                                    searchStartIndex = firstNonWs
                                    _uiState.update { current ->
                                        val updated = current.messages.toMutableList()
                                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                                            updated[updated.lastIndex] = updated.last().copy(text = "")
                                        }
                                        current.copy(isSearchInProgress = true, currentSearchQuery = "", messages = updated)
                                    }
                                    return@collect
                                }
                            }
                            val startIdx = fullResponse.indexOf(searchToken)
                            if (!searchStartDetected && startIdx >= 0 && startIdx == firstNonWs) {
                                searchStartDetected = true
                                searchStartIndex = startIdx
                                val partialQuery = if (fullResponse.length >= startIdx + searchToken.length) {
                                    fullResponse.substring(startIdx + searchToken.length).substringBefore("[/SEARCH]").trim()
                                } else ""
                                _uiState.update { current ->
                                    val updated = current.messages.toMutableList()
                                    if (updated.isNotEmpty() && updated.last().isStreaming) {
                                        updated[updated.lastIndex] = updated.last().copy(text = "")
                                    }
                                    current.copy(isSearchInProgress = true, currentSearchQuery = partialQuery, messages = updated)
                                }
                            }
                            if (searchStartDetected) {
                                val tokenEndBase = searchStartIndex + searchToken.length
                                val partialQuery = if (fullResponse.length > tokenEndBase) fullResponse.substring(tokenEndBase).substringBefore("[/SEARCH]").trim() else ""
                                _uiState.update { it.copy(currentSearchQuery = partialQuery) }
                                val endIdx = if (fullResponse.length > tokenEndBase) fullResponse.indexOf("[/SEARCH]", tokenEndBase) else -1
                                if (endIdx != -1 && !searchTriggered) {
                                    searchTriggered = true
                                    val query = fullResponse.substring(tokenEndBase, endIdx).trim()
                                    viewModelScope.launch { continueWithSearchResultsUsingExistingBubble(userMessage, query) }
                                    generationJob?.cancel()
                                    return@collect
                                }
                                return@collect
                            }
                        }

                        val elapsed = SystemClock.elapsedRealtime() - streamStartMs
                        if (!searchStartDetected && elapsed < 300 && fullResponse.length < 24) {
                            return@collect
                        }

                        val earliestIndex = stopTokens
                            .map { token -> fullResponse.indexOf(token).let { idx -> if (idx >= 0) idx else Int.MAX_VALUE } }
                            .minOrNull() ?: Int.MAX_VALUE
                        delay(35)
                        _uiState.update { currentState ->
                            val updated = currentState.messages.toMutableList()
                            if (updated.isNotEmpty() && updated.last().isStreaming) {
                                val displayTextRaw = if (earliestIndex != Int.MAX_VALUE) fullResponse.substring(0, earliestIndex) else {
                                    val holdBack = findPartialStopSuffixLength(fullResponse, stopTokens)
                                    if (holdBack > 0 && fullResponse.length > holdBack) fullResponse.dropLast(holdBack) else fullResponse
                                }
                                val displayText = sanitizeAssistantText(displayTextRaw)
                                updated[updated.lastIndex] = updated.last().copy(text = displayText)
                            }
                            currentState.copy(messages = updated)
                        }
                        if (earliestIndex != Int.MAX_VALUE) {
                            generationJob?.cancel()
                        }
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "Job cancelled as expected.")
                } else {
                    Log.e(TAG, "Exception during generation", e)
                    _uiState.update { it.copy(isGenerating = false, messages = it.messages + ChatMessage(text = "Error: ${e.message}", isFromUser = false)) }
                    _uiState.value.currentSessionId?.let { sid -> repository.replaceMessages(sid, _uiState.value.messages) }
                }
            }
        }
    }

    private suspend fun continueWithSearchResultsUsingExistingBubble(userMessage: ChatMessage, query: String) {
        try {
            val results = webSearchService.search(query)

            val promptBuilder = StringBuilder()
            promptBuilder.append(PromptTemplates.postSearchPreamble())

            // Add custom instructions if enabled
            if (_uiState.value.customInstructionsEnabled && _uiState.value.customInstructions.isNotBlank()) {
                promptBuilder.append(PromptTemplates.customInstructionsBlock(_uiState.value.customInstructions))
            }

            promptBuilder.append("[WEB_RESULTS]\n${results}\n[/WEB_RESULTS]\n\n")

            // Add memory context if enabled
            if (_uiState.value.memoryContextEnabled) {
                val relevantMemories = PromptTemplates.buildMemoryContextFromQuery(
                    query = userMessage.text,
                    allMemories = _uiState.value.memoryEntries
                )

                val bioInfo = if (_uiState.value.bioContextEnabled) _uiState.value.bioInformation else null
                promptBuilder.append(PromptTemplates.memoryContextBlock(relevantMemories, bioInfo))
            }
            // Build recent history WITHOUT any trailing streaming placeholder bubble (we never persisted it)
            val recent = _uiState.value.messages.filter { !it.isStreaming }
            (recent + userMessage).takeLast(10).forEach { m ->
                if (m.isFromUser) promptBuilder.append("[USER]\n${m.text}\n[/USER]\n")
                else promptBuilder.append("[ASSISTANT]\n${m.text}\n[/ASSISTANT]\n")
            }
            promptBuilder.append("[ASSISTANT]\n")

            val fullPrompt = promptBuilder.toString()
                Log.d(TAG, "Follow-up with web results (reuse bubble):\n$fullPrompt")

            var fullResponse = ""
            val stopTokens = listOf("[/ASSISTANT]", "[ASSISTANT]", "[/USER]", "[USER]")
            val streamStartMs = SystemClock.elapsedRealtime()

            generativeModel!!.generateContentStream(fullPrompt)
                .onStart {
                    // Hide searching flag and reuse the same streaming bubble
                    _uiState.update { it.copy(isSearchInProgress = false, currentSearchQuery = null, isGenerating = true) }
                }
                .onCompletion {
                    _uiState.update { current ->
                        val updated = current.messages.toMutableList()
                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                            val finalText = finalizeAssistantDisplayText(fullResponse, updated.last().text)
                            updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                        }
                        current.copy(isGenerating = false, messages = updated)
                    }
                    _uiState.value.currentSessionId?.let { sid -> repository.replaceMessages(sid, _uiState.value.messages) }
                }
                .catch { e ->
                    Log.e(TAG, "Error generating after search", e)
                    _uiState.update { current ->
                        val updated = current.messages.toMutableList()
                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                            val errorText = "Error: ${e.message}"
                            val finalText = finalizeAssistantDisplayText(errorText, updated.last().text)
                            updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                        }
                        current.copy(isGenerating = false, messages = updated)
                    }
                }
                .collect { chunk ->
                    fullResponse += chunk.text
                    // Initial suppression window for follow-up, too
                    val elapsed = SystemClock.elapsedRealtime() - streamStartMs
                    if (elapsed < 300 && fullResponse.length < 24) {
                        return@collect
                    }
                    val earliestIndex = stopTokens
                        .map { token -> fullResponse.indexOf(token).let { idx -> if (idx >= 0) idx else Int.MAX_VALUE } }
                        .minOrNull() ?: Int.MAX_VALUE
                    delay(35)
                    _uiState.update { current ->
                        val updated = current.messages.toMutableList()
                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                            val displayTextRaw = if (earliestIndex != Int.MAX_VALUE) fullResponse.substring(0, earliestIndex) else {
                                val holdBack = findPartialStopSuffixLength(fullResponse, stopTokens)
                                if (holdBack > 0 && fullResponse.length > holdBack) fullResponse.dropLast(holdBack) else fullResponse
                            }
                            val displayText = sanitizeAssistantText(displayTextRaw)
                            updated[updated.lastIndex] = updated.last().copy(text = displayText)
                        }
                        current.copy(messages = updated)
                    }
                    if (earliestIndex != Int.MAX_VALUE) {
                        generationJob?.cancel()
                    }
                }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSearchInProgress = false, currentSearchQuery = null, modelError = e.message) }
        }
    }

    private fun findPartialStopSuffixLength(text: String, tokens: List<String>): Int {
        var maxLen = 0
        tokens.forEach { token ->
            val maxCheck = minOf(token.length - 1, text.length)
            for (k in maxCheck downTo 1) {
                if (text.endsWith(token.substring(0, k))) {
                    if (k > maxLen) maxLen = k
                    break
                }
            }
        }
        return maxLen
    }

    // Custom Instructions Management
    fun addCustomInstruction(title: String, instruction: String, category: String = "General") {
        viewModelScope.launch {
            try {
                val newInstruction = CustomInstruction(
                    title = title,
                    instruction = instruction,
                    category = category
                )
                memoryRepository.addCustomInstruction(newInstruction)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to add custom instruction: ${e.message}") }
            }
        }
    }

    fun updateCustomInstruction(instruction: CustomInstruction) {
        viewModelScope.launch {
            try {
                memoryRepository.updateCustomInstruction(instruction)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to update custom instruction: ${e.message}") }
            }
        }
    }

    fun deleteCustomInstruction(instructionId: String) {
        viewModelScope.launch {
            try {
                memoryRepository.deleteCustomInstruction(instructionId)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to delete custom instruction: ${e.message}") }
            }
        }
    }

    fun toggleCustomInstruction(instructionId: String) {
        viewModelScope.launch {
            try {
                memoryRepository.toggleCustomInstruction(instructionId)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to toggle custom instruction: ${e.message}") }
            }
        }
    }

    // Memory Entries Management
    fun addMemoryEntry(content: String) {
        viewModelScope.launch {
            try {
                val newMemory = MemoryEntry(
                    content = content
                )
                memoryRepository.addMemoryEntry(newMemory)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to add memory entry: ${e.message}") }
            }
        }
    }

    fun updateMemoryEntry(memory: MemoryEntry) {
        viewModelScope.launch {
            try {
                memoryRepository.updateMemoryEntry(memory)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to update memory entry: ${e.message}") }
            }
        }
    }

    fun deleteMemoryEntry(memoryId: String) {
        viewModelScope.launch {
            try {
                memoryRepository.deleteMemoryEntry(memoryId)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to delete memory entry: ${e.message}") }
            }
        }
    }

    fun toggleMemoryEntry(memoryId: String) {
        viewModelScope.launch {
            try {
                memoryRepository.toggleMemoryEntry(memoryId)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to toggle memory entry: ${e.message}") }
            }
        }
    }

    fun updateMemoryLastAccessed(memoryId: String) {
        viewModelScope.launch {
            try {
                memoryRepository.updateMemoryLastAccessed(memoryId)
                // Don't reload all data, just update the specific memory
                val updatedMemories = _uiState.value.memoryEntries.map { memory ->
                    if (memory.id == memoryId) {
                        memory.copy(lastAccessed = System.currentTimeMillis())
                    } else memory
                }
                _uiState.update { it.copy(memoryEntries = updatedMemories) }
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to update memory access time: ${e.message}") }
            }
        }
    }

    // Bio Information Management
    fun saveBioInformation(bio: BioInformation) {
        viewModelScope.launch {
            try {
                memoryRepository.saveBioInformation(bio)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to save bio information: ${e.message}") }
            }
        }
    }

    fun deleteBioInformation() {
        viewModelScope.launch {
            try {
                memoryRepository.deleteBioInformation()
                _uiState.update { it.copy(bioInformation = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to delete bio information: ${e.message}") }
            }
        }
    }

    // Search and Filter Methods
    fun searchMemoryEntries(query: String) {
        viewModelScope.launch {
            try {
                val results = memoryRepository.searchMemoryEntries(query)
                _uiState.update { it.copy(memoryEntries = results, memorySearchQuery = query) }
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to search memories: ${e.message}") }
            }
        }
    }

    fun clearMemorySearch() {
        loadMemoryData()
        _uiState.update { it.copy(memorySearchQuery = "", selectedMemoryCategory = null) }
    }


    // Data Import/Export
    fun exportAllMemoryData(): String? {
        return try {
            memoryRepository.exportAllData()
        } catch (e: Exception) {
            _uiState.update { it.copy(memoryError = "Failed to export data: ${e.message}") }
            null
        }
    }

    fun importMemoryData(jsonData: String) {
        viewModelScope.launch {
            try {
                val result = memoryRepository.importData(jsonData)
                when (result) {
                    is MemoryRepository.ImportResult.Success -> {
                        loadMemoryData()
                        _uiState.update { it.copy(memoryError = null) }
                    }
                    is MemoryRepository.ImportResult.Error -> {
                        _uiState.update { it.copy(memoryError = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to import data: ${e.message}") }
            }
        }
    }

    // Utility Methods
    fun clearMemoryError() {
        _uiState.update { it.copy(memoryError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        generativeModel?.close()
        imageDescriptionService.close()
        llmManager.close()
    }
}
