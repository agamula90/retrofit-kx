package com.github.retrofitx.android.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class IdError(val id: Int)