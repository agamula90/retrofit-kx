package com.github.retrofitx.android.shops.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.retrofitx.ShopService
import com.github.retrofitx.UnitResponse
import com.github.retrofitx.android.NavigationDispatcher
import com.github.retrofitx.android.R
import com.github.retrofitx.android.dto.DefaultError
import com.github.retrofitx.android.dto.Shop
import com.github.retrofitx.android.shops.ShopsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopDetailsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val shopService: ShopService,
    private val navigationDispatcher: NavigationDispatcher
): ViewModel() {
    val events = Channel<ShopDetailsEvent>()
    val shop = handle.get<Shop>(SHOP)!!

    fun deleteShop() = viewModelScope.launch(Dispatchers.IO) {
        when(val response = shopService.deleteShop(shop.id)) {
            is UnitResponse.Success -> {
                navigationDispatcher.setNavigationResult(
                    backStackDestinationId = R.id.shops,
                    navigationKey = ShopsViewModel.RELOAD_SHOPS,
                    value = true
                )
                // comment to get api error message on subsequent shop deletion
                navigationDispatcher.navigateBack()
            }
            is UnitResponse.ConnectionError -> {
                events.send(ShopDetailsEvent.ShowConnectionErrorMessage)
            }
            is UnitResponse.ApiError -> {
                events.send(ShopDetailsEvent.ShowApiErrorMessage(response.cause))
            }
        }
    }

    // use this one if you don't care if remove succeeded or not
    fun deleteShopWithoutResult() = viewModelScope.launch(Dispatchers.IO) {
        shopService.deleteShopSafe(shop.id)
        navigationDispatcher.setNavigationResult(
            backStackDestinationId = R.id.shops,
            navigationKey = ShopsViewModel.RELOAD_SHOPS,
            value = true
        )
        navigationDispatcher.navigateBack()
    }

    companion object {
        const val SHOP = "shop"
    }
}