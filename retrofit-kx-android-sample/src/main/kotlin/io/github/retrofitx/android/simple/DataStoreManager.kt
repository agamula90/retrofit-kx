package io.github.retrofitx.android.simple

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.retrofitx.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("RetrofitKxAppSample") }
    )

    fun getBaseUrl(): Flow<String> {
        return dataStore.data.map { it[BASE_URL] ?: BuildConfig.BASE_URL }
    }

    suspend fun setBaseUrl(baseUrl: String) {
        val oldBaseUrl = getBaseUrl().first()
        if (oldBaseUrl != baseUrl) {
            dataStore.edit { it[BASE_URL] = baseUrl }
        }
    }

    companion object {
        private val BASE_URL = stringPreferencesKey("baseUrl")
    }
}