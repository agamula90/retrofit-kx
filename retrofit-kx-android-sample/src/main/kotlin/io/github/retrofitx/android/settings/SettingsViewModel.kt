package io.github.retrofitx.android.settings

import androidx.lifecycle.*
import io.github.retrofitx.android.simple.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val dataStoreManager: DataStoreManager,
    handle: SavedStateHandle
): ViewModel() {
    private val isProcessDeathRestoration = handle.keys().isNotEmpty()
    private val currentBaseUrl = dataStoreManager.getBaseUrl()
    val baseUrl = handle.getLiveData<String>("new_base_url")
    val isBaseUrlChanged = baseUrl.switchMap {
        liveData {
            emit(currentBaseUrl.first() != it)
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isProcessDeathRestoration) {
                baseUrl.postValue(currentBaseUrl.first())
            }
        }
    }

    fun confirmBaseUrlChange() {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setBaseUrl(baseUrl.value!!)
        }
    }

    fun setBaseUrl(newBaseUrl: String) {
        baseUrl.value = newBaseUrl
    }
}