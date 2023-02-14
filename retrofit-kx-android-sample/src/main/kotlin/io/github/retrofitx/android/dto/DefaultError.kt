package io.github.retrofitx.android.dto

import com.squareup.moshi.JsonClass
import io.github.retrofitx.RetrofitError

@JsonClass(generateAdapter = true)
@RetrofitError
class DefaultError(val message: String)