package com.github.retrofitx.ksp

import com.squareup.moshi.Moshi
import okhttp3.Call
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import kotlin.reflect.KClass
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

class RetrofitXStaticBaseUrlTest: RetrofitXInstanceTest() {
    @get:Rule val server = MockWebServer()

    override fun KClass<*>.newRetrofitX(callFactory: Call.Factory, boxed: Boolean): Any {
        val constructor = constructors.first {
            it.valueParameters.size == 4 &&
                    it.valueParameters[0].type.jvmErasure == String::class &&
                    it.valueParameters[1].type.jvmErasure == Call.Factory::class &&
                    it.valueParameters[2].type.jvmErasure == Moshi::class &&
                    it.valueParameters[3].type.jvmErasure == Boolean::class
        }
        return constructor.call(
            server.url("/").toString(),
            callFactory,
            Moshi.Builder().build(),
            boxed
        )
    }

    override val baseUrlServers: Array<MockWebServer>
        get() = arrayOf(MockWebServer())
}