package com.github.retrofitx.android.products

import com.github.retrofitx.android.dto.IdError

sealed class ProductEvent {
    class ShowApiErrorMessage(val error: IdError): ProductEvent()
    object ShowConnectionErrorMessage : ProductEvent()
}
