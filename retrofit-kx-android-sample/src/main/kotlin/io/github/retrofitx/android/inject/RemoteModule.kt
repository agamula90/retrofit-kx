package io.github.retrofitx.android.inject

import io.github.retrofitx.RetrofitX
import io.github.retrofitx.android.BuildConfig
import io.github.retrofitx.android.simple.DataStoreManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module
@ComponentScan("io.github.retrofitx.android")
object AnnotationBasedModule {

}

val remoteModule = module {
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