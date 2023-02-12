package com.github.retrofitx.android.remote

import com.github.retrofitx.NotBoxed
import com.github.retrofitx.android.dto.Shop
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@NotBoxed
interface ShopService {

    @POST("shops")
    suspend fun getShops(): List<Shop>

    @GET("shops/delete")
    suspend fun deleteShop(@Query("shopId") shopId: Int)
}