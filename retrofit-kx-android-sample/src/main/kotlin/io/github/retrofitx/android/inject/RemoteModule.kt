package io.github.retrofitx.android.inject

import io.github.retrofitx.RetrofitX
import io.github.retrofitx.android.BuildConfig
import io.github.retrofitx.android.NavigationDispatcher
import io.github.retrofitx.android.shops.ShopsViewModel
import io.github.retrofitx.android.shops.details.ShopDetailsViewModel
import io.github.retrofitx.android.products.ProductsViewModel
import io.github.retrofitx.android.products.details.ProductDetailsViewModel
import io.github.retrofitx.android.settings.SettingsViewModel
import io.github.retrofitx.android.simple.DataStoreManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module


val remoteModule = module {
    single {
        DataStoreManager(androidApplication().applicationContext)
    }
    single {
        provideOkHttpClient()
    }
    single {
        provideRetrofitX(get(DataStoreManager::class), get(OkHttpClient::class))
    }
    factory {
        get<RetrofitX>(RetrofitX::class).shopService
    }
    factory {
        get<RetrofitX>(RetrofitX::class).productService
    }
    single {
        NavigationDispatcher()
    }
    viewModelOf(::ShopsViewModel)
    viewModelOf(::ShopDetailsViewModel)
    viewModelOf(::ProductsViewModel)
    viewModelOf(::ProductDetailsViewModel)
    viewModelOf(::SettingsViewModel)
}

private fun provideOkHttpClient() = OkHttpClient.Builder()
    .addInterceptor(
        interceptor = HttpLoggingInterceptor().apply {
            level = when {
                BuildConfig.DEBUG -> HttpLoggingInterceptor.Level.BODY
                else -> HttpLoggingInterceptor.Level.NONE
            }
        }
    )
    .build()

@OptIn(DelicateCoroutinesApi::class)
fun provideRetrofitX(
    dataStoreManager: DataStoreManager,
    okHttpClient: OkHttpClient
): RetrofitX {
    return RetrofitX(
        baseUrl = dataStoreManager.getBaseUrl(),
        okHttpClient = okHttpClient,
        scope = GlobalScope,
        boxedByDefault = true
    )
    //TODO uncomment to test with static base url
    //return RetrofitX(baseUrl = BuildConfig.BASE_URL, okHttpClient = okHttpClient)
}