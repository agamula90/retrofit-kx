package com.github.retrofitx.kotlin.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Product(val name: String, val price: Float)