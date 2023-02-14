package io.github.retrofitx.android.products.details

import io.github.retrofitx.android.dto.IdError

sealed class ProductDetailsEvent {
    class ShowApiErrorMessage(val error: IdError): ProductDetailsEvent()
    object ShowConnectionErrorMessage: ProductDetailsEvent()
}
