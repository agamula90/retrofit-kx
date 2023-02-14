package io.github.retrofitx.kotlin.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class IdError(val id: Int)