package com.github.retrofitx.android.dto

import com.squareup.moshi.JsonClass
import com.github.retrofitx.RetrofitError

@JsonClass(generateAdapter = true)
@RetrofitError
class DefaultError(val message: String)