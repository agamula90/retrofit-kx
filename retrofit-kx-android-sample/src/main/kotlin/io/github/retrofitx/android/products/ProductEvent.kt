package io.github.retrofitx.android.products

import io.github.retrofitx.android.dto.IdError

sealed class ProductEvent {
    class ShowApiErrorMessage(val error: IdError): ProductEvent()
    object ShowConnectionErrorMessage : ProductEvent()
}
