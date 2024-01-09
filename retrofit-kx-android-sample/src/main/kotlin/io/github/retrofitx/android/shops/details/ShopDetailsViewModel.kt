package io.github.retrofitx.android.shops.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.retrofitx.ShopService
import io.github.retrofitx.UnitResponse
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Shop
import io.github.retrofitx.android.shops.ShopsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class ShopDetailsViewModel(
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