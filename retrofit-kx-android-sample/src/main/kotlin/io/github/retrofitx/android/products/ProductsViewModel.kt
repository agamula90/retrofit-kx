package io.github.retrofitx.android.products

import android.os.Build
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.retrofitx.DataResponse
import io.github.retrofitx.ParseFailureException
import io.github.retrofitx.ProductService
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Product
import io.github.retrofitx.android.products.details.ProductDetailsViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ProductsViewModel (
    handle: SavedStateHandle,
    private val navigationDispatcher: NavigationDispatcher,
    private val productService: ProductService
): ViewModel() {
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
        try {
            Build.VERSION_CODES.TIRAMISU
            isParseFailed.postValue(false)
            when(val response = productService.getProducts()) {
                is DataResponse.Success -> products.postValue(response.data)
                is DataResponse.ApiError -> {
                    events.send(ProductEvent.ShowApiErrorMessage(response.cause))
                }
                is DataResponse.ConnectionError -> {
                    events.send(ProductEvent.ShowConnectionErrorMessage)
                }
            }
        } catch (e: ParseFailureException) {
            products.postValue(emptyList())
            isParseFailed.postValue(true)
        }
    }

    fun goProductDetails(product: Product) {
        navigationDispatcher.navigate(R.id.goProductDetails, bundleOf(ProductDetailsViewModel.PRODUCT to product))
    }

    companion object {
        const val RELOAD_PRODUCTS = "reloadProducts"
    }
}