package io.github.retrofitx.android.shops.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.retrofit.serialization.deserializeErrorBody
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Shop
import io.github.retrofitx.android.shops.ShopsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.retrofitx.android.dto.DefaultError
import io.github.retrofitx.android.remote.ShopService
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
            is ApiResponse.Success -> {
                navigationDispatcher.setNavigationResult(
                    backStackDestinationId = R.id.shops,
                    navigationKey = ShopsViewModel.RELOAD_SHOPS,
                    value = true
                )
                // comment to get api error message on subsequent shop deletion
                navigationDispatcher.navigateBack()
            }
            is ApiResponse.Failure.Exception -> {
                events.send(ShopDetailsEvent.ShowConnectionErrorMessage)
            }
            is ApiResponse.Failure.Error -> {
                val error = response.deserializeErrorBody<Any, DefaultError>()!!
                events.send(ShopDetailsEvent.ShowApiErrorMessage(error))
            }
        }
    }

    companion object {
        const val SHOP = "shop"
    }
}