package com.github.retrofitx.android.shops.details

import com.github.retrofitx.android.dto.DefaultError

sealed class ShopDetailsEvent {
    class ShowApiErrorMessage(val error: DefaultError): ShopDetailsEvent()
    object ShowConnectionErrorMessage: ShopDetailsEvent()
}
