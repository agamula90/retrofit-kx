package io.github.retrofitx.android.shops

import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.retrofitx.DataResponse
import io.github.retrofitx.ParseFailureException
import io.github.retrofitx.ShopService
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Shop
import io.github.retrofitx.android.shops.details.ShopDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ShopsViewModel(
    handle: SavedStateHandle,
    private val navigationDispatcher: NavigationDispatcher,
    private val shopService: ShopService
): ViewModel() {
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
        try {
            isParseFailed.postValue(false)
            when(val response = shopService.getShops()) {
                is DataResponse.Success -> shops.postValue(response.data)
                is DataResponse.ApiError -> {
                    events.send(ShopEvent.ShowApiErrorMessage(response.cause))
                }
                is DataResponse.ConnectionError -> {
                    events.send(ShopEvent.ShowConnectionErrorMessage)
                }
            }
        } catch (e: ParseFailureException) {
            shops.postValue(emptyList())
            isParseFailed.postValue(true)
        }
    }

    fun goShopDetails(shop: Shop) {
        navigationDispatcher.navigate(R.id.goShopDetails, bundleOf(ShopDetailsViewModel.SHOP to shop))
    }

    companion object {
        const val RELOAD_SHOPS = "reloadShops"
    }
}