package io.github.retrofitx.android.products

import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import io.github.retrofitx.DataResponse
import io.github.retrofitx.ParseFailureException
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.R
import io.github.retrofitx.android.dto.Product
import io.github.retrofitx.android.products.details.ProductDetailsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.retrofitx.android.dto.IdError
import io.github.retrofitx.android.inject.DynamicRetrofit
import io.github.retrofitx.android.remote.ProductService
import io.github.retrofitx.internal.invokeDataFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val navigationDispatcher: NavigationDispatcher,
    private val retrofit: DynamicRetrofit,
    private val errorAdapter: JsonAdapter<IdError>
): ViewModel() {
    val events = Channel<ProductEvent>()
    val products = handle.getLiveData<List<Product>>("products", emptyList())
    val isParseFailed = handle.getLiveData("isParseFailed", false)

    private val productService: ProductService
        get() = retrofit.create(ProductService::class.java)
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
            isParseFailed.postValue(false)
            when(val response = invokeDataFunction({ productService.getProducts() }, errorAdapter)) {
                is DataResponse.Success -> products.postValue(response.data.data)
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