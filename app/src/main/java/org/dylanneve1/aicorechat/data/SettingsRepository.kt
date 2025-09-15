package org.dylanneve1.aicorechat.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.map
import org.dylanneve1.aicorechat.data.model.ModelBackend
import org.dylanneve1.aicorechat.proto.AppSettings
import javax.inject.Inject

class SettingsRepository @Inject constructor(private val settingsDataStore: DataStore<AppSettings>) {
    val appSettingsFlow = settingsDataStore.data
        .map { settings ->
            val backendId = if (settings.selectedModelBackendId.isNullOrEmpty()) {
                ModelBackend.AICORE_GEMINI_NANO.id
            } else {
                settings.selectedModelBackendId
            }
            Pair(
                ModelBackend.entries.firstOrNull { it.id == backendId } ?: ModelBackend.AICORE_GEMINI_NANO
            )
        }

    suspend fun updateSelectedBackend(backend: ModelBackend) {
        settingsDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setSelectedModelBackendId(backend.id)
                .build()
        }
    }
}
