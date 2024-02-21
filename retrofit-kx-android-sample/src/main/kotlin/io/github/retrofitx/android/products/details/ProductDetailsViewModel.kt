package io.github.retrofitx.android.products.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.retrofit.serialization.deserializeErrorBody
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Product
import io.github.retrofitx.android.products.ProductsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.retrofitx.android.dto.IdError
import io.github.retrofitx.android.remote.ProductService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val productService: ProductService,
    private val navigationDispatcher: NavigationDispatcher
): ViewModel() {
    val product = handle.get<Product>(PRODUCT)!!
    val events = Channel<ProductDetailsEvent>()

    fun deleteProduct() = viewModelScope.launch(Dispatchers.IO) {
        when(val response = productService.deleteProduct(product.name)) {
            is ApiResponse.Success -> {
                navigationDispatcher.setNavigationResult(
                    backStackDestinationId = R.id.products,
                    navigationKey = ProductsViewModel.RELOAD_PRODUCTS,
                    value = true
                )
                // comment to get api error message on subsequent product deletion
                navigationDispatcher.navigateBack()
            }
            is ApiResponse.Failure.Exception -> {
                events.send(ProductDetailsEvent.ShowConnectionErrorMessage)
            }
            is ApiResponse.Failure.Error -> {
                val error = response.deserializeErrorBody<Any, IdError>()!!
                events.send(ProductDetailsEvent.ShowApiErrorMessage(error))
            }
        }
    }

    companion object {
        const val PRODUCT = "product"
    }
}