package org.dylanneve1.aicorechat.data.chat.settings

import android.content.SharedPreferences
import androidx.core.content.edit

data class ChatPreferences(
    val temperature: Float,
    val topK: Int,
    val userName: String,
    val personalContextEnabled: Boolean,
    val webSearchEnabled: Boolean,
    val multimodalEnabled: Boolean,
    val memoryContextEnabled: Boolean,
    val customInstructionsEnabled: Boolean,
    val customInstructions: String,
    val bioContextEnabled: Boolean,
)

class ChatSettingsManager(private val preferences: SharedPreferences) {

    fun load(): ChatPreferences = ChatPreferences(
        temperature = preferences.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE),
        topK = preferences.getInt(KEY_TOP_K, DEFAULT_TOP_K),
        userName = preferences.getString(KEY_USER_NAME, "") ?: "",
        personalContextEnabled = preferences.getBoolean(KEY_PERSONAL_CONTEXT, false),
        webSearchEnabled = preferences.getBoolean(KEY_WEB_SEARCH, false),
        multimodalEnabled = preferences.getBoolean(KEY_MULTIMODAL, true),
        memoryContextEnabled = preferences.getBoolean(KEY_MEMORY_CONTEXT, true),
        customInstructionsEnabled = preferences.getBoolean(KEY_CUSTOM_INSTRUCTIONS, true),
        customInstructions = preferences.getString(KEY_CUSTOM_INSTRUCTIONS_TEXT, "") ?: "",
        bioContextEnabled = preferences.getBoolean(KEY_BIO_CONTEXT, true),
    )

    fun setUserName(name: String) {
        preferences.edit { putString(KEY_USER_NAME, name) }
    }

    fun setPersonalContextEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_PERSONAL_CONTEXT, enabled) }
    }

    fun setWebSearchEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_WEB_SEARCH, enabled) }
    }

    fun setMultimodalEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_MULTIMODAL, enabled) }
    }

    fun setMemoryContextEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_MEMORY_CONTEXT, enabled) }
    }

    fun setCustomInstructionsEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_CUSTOM_INSTRUCTIONS, enabled) }
    }

    fun setCustomInstructions(text: String) {
        preferences.edit { putString(KEY_CUSTOM_INSTRUCTIONS_TEXT, text) }
    }

    fun setBioContextEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_BIO_CONTEXT, enabled) }
    }

    fun setTemperature(value: Float) {
        preferences.edit { putFloat(KEY_TEMPERATURE, value) }
    }

    fun setTopK(value: Int) {
        preferences.edit { putInt(KEY_TOP_K, value) }
    }

    fun resetModelDefaults() {
        preferences.edit {
            putFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
            putInt(KEY_TOP_K, DEFAULT_TOP_K)
        }
    }

    companion object {
        private const val DEFAULT_TEMPERATURE = 0.3f
        private const val DEFAULT_TOP_K = 40

        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_TOP_K = "top_k"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_PERSONAL_CONTEXT = "personal_context"
        private const val KEY_WEB_SEARCH = "web_search"
        private const val KEY_MULTIMODAL = "multimodal"
        private const val KEY_MEMORY_CONTEXT = "memory_context"
        private const val KEY_CUSTOM_INSTRUCTIONS = "custom_instructions_enabled"
        private const val KEY_CUSTOM_INSTRUCTIONS_TEXT = "custom_instructions_text"
        private const val KEY_BIO_CONTEXT = "bio_context"

        fun defaultTemperature() = DEFAULT_TEMPERATURE
        fun defaultTopK() = DEFAULT_TOP_K
    }
}
