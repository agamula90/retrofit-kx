package io.github.retrofitx.android.inject

import retrofit2.Retrofit

fun interface RetrofitFactory {
    fun provideRetrofit(baseUrl: String): Retrofit
}