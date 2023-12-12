package io.github.retrofitx.android.remote

import io.github.retrofitx.android.dto.Shop
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ShopService {

    @POST("shops")
    suspend fun getShops(): List<Shop>

    @GET("shops/delete")
    suspend fun deleteShop(@Query("shopId") shopId: Int)
}