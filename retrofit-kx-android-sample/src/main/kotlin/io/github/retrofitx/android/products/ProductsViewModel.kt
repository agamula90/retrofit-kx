package io.github.retrofitx.android.products

import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.retrofit.serialization.deserializeErrorBody
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Product
import io.github.retrofitx.android.products.details.ProductDetailsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.retrofitx.android.dto.IdError
import io.github.retrofitx.android.remote.ProductService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val navigationDispatcher: NavigationDispatcher,
    private val productService: ProductService
) : ViewModel() {
    val events = Channel<ProductEvent>()
    val products = handle.getLiveData<List<Product>>("products", emptyList())
    val isParseFailed = handle.getLiveData("isParseFailed", false)

    init {
        if (handle.get<List<Product>>("products").isNullOrEmpty()) {
            loadProducts()
        }

        navigationDispatcher.observeNavigationResult(
            navigationKey = RELOAD_PRODUCTS,
            initialValue = false,
            coroutineScope = viewModelScope
        ) { reloadProducts -> if (reloadProducts) loadProducts() }
    }

    fun loadProducts() = viewModelScope.launch(Dispatchers.IO) {
        isParseFailed.postValue(false)
        when (val response = productService.getProducts()) {
            is ApiResponse.Success -> products.postValue(response.data.data)
            is ApiResponse.Failure.Error -> {
                val error = response.deserializeErrorBody<Any, IdError>()!!
                events.send(ProductEvent.ShowApiErrorMessage(error))
            }

            is ApiResponse.Failure.Exception -> {
                events.send(ProductEvent.ShowConnectionErrorMessage)
            }
        }
    }

    fun goProductDetails(product: Product) {
        navigationDispatcher.navigate(
            R.id.goProductDetails,
            bundleOf(ProductDetailsViewModel.PRODUCT to product)
        )
    }

    companion object {
        const val RELOAD_PRODUCTS = "reloadProducts"
    }
}