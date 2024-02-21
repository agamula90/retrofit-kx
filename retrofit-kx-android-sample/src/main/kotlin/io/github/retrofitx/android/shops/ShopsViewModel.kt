package io.github.retrofitx.android.shops

import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.retrofit.serialization.deserializeErrorBody
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Shop
import io.github.retrofitx.android.shops.details.ShopDetailsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.retrofitx.android.dto.DefaultError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val navigationDispatcher: NavigationDispatcher,
    private val shopService: io.github.retrofitx.android.remote.ShopService
) : ViewModel() {
    val events = Channel<ShopEvent>()
    val shops = handle.getLiveData<List<Shop>>("shops", emptyList())
    val isParseFailed = handle.getLiveData("isParseFailed", false)

    init {
        if (handle.get<List<Shop>>("shops").isNullOrEmpty()) {
            loadShops()
        }

        navigationDispatcher.observeNavigationResult(
            navigationKey = RELOAD_SHOPS,
            initialValue = false,
            coroutineScope = viewModelScope
        ) { reloadShops -> if (reloadShops) loadShops() }
    }

    fun loadShops() = viewModelScope.launch(Dispatchers.IO) {
        isParseFailed.postValue(false)
        when (val response = shopService.getShops()) {
            is ApiResponse.Success -> shops.postValue(response.data)
            is ApiResponse.Failure.Error -> {
                val error = response.deserializeErrorBody<Any, DefaultError>()!!
                events.send(ShopEvent.ShowApiErrorMessage(error))
            }

            is ApiResponse.Failure.Exception -> {
                events.send(ShopEvent.ShowConnectionErrorMessage)
            }
        }
    }

    fun goShopDetails(shop: Shop) {
        navigationDispatcher.navigate(
            R.id.goShopDetails,
            bundleOf(ShopDetailsViewModel.SHOP to shop)
        )
    }

    companion object {
        const val RELOAD_SHOPS = "reloadShops"
    }
}