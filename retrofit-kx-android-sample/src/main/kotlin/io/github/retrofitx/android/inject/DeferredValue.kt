package io.github.retrofitx.android.inject

class DeferredValue<T>(private val factory: () -> T?) {
    private var source: T? = null

    fun updateFromFactory() {
        source = factory()
    }

    fun get(): T {
        return source!!;
    }
}