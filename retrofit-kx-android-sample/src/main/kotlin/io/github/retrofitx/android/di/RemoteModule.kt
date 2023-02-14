package io.github.retrofitx.android.di

import android.content.Context
import io.github.retrofitx.ProductService
import io.github.retrofitx.RetrofitX
import io.github.retrofitx.ShopService
import io.github.retrofitx.android.BuildConfig
import io.github.retrofitx.android.simple.DataStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    @Singleton
    @Provides
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                interceptor = HttpLoggingInterceptor().apply {
                    level = when {
                        BuildConfig.DEBUG -> HttpLoggingInterceptor.Level.BODY
                        else -> HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Singleton
    @Provides
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

    @Provides
    fun provideShopService(retrofitX: RetrofitX): ShopService {
        return retrofitX.shopService
    }

    @Provides
    fun provideProductService(retrofitX: RetrofitX): ProductService {
        return retrofitX.productService
    }
}