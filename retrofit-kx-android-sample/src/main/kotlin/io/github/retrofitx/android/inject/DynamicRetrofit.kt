package io.github.retrofitx.android.inject

import io.github.retrofitx.android.BuildConfig
import io.github.retrofitx.android.simple.DataStoreManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton


@OptIn(DelicateCoroutinesApi::class)
@Singleton
class DynamicRetrofit @Inject constructor(
    val dataStoreManager: DataStoreManager,
    val retrofitFactory: RetrofitFactory
) {

    private var retrofit: Retrofit = retrofitFactory.provideRetrofit(BuildConfig.BASE_URL)

    init {
        GlobalScope.launch {
            dataStoreManager.getBaseUrl().collect {
                retrofit = retrofitFactory.provideRetrofit(it)
            }
        }
    }

    fun <T> create(clazz: Class<T>): T {
        return retrofit.create(clazz)
    }
}