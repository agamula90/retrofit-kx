package io.github.retrofitx.android.products.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.retrofitx.DataResponse
import io.github.retrofitx.ProductService
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Product
import io.github.retrofitx.android.products.ProductsViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class ProductDetailsViewModel(
    handle: SavedStateHandle,
    private val productService: ProductService,
    private val navigationDispatcher: NavigationDispatcher
): ViewModel() {
    val product = handle.get<Product>(PRODUCT)!!
    val events = Channel<ProductDetailsEvent>()

    fun deleteProduct() = viewModelScope.launch(Dispatchers.IO) {
        when(val response = productService.deleteProduct(product.name)) {
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