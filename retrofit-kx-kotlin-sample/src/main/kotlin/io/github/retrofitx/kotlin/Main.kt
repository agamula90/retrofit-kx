package io.github.retrofitx.kotlin

import io.github.retrofitx.DataResponse
import io.github.retrofitx.RetrofitX
import io.github.retrofitx.UnitResponse
import kotlinx.coroutines.*

// Default error urls: https://statuesque-valuable-avenue.glitch.me/, https://vast-berry-paranthodon.glitch.me/
// id error url: https://viridian-charmed-curtain.glitch.me/
fun main() {
    val retrofitX = RetrofitX(baseUrl = "https://statuesque-valuable-avenue.glitch.me/")
    runBlocking(Dispatchers.IO) {
        //retrofitX.printAllShops()
        //retrofitX.printAllProducts()
        retrofitX.deleteProduct("be3_product2")
        retrofitX.deleteShop(10)
    }
}

private suspend fun RetrofitX.deleteProduct(productName: String) {
    when (val response = productService.deleteProduct(productName)) {
        is DataResponse.Success -> {
            println("product ${response.data} successfully deleted")
        }
        is DataResponse.ConnectionError -> {
            response.cause.printStackTrace(System.err)
        }
        is DataResponse.ApiError -> {
            println("Oops, api error: ${response.cause.id} [${response.errorCode}]")
        }
    }
}

private suspend fun RetrofitX.deleteShop(shopId: Int) {
    when (val response = shopService.deleteShop(shopId)) {
        is UnitResponse.Success -> {
            println("shop $shopId successfully deleted")
        }
        is UnitResponse.ConnectionError -> {
            response.cause.printStackTrace(System.err)
        }
        is UnitResponse.ApiError -> {
            println("Oops, api error: ${response.cause.message} [${response.errorCode}]")
        }
    }
}

private suspend fun RetrofitX.printAllProducts() {
    when (val response = productService.getProducts()) {
        is DataResponse.Success -> {
            println("Products: ${response.data.joinToString()}")
        }
        is DataResponse.ConnectionError -> {
            response.cause.printStackTrace(System.err)
        }
        is DataResponse.ApiError -> {
            println("Oops, api error: ${response.cause.id} [${response.errorCode}]")
        }
    }
}

private suspend fun RetrofitX.printAllShops() {
    when (val response = shopService.getShops()) {
        is DataResponse.Success -> {
            println("Shops: ${response.data.joinToString()}")
        }
        is DataResponse.ConnectionError -> {
            response.cause.printStackTrace(System.err)
        }
        is DataResponse.ApiError -> {
            println("Oops, api error: ${response.cause.message} [${response.errorCode}]")
        }
    }
}