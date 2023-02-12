package com.github.retrofitx.kotlin.remote

import com.github.retrofitx.kotlin.dto.Shop
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ShopService {

    @POST("shops")
    suspend fun getShops(): List<Shop>

    @GET("shops/delete")
    suspend fun deleteShop(@Query("shopId") shopId: Int)
}