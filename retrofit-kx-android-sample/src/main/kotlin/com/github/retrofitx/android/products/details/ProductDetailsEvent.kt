package com.github.retrofitx.android.products.details

import com.github.retrofitx.android.dto.IdError

sealed class ProductDetailsEvent {
    class ShowApiErrorMessage(val error: IdError): ProductDetailsEvent()
    object ShowConnectionErrorMessage: ProductDetailsEvent()
}
