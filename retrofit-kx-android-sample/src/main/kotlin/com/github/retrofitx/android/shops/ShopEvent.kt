package com.github.retrofitx.android.shops

import com.github.retrofitx.android.dto.DefaultError

sealed class ShopEvent {
    class ShowApiErrorMessage(val error: DefaultError): ShopEvent()
    object ShowConnectionErrorMessage : ShopEvent()
}
