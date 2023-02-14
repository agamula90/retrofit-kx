package io.github.retrofitx.kotlin.remote

import io.github.retrofitx.Boxed
import io.github.retrofitx.Remote
import io.github.retrofitx.kotlin.dto.IdError
import io.github.retrofitx.kotlin.dto.Product
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@Remote(url = "https://viridian-charmed-curtain.glitch.me/", error = IdError::class)
interface ProductService {

    @Boxed
    @POST("products")
    suspend fun getProducts(): List<Product>

    @GET("products/deleteProduct")
    suspend fun deleteProduct(@Query("productName") productName: String): Product
}