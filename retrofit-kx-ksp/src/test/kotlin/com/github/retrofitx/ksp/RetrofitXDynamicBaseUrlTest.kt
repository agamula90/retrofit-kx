package com.github.retrofitx.ksp

import com.squareup.moshi.Moshi
import com.tschuchort.compiletesting.KotlinCompilation
import com.github.retrofitx.internal.ServicesCache
import com.github.retrofitx.ksp.utils.getDefaultService
import com.github.retrofitx.ksp.utils.getRetrofitXClass
import com.github.retrofitx.ksp.utils.getServiceName
import com.github.retrofitx.ksp.utils.toCallFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.Call
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

class RetrofitXDynamicBaseUrlTest: RetrofitXInstanceTest() {

    @get:Rule val testCoroutineScope = TestCoroutineScope()
    @get:Rule val oldServer = MockWebServer()
    @get:Rule val newServer = MockWebServer()

    override fun KClass<*>.newRetrofitX(callFactory: Call.Factory, boxed: Boolean): Any {
        val constructor = constructors.first {
            it.valueParameters.size == 5 &&
                    it.valueParameters[0].type.jvmErasure == Flow::class &&
                    it.valueParameters[1].type.jvmErasure == CoroutineScope::class &&
                    it.valueParameters[2].type.jvmErasure == Call.Factory::class &&
                    it.valueParameters[3].type.jvmErasure == Moshi::class &&
                    it.valueParameters[4].type.jvmErasure == Boolean::class
        }
        return constructor.call(
            flow {
                emit(oldServer.url("/").toString())
                delay(BASE_URL_CHANGE_DELAY.toLong())
                emit(newServer.url("/").toString())
            },
            testCoroutineScope,
            callFactory,
            Moshi.Builder().build(),
            boxed
        )
    }

    override val baseUrlServers: Array<MockWebServer>
        get() = arrayOf(oldServer, newServer)

    @Test
    fun testServiceCacheIsEmptyAfterBaseUrlChanged() {
        val serviceToTest = servicesPackage.getDefaultService()
        val dispatcher = givenSuccessDispatcher()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        testCoroutineScope.launch(Dispatchers.IO) {
            delay(WAIT_BASE_URL_INITIALISED_MILLIS)
            val servicesCache = compilationResult.getServiceCache(dispatcher)
            servicesCache.thenCacheIsFilledAfterFirstServiceInvocation(
                compilationResult = compilationResult,
                dispatcher = dispatcher,
                serviceName = serviceToTest.getServiceName()
            )
            delay(TimeUnit.SECONDS.toMillis(BASE_URL_CHANGE_DELAY.toLong()))
            compilationResult.getServiceCache(dispatcher).thenCacheIsChanged(servicesCache)
        }
    }

    private suspend fun ServicesCache.thenCacheIsFilledAfterFirstServiceInvocation(
        compilationResult: KotlinCompilation.Result,
        dispatcher: Dispatcher,
        serviceName: String
    ) {
        assert(size() == 0) {
            "Expect cache being empty before first public service invocation, but it contained ${size()} items"
        }
        val publicService = compilationResult.getPublicService(dispatcher, serviceName)
        val function = publicService.javaClass.kotlin.functions.first {
            it.name == "signOutSafe"
        }
        function.callSuspend(publicService)
        assert(size() == 1) {
            "Expect cache size = 1 after first public service invocation, but it contained ${size()} items"
        }
    }

    private fun ServicesCache.thenCacheIsChanged(servicesCache: ServicesCache) {
        assert(this === servicesCache) {
            "Expect cache recreated, but it it left untouched"
        }
    }

    private fun KotlinCompilation.Result.getServiceCache(dispatcher: Dispatcher): ServicesCache {
        val retrofitX = getRetrofitXClass().newRetrofitX(callFactory = dispatcher.toCallFactory())
        val servicesCacheProperty = retrofitX.javaClass.kotlin.declaredMemberProperties.first { it.name == "servicesCache" }
        return servicesCacheProperty.call(retrofitX)!! as ServicesCache
    }

    private fun givenSuccessDispatcher(): Dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return when(request.path) {
                "/signOut" -> MockResponse()
                //getSupportedMethods
                "/" -> {
                    MockResponse().setBody(
                        """
                        |[
                        |   "signIn",
                        |   "signUp",
                        |   "signOut"
                        |]
                        """.trimMargin()
                    )
                }
                "/signIn", "/signUp", "/user" -> {
                    MockResponse().setBody(
                        """
                        |{
                        |   "userId": 12,
                        |   "userName": "Andrii"
                        |}
                        """.trimMargin()
                    )
                }
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    companion object {
        const val BASE_URL_CHANGE_DELAY = 2
        private const val WAIT_BASE_URL_INITIALISED_MILLIS = 200L

        class TestCoroutineScope: ExternalResource(), CoroutineScope {

            override val coroutineContext: CoroutineContext = Job()

            override fun after() {
                cancel()
                super.after()
            }
        }
    }
}