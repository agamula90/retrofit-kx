package io.github.retrofitx.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.retrofitx.DataResponse
import io.github.retrofitx.UnitResponse
import io.github.retrofitx.ksp.utils.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.mockwebserver.*
import okhttp3.mockwebserver.Dispatcher
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.full.primaryConstructor

abstract class RetrofitXInstanceTest: BaseTest() {
    abstract fun KClass<*>.newRetrofitX(callFactory: Call.Factory, boxed: Boolean = false): Any

    abstract val baseUrlServers: Array<MockWebServer>

    @Test
    fun testDataFunctionsSuccessResults() {
        val successDispatcher = givenSuccessDispatcher(boxedPaths = listOf("/user"))
        baseUrlServers.forEach { it.dispatcher = successDispatcher }
        val serviceToTest = servicesPackage.getServiceWithBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))
        val dataResponses = compilationResult.whenDataFunctionsInvoked(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName()
        )

        dataResponses.thenDataResponsesAreOfTypeSuccess()
    }

    private fun givenSuccessDispatcher(
        boxedPaths: List<String>,
        simulateConnectionIssues: Boolean = false
    ): Dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return when {
                request.path == "/signOut" -> MockResponse()
                //getSupportedMethods
                request.path == "/" && boxedPaths.contains("/getSupportedMethods") -> {
                    MockResponse().setBody(
                        """
                        |{ "data":
                        |   [
                        |       "signIn",
                        |       "signUp",
                        |       "signOut"
                        |   ]
                        |}
                        """.trimMargin()
                    )
                }
                //getSupportedMethods
                request.path == "/" -> {
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
                boxedPaths.contains(request.path) -> {
                    MockResponse().setBody(
                        """
                        |{ "data":
                        |   {
                        |       "userId": 12,
                        |       "userName": "Andrii"
                        |   }
                        |}
                        """.trimMargin()
                    )
                }
                request.path != null -> {
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
            }.simulateConnectionIssues(simulateConnectionIssues)
        }
    }

    private fun givenErrorDispatcher(): Dispatcher {
        return object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setBody("{\"cause\": \"failure 1\"}").setResponseCode(500)
            }
        }
    }

    private fun MockResponse.simulateConnectionIssues(simulateConnectionIssues: Boolean): MockResponse {
        if (simulateConnectionIssues) setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
        return this
    }

    protected fun KotlinCompilation.Result.getPublicService(
        dispatcher: Dispatcher,
        serviceName: String,
        boxed: Boolean = false
    ): Any {
        val retrofitX = getRetrofitXClass().newRetrofitX(callFactory = dispatcher.toCallFactory(), boxed = boxed)
        val retrofitXClass = retrofitX.javaClass.kotlin
        val publicServiceProperty = retrofitXClass.declaredMemberProperties.first { it.name == serviceName.replaceFirstChar { it.lowercase() } }
        return publicServiceProperty.call(retrofitX)!!
    }

    private fun KotlinCompilation.Result.whenDataFunctionsInvoked(
        dispatcher: Dispatcher,
        serviceName: String,
        boxed: Boolean = false
    ): List<DataResponse<*, *>> {
        val publicService = getPublicService(dispatcher, serviceName, boxed)

        val functionParams = mapOf(
            "signIn" to arrayOf(getSignInRequest()),
            "signUp" to arrayOf(getSignUpRequest()),
            "getUser" to arrayOf("testAuthorisation"),
            "getSupportedMethods" to arrayOf("https://testsignup.com")
        )

        return runBlocking {
            val responses = mutableListOf<DataResponse<*, *>>()

            for ((functionName, params) in functionParams) {
                val function = publicService.javaClass.kotlin.functions.first { it.name == functionName }
                val result = function.callSuspend(publicService, *params)!!
                assert(result is DataResponse<*, *>) {
                    "Expect result to have type DataResponse, got ${result.javaClass.kotlin}"
                }
                responses.add(result as DataResponse<*, *>)
            }

            responses
        }
    }

    private fun KotlinCompilation.Result.getSignInRequest(): Any {
        val requestClass = classLoader.loadClass("io.github.retrofitx.test.dto.SignInRequest").kotlin
        return requestClass.primaryConstructor!!.call("Andrii")
    }

    private fun KotlinCompilation.Result.getSignUpRequest(): Any {
        val requestClass = classLoader.loadClass("io.github.retrofitx.test.dto.SignUpRequest").kotlin
        return requestClass.primaryConstructor!!.call("Andrii")
    }

    private fun KotlinCompilation.Result.whenUnitFunctionsInvoked(
        dispatcher: Dispatcher,
        serviceName: String,
        boxed: Boolean = false
    ): List<UnitResponse<*>> {
        val publicService = getPublicService(dispatcher, serviceName, boxed)
        val unitFunctionParams = mapOf<String, Array<Any>>(
            "signOut" to arrayOf()
        )

        return runBlocking {
            val responses = mutableListOf<UnitResponse<*>>()
            for ((functionName, params) in unitFunctionParams) {
                val function = publicService.javaClass.kotlin.functions.first { it.name == functionName }
                val result = function.callSuspend(publicService, *params)!!
                assert(result is UnitResponse<*>) {
                    "Expect result to have type UnitResponse, got ${result.javaClass.kotlin}"
                }
                responses.add(result as UnitResponse<*>)
            }
            responses
        }
    }

    private fun List<DataResponse<*, *>>.thenDataResponsesAreOfTypeSuccess() {
        for (response in withIndex()) {
            assert(response.value is DataResponse.Success) {
                "Expect success response for index: ${response.index}, got: ${response.value.encodeToString()}"
            }
        }
    }

    private fun List<DataResponse<*, *>>.thenDataResponsesAreOfTypeApiError() {
        for (response in withIndex()) {
            assert(response.value is DataResponse.ApiError) {
                "Expect api error response for index: ${response.index}, got: ${response.value.encodeToString()}"
            }
        }
    }

    private fun List<DataResponse<*, *>>.thenDataResponsesAreOfTypeConnectionError() {
        for (response in withIndex()) {
            assert(response.value is DataResponse.ConnectionError) {
                "Expect connection error response for index: ${response.index}, got: ${response.value.encodeToString()}"
            }
        }
    }

    private fun DataResponse<*, *>.encodeToString(): String = when(this) {
        is DataResponse.Success -> "Success"
        is DataResponse.ApiError -> "ApiError"
        is DataResponse.ConnectionError -> "ConnectionError"
    }

    private fun List<UnitResponse<*>>.thenUnitResponsesAreOfTypeSuccess() {
        for (response in withIndex()) {
            assert(response.value is UnitResponse.Success) {
                "Expect success response for index: ${response.index}, got: ${response.value.encodeToString()}"
            }
        }
    }

    private fun List<UnitResponse<*>>.thenUnitResponsesAreOfTypeApiError() {
        for (response in withIndex()) {
            assert(response.value is UnitResponse.ApiError) {
                "Expect api error response for index: ${response.index}, got: ${response.value.encodeToString()}"
            }
        }
    }

    private fun List<UnitResponse<*>>.thenUnitResponsesAreOfTypeConnectionError() {
        for (response in withIndex()) {
            assert(response.value is UnitResponse.ConnectionError) {
                "Expect connection error response for index: ${response.index}, got: ${response.value.encodeToString()}"
            }
        }
    }

    private fun UnitResponse<*>.encodeToString(): String = when(this) {
        is UnitResponse.Success -> "Success"
        is UnitResponse.ApiError -> "ApiError"
        is UnitResponse.ConnectionError -> "ConnectionError"
    }

    private fun KotlinCompilation.Result.thenSafeFunctionsSucceed(
        dispatcher: Dispatcher,
        serviceName: String,
        boxed: Boolean = false
    ) {
        val publicService = getPublicService(dispatcher, serviceName, boxed)
        val unitFunctionParams = mapOf<String, Array<Any>>(
            "signOut" to arrayOf()
        )

        runBlocking {
            for ((functionName, params) in unitFunctionParams) {
                val function = publicService.javaClass.kotlin.functions.first {
                    it.name == functionName + "Safe"
                }
                try {
                    function.callSuspend(publicService, *params)!!
                } catch (e: Exception) {
                    assert(false) {
                        """
                        |Expect safe method to not through exceptions
                        | but method ${function.name} invocation through exception: ${e.message} 
                        """.trimMargin()
                    }
                }
            }
        }
    }

    @Test
    fun testDataFunctionsApiErrorResults() {
        val errorDispatcher = givenErrorDispatcher()
        baseUrlServers.forEach { it.dispatcher = errorDispatcher }
        val serviceToTest = servicesPackage.getServiceWithBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))
        val dataResponses = compilationResult.whenDataFunctionsInvoked(
            dispatcher = errorDispatcher,
            serviceName = serviceToTest.getServiceName()
        )

        dataResponses.thenDataResponsesAreOfTypeApiError()
    }

    @Test
    fun testConnectionErrorResults() {
        val successDispatcher = givenSuccessDispatcher(
            boxedPaths = listOf("/user"),
            simulateConnectionIssues = true
        )
        baseUrlServers.forEach { it.dispatcher = successDispatcher }
        val serviceToTest = servicesPackage.getServiceWithBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))
        val dataResponses = compilationResult.whenDataFunctionsInvoked(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName()
        )
        val unitResponses = compilationResult.whenUnitFunctionsInvoked(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName()
        )

        dataResponses.thenDataResponsesAreOfTypeConnectionError()
        unitResponses.thenUnitResponsesAreOfTypeConnectionError()
        compilationResult.thenSafeFunctionsSucceed(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName()
        )
    }

    @Test
    fun testUnitFunctionsSuccessResults() {
        val successDispatcher = givenSuccessDispatcher(
            boxedPaths = listOf("/user")
        )
        baseUrlServers.forEach { it.dispatcher = successDispatcher }
        val serviceToTest = servicesPackage.getServiceWithBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))
        val unitResponses = compilationResult.whenUnitFunctionsInvoked(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName()
        )

        unitResponses.thenUnitResponsesAreOfTypeSuccess()
    }

    @Test
    fun testUnitFunctionsApiErrorResults() {
        val errorDispatcher = givenErrorDispatcher()
        baseUrlServers.forEach { it.dispatcher = errorDispatcher }
        val serviceToTest = servicesPackage.getServiceWithBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))
        val dataResponses = compilationResult.whenUnitFunctionsInvoked(
            dispatcher = errorDispatcher,
            serviceName = serviceToTest.getServiceName()
        )

        dataResponses.thenUnitResponsesAreOfTypeApiError()
    }

    @Test
    fun testSafeFunctionsWithSuccessCompletion() {
        val successDispatcher = givenSuccessDispatcher(
            boxedPaths = listOf("/user")
        )
        baseUrlServers.forEach { it.dispatcher = successDispatcher }
        val serviceToTest = servicesPackage.getServiceWithBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.thenSafeFunctionsSucceed(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName()
        )
    }

    @Test
    fun testSafeFunctionsWithExceptionalCompletion() {
        val errorDispatcher = givenErrorDispatcher()
        baseUrlServers.forEach { it.dispatcher = errorDispatcher }
        val serviceToTest = servicesPackage.getServiceWithBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.thenSafeFunctionsSucceed(
            dispatcher = errorDispatcher,
            serviceName = serviceToTest.getServiceName()
        )
    }

    @Test
    fun testDataFunctionsSuccessResultsForBoxedRetrofitX() {
        val successDispatcher = givenSuccessDispatcher(
            boxedPaths = listOf("/signIn", "/signUp", "/getSupportedMethods")
        )
        baseUrlServers.forEach { it.dispatcher = successDispatcher }
        val serviceToTest = servicesPackage.getServiceWithNotBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))
        val dataResponses = compilationResult.whenDataFunctionsInvoked(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName(),
            boxed = true
        )

        dataResponses.thenDataResponsesAreOfTypeSuccess()
    }

    @Test
    fun testDataFunctionsApiErrorResultsForBoxedRetrofitX() {
        val errorDispatcher = givenErrorDispatcher()
        baseUrlServers.forEach { it.dispatcher = errorDispatcher }
        val serviceToTest = servicesPackage.getServiceWithNotBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))
        val dataResponses = compilationResult.whenDataFunctionsInvoked(
            dispatcher = errorDispatcher,
            serviceName = serviceToTest.getServiceName(),
            boxed = true
        )

        dataResponses.thenDataResponsesAreOfTypeApiError()
    }

    @Test
    fun testConnectionErrorResultsForBoxedRetrofitX() {
        val successDispatcher = givenSuccessDispatcher(
            boxedPaths = listOf("/signIn", "/signUp", "/getSupportedMethods"),
            simulateConnectionIssues = true
        )
        baseUrlServers.forEach { it.dispatcher = successDispatcher }
        val serviceToTest = servicesPackage.getServiceWithNotBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))
        val dataResponses = compilationResult.whenDataFunctionsInvoked(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName(),
            boxed = true
        )
        val unitResponses = compilationResult.whenUnitFunctionsInvoked(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName(),
            boxed = true
        )

        dataResponses.thenDataResponsesAreOfTypeConnectionError()
        unitResponses.thenUnitResponsesAreOfTypeConnectionError()
        compilationResult.thenSafeFunctionsSucceed(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName(),
            boxed = true
        )
    }

    @Test
    fun testUnitFunctionsSuccessResultsForBoxedRetrofitX() {
        val successDispatcher = givenSuccessDispatcher(
            boxedPaths = listOf("/signIn", "/signUp", "/getSupportedMethods")
        )
        baseUrlServers.forEach { it.dispatcher = successDispatcher }
        val serviceToTest = servicesPackage.getServiceWithNotBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))
        val unitResponses = compilationResult.whenUnitFunctionsInvoked(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName(),
            boxed = true
        )

        unitResponses.thenUnitResponsesAreOfTypeSuccess()
    }

    @Test
    fun testUnitFunctionsApiErrorResultsForBoxedRetrofitX() {
        val errorDispatcher = givenErrorDispatcher()
        baseUrlServers.forEach { it.dispatcher = errorDispatcher }
        val serviceToTest = servicesPackage.getServiceWithNotBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))
        val dataResponses = compilationResult.whenUnitFunctionsInvoked(
            dispatcher = errorDispatcher,
            serviceName = serviceToTest.getServiceName(),
            boxed = true
        )

        dataResponses.thenUnitResponsesAreOfTypeApiError()
    }

    @Test
    fun testSafeFunctionsWithSuccessCompletionForBoxedRetrofitX() {
        val successDispatcher = givenSuccessDispatcher(
            boxedPaths = listOf("/signIn", "/signUp", "/getSupportedMethods")
        )
        baseUrlServers.forEach { it.dispatcher = successDispatcher }
        val serviceToTest = servicesPackage.getServiceWithNotBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.thenSafeFunctionsSucceed(
            dispatcher = successDispatcher,
            serviceName = serviceToTest.getServiceName(),
            boxed = true
        )
    }

    @Test
    fun testSafeFunctionsWithExceptionalCompletionForBoxedRetrofitX() {
        val errorDispatcher = givenErrorDispatcher()
        baseUrlServers.forEach { it.dispatcher = errorDispatcher }
        val serviceToTest = servicesPackage.getServiceWithNotBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.thenSafeFunctionsSucceed(
            dispatcher = errorDispatcher,
            serviceName = serviceToTest.getServiceName(),
            boxed = true
        )
    }
}