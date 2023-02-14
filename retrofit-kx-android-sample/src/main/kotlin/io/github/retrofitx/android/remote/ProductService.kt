package io.github.retrofitx.android.remote

import io.github.retrofitx.NotBoxed
import io.github.retrofitx.Remote
import io.github.retrofitx.android.dto.IdError
import io.github.retrofitx.android.dto.Product
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@Remote(url = "https://viridian-charmed-curtain.glitch.me/", error = IdError::class)
interface ProductService {

    @POST("products")
    suspend fun getProducts(): List<Product>

    @NotBoxed
    @GET("products/deleteProduct")
    suspend fun deleteProduct(@Query("productName") productName: String): Product
}