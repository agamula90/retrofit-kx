package com.github.retrofitx.kotlin.dto

import com.github.retrofitx.RetrofitError
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@RetrofitError
class DefaultError(val message: String)