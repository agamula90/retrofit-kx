package io.github.retrofitx.android.shops.details

import io.github.retrofitx.android.dto.DefaultError

sealed class ShopDetailsEvent {
    class ShowApiErrorMessage(val error: DefaultError): ShopDetailsEvent()
    object ShowConnectionErrorMessage: ShopDetailsEvent()
}
