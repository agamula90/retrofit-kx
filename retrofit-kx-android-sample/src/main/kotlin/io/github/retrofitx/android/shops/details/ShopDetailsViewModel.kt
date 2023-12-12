package io.github.retrofitx.android.shops.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import io.github.retrofitx.UnitResponse
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Shop
import io.github.retrofitx.android.shops.ShopsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.retrofitx.android.dto.DefaultError
import io.github.retrofitx.android.inject.DeferredValue
import io.github.retrofitx.android.remote.ShopService
import io.github.retrofitx.internal.invokeUnitFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopDetailsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val shopService: DeferredValue<ShopService>,
    private val errorAdapter: JsonAdapter<DefaultError>,
    private val navigationDispatcher: NavigationDispatcher
): ViewModel() {
    val events = Channel<ShopDetailsEvent>()
    val shop = handle.get<Shop>(SHOP)!!

    fun deleteShop() = viewModelScope.launch(Dispatchers.IO) {
        when(val response = invokeUnitFunction({shopService.get().deleteShop(shop.id)}, errorAdapter)) {
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

    companion object {
        const val SHOP = "shop"
    }
}