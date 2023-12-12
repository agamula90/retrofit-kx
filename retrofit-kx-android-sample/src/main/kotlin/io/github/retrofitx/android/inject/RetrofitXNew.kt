package io.github.retrofitx.android.inject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import retrofit2.Retrofit

class RetrofitXNew(val baseUrl: Flow<String>, val observer: CoroutineScope, val retrofitFactory: (String) -> Retrofit) {

    private var retrofit: Retrofit? = null
    private val services = mutableListOf<DeferredValue<*>>()

    init {
        observer.launch {
            baseUrl.collect {
                retrofit = retrofitFactory(it)
                services.forEach { it.updateFromFactory() }
            }
        }
    }

    fun <T> create(clazz: Class<T>): DeferredValue<T> {
        return DeferredValue { retrofit?.create(clazz) }.also {
            it.updateFromFactory()
            services.add(it)
        }
    }
}