package org.dylanneve1.aicorechat.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.dylanneve1.aicorechat.data.SettingsRepository
import org.dylanneve1.aicorechat.data.SettingsSerializer
import org.dylanneve1.aicorechat.proto.AppSettings
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<AppSettings> {
        return DataStoreFactory.create(
            serializer = SettingsSerializer,
            produceFile = { context.dataStoreFile("app_settings.pb") }
        )
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<AppSettings>): SettingsRepository {
        return SettingsRepository(dataStore)
    }
}
