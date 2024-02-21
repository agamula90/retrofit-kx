package io.github.retrofitx.android.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Data<T>(val data: T)