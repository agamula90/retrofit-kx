package io.github.retrofitx.kotlin.dto

import io.github.retrofitx.RetrofitError
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@RetrofitError
class DefaultError(val message: String)