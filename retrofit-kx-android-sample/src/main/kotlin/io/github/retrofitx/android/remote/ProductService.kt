package io.github.retrofitx.android.remote

import io.github.retrofitx.android.dto.Data
import io.github.retrofitx.android.dto.Product
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ProductService {

    @POST("products")
    suspend fun getProducts(): Data<List<Product>>

    @GET("products/deleteProduct")
    suspend fun deleteProduct(@Query("productName") productName: String): Product
}