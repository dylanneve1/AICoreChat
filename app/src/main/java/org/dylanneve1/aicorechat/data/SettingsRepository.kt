package org.dylanneve1.aicorechat.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.map
import org.dylanneve1.aicorechat.data.model.ModelBackend
import javax.inject.Inject

data class UserSettings(
    val selectedBackend: ModelBackend,
    val huggingFaceToken: String
)

class SettingsRepository @Inject constructor(
    private val settingsDataStore: DataStore<Preferences>
) {
    private val backendKey = stringPreferencesKey("selected_model_backend_id")
    private val huggingFaceTokenKey = stringPreferencesKey("hugging_face_token")

    val appSettingsFlow = settingsDataStore.data
        .map { preferences ->
            val backendId = preferences[backendKey].orEmpty().ifBlank { ModelBackend.AICORE_GEMINI_NANO.id }
            val backend = ModelBackend.entries.firstOrNull { it.id == backendId } ?: ModelBackend.AICORE_GEMINI_NANO
            val token = preferences[huggingFaceTokenKey].orEmpty()
            UserSettings(selectedBackend = backend, huggingFaceToken = token)
        }

    suspend fun updateSelectedBackend(backend: ModelBackend) {
        settingsDataStore.edit { preferences ->
            preferences[backendKey] = backend.id
        }
    }

    suspend fun updateHuggingFaceToken(token: String) {
        settingsDataStore.edit { preferences ->
            if (token.isBlank()) {
                preferences.remove(huggingFaceTokenKey)
            } else {
                preferences[huggingFaceTokenKey] = token
            }
        }
    }
}
