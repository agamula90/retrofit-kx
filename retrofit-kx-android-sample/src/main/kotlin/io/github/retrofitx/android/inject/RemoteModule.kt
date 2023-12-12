package io.github.retrofitx.android.inject

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.github.retrofitx.android.BuildConfig
import io.github.retrofitx.android.simple.DataStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.retrofitx.android.dto.DefaultError
import io.github.retrofitx.android.dto.IdError
import io.github.retrofitx.android.remote.ProductService
import io.github.retrofitx.android.remote.ShopService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

//@Module
//@InstallIn(SingletonComponent::class)
object RemoteModule {

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

    @Singleton
    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build();
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Singleton
    @Provides
    //dependency graph = static, before code runs,
    //ksp - symbol processing, when code runs
    fun provideRetrofitX(
        dataStoreManager: DataStoreManager,
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): RetrofitXNew {
        return RetrofitXNew(
            baseUrl = dataStoreManager.getBaseUrl(),
            observer = GlobalScope,
            retrofitFactory = {
                Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(it)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
            },
        )
        //TODO uncomment to test with static base url
        //return RetrofitX(baseUrl = BuildConfig.BASE_URL, okHttpClient = okHttpClient)
    }

    @Provides
    fun provideShopService(retrofitX: RetrofitXNew): DeferredValue<ShopService> {
        return retrofitX.create(ShopService::class.java)
    }

    @Provides
    fun provideProductService(retrofitX: RetrofitXNew): DeferredValue<ProductService> {
        return retrofitX.create(ProductService::class.java)
    }

    @Singleton
    @Provides
    fun provideDefaultErrorAdapter(moshi: Moshi): JsonAdapter<DefaultError> {
        return moshi.adapter(DefaultError::class.java)
    }

    @Singleton
    @Provides
    fun provideIdErrorAdapter(moshi: Moshi): JsonAdapter<IdError> {
        return moshi.adapter(IdError::class.java)
    }
}