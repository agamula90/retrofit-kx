package io.github.retrofitx.android.products.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import io.github.retrofitx.DataResponse
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Product
import io.github.retrofitx.android.products.ProductsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.retrofitx.android.dto.IdError
import io.github.retrofitx.android.inject.DeferredValue
import io.github.retrofitx.android.remote.ProductService
import io.github.retrofitx.internal.invokeDataFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val productService: DeferredValue<ProductService>,
    private val errorAdapter: JsonAdapter<IdError>,
    private val navigationDispatcher: NavigationDispatcher
): ViewModel() {
    val product = handle.get<Product>(PRODUCT)!!
    val events = Channel<ProductDetailsEvent>()

    fun deleteProduct() = viewModelScope.launch(Dispatchers.IO) {
        when(val response = invokeDataFunction( {productService.get().deleteProduct(product.name) }, errorAdapter)) {
            is DataResponse.Success -> {
                navigationDispatcher.setNavigationResult(
                    backStackDestinationId = R.id.products,
                    navigationKey = ProductsViewModel.RELOAD_PRODUCTS,
                    value = true
                )
                // comment to get api error message on subsequent product deletion
                navigationDispatcher.navigateBack()
            }
            is DataResponse.ConnectionError -> {
                events.send(ProductDetailsEvent.ShowConnectionErrorMessage)
            }
            is DataResponse.ApiError -> {
                events.send(ProductDetailsEvent.ShowApiErrorMessage(response.cause))
            }
        }
    }

    companion object {
        const val PRODUCT = "product"
    }
}