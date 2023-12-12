package io.github.retrofitx.android.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Data<T>(val data: T)
