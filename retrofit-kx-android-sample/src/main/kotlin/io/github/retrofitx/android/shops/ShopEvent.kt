package io.github.retrofitx.android.shops

import io.github.retrofitx.android.dto.DefaultError

sealed class ShopEvent {
    class ShowApiErrorMessage(val error: DefaultError): ShopEvent()
    object ShowConnectionErrorMessage : ShopEvent()
}
