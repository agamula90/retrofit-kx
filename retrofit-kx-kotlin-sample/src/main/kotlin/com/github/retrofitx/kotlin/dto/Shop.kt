package com.github.retrofitx.kotlin.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Shop(val id: Int, val name: String)