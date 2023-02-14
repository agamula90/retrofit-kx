package io.github.retrofitx

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.github.retrofitx.internal.invokeDataFunction
import io.github.retrofitx.internal.invokeUnitFunction
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

class HttpFunctionsTest {

    @get:Rule val server = MockWebServer()
    private val moshi = Moshi.Builder().build()
    private val errorAdapter = moshi.adapter(Error::class.java)

    @Test(expected = ParseFailureException::class)
    fun testDataFunctionFailedWhenResponseBodyIsEmpty() {
        val serviceToTest = givenServiceToTest()

        server.whenEmptyResponsesProvided()
        serviceToTest.whenDataFunctionInvoked()
    }

    @Test(expected = ParseFailureException::class)
    fun testDataFunctionFailedWhenResponseIsNotJson() {
        val serviceToTest = givenServiceToTest()

        server.whenMalformedJsonProvided()
        serviceToTest.whenDataFunctionInvoked()
    }

    @Test(expected = ParseFailureException::class)
    fun testDataFunctionFailedWhenResponseIsUnexpectedJson() {
        val serviceToTest = givenServiceToTest()

        server.whenUnexpectedJsonProvided()
        serviceToTest.whenDataFunctionInvoked()
    }

    @Test(expected = ParseFailureException::class)
    fun testDataFunctionFailedWhenMoshiAdapterNotGenerated() {
        val serviceToTest = givenProductService()

        server.whenCorrectJsonProvided()
        serviceToTest.whenDataFunctionInvoked()
    }

    @Test
    fun testDataFunctionReturnConnectionErrorWhenConnectionFailure() {
        val serviceToTest = givenServiceToTest()

        server.whenCorrectJsonProvided(simulateConnectionIssues = true)
        val response = serviceToTest.whenDataFunctionInvoked()

        assert(response is DataResponse.ConnectionError) {
            "Expect response of type ConnectionError, got $response"
        }
    }

    @Test
    fun testDataFunctionReturnSuccessWhenResponseIsExpectedJson() {
        val serviceToTest = givenServiceToTest()

        server.whenCorrectJsonProvided()
        val response = serviceToTest.whenDataFunctionInvoked()

        assert(response is DataResponse.Success) {
            "Expect response of type Success, got $response"
        }
    }

    @Test(expected = ParseFailureException::class)
    fun testUnitFunctionFailedWhenMoshiAdapterNotGenerated() {
        val serviceToTest = givenProductService()

        server.whenCorrectJsonProvided()
        serviceToTest.whenUnitFunctionInvoked()
    }

    @Test
    fun testUnitFunctionReturnConnectionErrorWhenConnectionFailure() {
        val serviceToTest = givenServiceToTest()

        server.whenCorrectJsonProvided(simulateConnectionIssues = true)
        val response = serviceToTest.whenUnitFunctionInvoked()

        assert(response is UnitResponse.ConnectionError) {
            "Expect response of type ConnectionError, got $response"
        }
    }

    @Test
    fun testUnitFunctionReturnSuccessWhenResponseIsExpectedJson() {
        val serviceToTest = givenServiceToTest()

        server.whenCorrectJsonProvided()
        val response = serviceToTest.whenUnitFunctionInvoked()

        assert(response is UnitResponse.Success) {
            "Expect response of type Success, got $response"
        }
    }

    @Test(expected = ParseFailureException::class)
    fun testDataFunctionFailedWhenErrorBodyIsEmpty() {
        val serviceToTest = givenServiceToTest()

        server.whenEmptyResponsesProvided(simulateErrors = true)
        serviceToTest.whenDataFunctionInvoked()
    }

    @Test(expected = ParseFailureException::class)
    fun testDataFunctionFailedWhenErrorIsNotJson() {
        val serviceToTest = givenServiceToTest()

        server.whenMalformedJsonProvided(simulateErrors = true)
        serviceToTest.whenDataFunctionInvoked()
    }

    @Test(expected = ParseFailureException::class)
    fun testDataFunctionFailedWhenErrorIsUnexpectedJson() {
        val serviceToTest = givenServiceToTest()

        server.whenErrorUnexpectedJsonProvided()
        serviceToTest.whenDataFunctionInvoked()
    }

    @Test
    fun testDataFunctionReturnApiErrorWhenErrorIsExpectedJson() {
        val serviceToTest = givenServiceToTest()

        server.whenErrorCorrectJsonProvided()
        val response = serviceToTest.whenDataFunctionInvoked()

        assert(response is DataResponse.ApiError) {
            "Expect response of type ApiError, got $response"
        }
    }

    @Test(expected = ParseFailureException::class)
    fun testUnitFunctionFailedWhenErrorBodyIsEmpty() {
        val serviceToTest = givenServiceToTest()

        server.whenEmptyResponsesProvided(simulateErrors = true)
        serviceToTest.whenUnitFunctionInvoked()
    }

    @Test(expected = ParseFailureException::class)
    fun testUnitFunctionFailedWhenErrorIsNotJson() {
        val serviceToTest = givenServiceToTest()

        server.whenMalformedJsonProvided(simulateErrors = true)
        serviceToTest.whenUnitFunctionInvoked()
    }

    @Test(expected = ParseFailureException::class)
    fun testUnitFunctionFailedWhenErrorIsUnexpectedJson() {
        val serviceToTest = givenServiceToTest()

        server.whenErrorUnexpectedJsonProvided()
        serviceToTest.whenUnitFunctionInvoked()
    }

    @Test
    fun testUnitFunctionReturnApiErrorWhenErrorIsExpectedJson() {
        val serviceToTest = givenServiceToTest()

        server.whenErrorCorrectJsonProvided()
        val response = serviceToTest.whenUnitFunctionInvoked()

        assert(response is UnitResponse.ApiError) {
            "Expect response of type ApiError, got $response"
        }
    }

    private fun givenServiceToTest(): AuthorisationService {
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthorisationService::class.java)
    }

    private fun givenProductService(): ProductService {
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ProductService::class.java)
    }

    private fun MockWebServer.whenEmptyResponsesProvided(simulateErrors: Boolean = false) {
        dispatcher = object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().simulateErrors(simulateErrors)
            }
        }
    }

    private fun MockWebServer.whenMalformedJsonProvided(simulateErrors: Boolean = false) {
        dispatcher = object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setBody("<html>malformed json</html>").simulateErrors(simulateErrors)
            }
        }
    }

    private fun MockResponse.simulateErrors(simulateErrors: Boolean): MockResponse {
        if (simulateErrors) setResponseCode(500)
        return this
    }

    private fun MockWebServer.whenUnexpectedJsonProvided() {
        dispatcher = object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when(val path = request.path!!) {
                    "/signIn" -> {
                        MockResponse().setBody("{\"userId\": 12, \"userPhoto\": \"https://google.com\"}")
                    }
                    "/signUp", "/addProduct" -> MockResponse()
                    else -> {
                        when {
                            path.startsWith("/getProduct") -> {
                                MockResponse().setBody("{\"productName\": \"testProduct\"}")
                            }
                            else -> MockResponse().setResponseCode(404)
                        }
                    }
                }
            }
        }
    }

    private fun MockWebServer.whenErrorUnexpectedJsonProvided() {
        dispatcher = object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setBody("{\"reason\": \"failure 1\"}").simulateErrors(true)
            }
        }
    }

    private fun MockWebServer.whenErrorCorrectJsonProvided() {
        dispatcher = object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setBody("{\"cause\": \"failure 1\"}").simulateErrors(true)
            }
        }
    }

    private fun MockWebServer.whenCorrectJsonProvided(
        simulateConnectionIssues: Boolean = false
    ) {
        dispatcher = object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when(val path = request.path!!) {
                    "/signIn" -> {
                        MockResponse().setBody("{\"userId\": 12, \"userName\": \"Andrii\"}")
                    }
                    "/signUp", "/addProduct" -> MockResponse()
                    else -> {
                        when {
                            path.startsWith("/getProduct") -> {
                                MockResponse().setBody("{\"productId\": 12}")
                            }
                            else -> MockResponse().setResponseCode(404)
                        }
                    }
                }.simulateConnectionIssues(simulateConnectionIssues)
            }
        }
    }

    private fun MockResponse.simulateConnectionIssues(simulateConnectionIssues: Boolean): MockResponse {
        if (simulateConnectionIssues) setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
        return this
    }

    private fun AuthorisationService.whenDataFunctionInvoked(): DataResponse<UserDTO, Error> {
        return runBlocking {
            invokeDataFunction(
                retrofitFunction = { signIn(SignInRequest("test")) },
                errorAdapter = errorAdapter
            )
        }
    }

    private fun ProductService.whenDataFunctionInvoked(): DataResponse<Product, Error> {
        return runBlocking {
            invokeDataFunction(
                retrofitFunction = { getProducts(10) },
                errorAdapter = errorAdapter
            )
        }
    }

    private fun AuthorisationService.whenUnitFunctionInvoked(): UnitResponse<Error> {
        return runBlocking {
            invokeUnitFunction(
                retrofitFunction = { signUp(SignUpRequest("test"))},
                errorAdapter = errorAdapter
            )
        }
    }

    private fun ProductService.whenUnitFunctionInvoked(): UnitResponse<Error> {
        return runBlocking {
            invokeUnitFunction(
                retrofitFunction = { addProduct(Product(10)) },
                errorAdapter = errorAdapter
            )
        }
    }

    companion object {
        @JsonClass(generateAdapter = true)
        class SignInRequest(val name: String)

        @JsonClass(generateAdapter = true)
        class UserDTO(val userId: Int, val userName: String)

        @JsonClass(generateAdapter = true)
        class SignUpRequest(val name: String)

        @JsonClass(generateAdapter = true)
        class Error(val cause: String)

        interface AuthorisationService {

            @POST("signIn")
            suspend fun signIn(@Body body: SignInRequest): UserDTO

            @POST("signUp")
            suspend fun signUp(@Body body: SignUpRequest)
        }

        class Product(val productId: Long)

        interface ProductService {
            @POST("getProduct")
            suspend fun getProducts(@Path(value = "id") id: Long): Product

            @POST("addProduct")
            suspend fun addProduct(@Body body: Product)
        }
    }
}