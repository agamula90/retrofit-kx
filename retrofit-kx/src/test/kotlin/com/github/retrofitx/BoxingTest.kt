package com.github.retrofitx

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.github.retrofitx.internal.ResponseBoxingFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

class BoxingTest {

    @get:Rule val server = MockWebServer()

    @Test(expected = JsonDataException::class)
    fun testDecodingFailedForBoxedFunctionWithOrdinaryServer() {
        val serviceToTest = givenServiceToTest(boxedByDefault = false)

        server.whenOrdinaryResponsesProvided()
        serviceToTest.whenBoxedFunctionInvoked()
    }

    @Test
    fun testDecodingSucceedForNotBoxedFunctionWithOrdinaryServer() {
        val serviceToTest = givenServiceToTest(boxedByDefault = false)

        server.whenOrdinaryResponsesProvided()
        val user = serviceToTest.whenNotBoxedFunctionInvoked()

        user.thenResponseIsCorrect()
    }

    @Test
    fun testDecodingSucceedForOrdinaryFunctionWithOrdinaryServer() {
        val serviceToTest = givenServiceToTest(boxedByDefault = false)

        server.whenOrdinaryResponsesProvided()
        val user = serviceToTest.whenOrdinaryFunctionInvoked()

        user.thenResponseIsCorrect()
    }

    @Test
    fun testDecodingSucceedForBoxedFunctionWithBoxedServer() {
        val serviceToTest = givenServiceToTest(boxedByDefault = false)

        server.whenBoxedResponsesProvided()
        val user = serviceToTest.whenBoxedFunctionInvoked()

        user.thenResponseIsCorrect()
    }

    @Test(expected = JsonDataException::class)
    fun testDecodingFailedForNotBoxedFunctionWithBoxedServer() {
        val serviceToTest = givenServiceToTest(boxedByDefault = false)

        server.whenBoxedResponsesProvided()
        serviceToTest.whenNotBoxedFunctionInvoked()
    }

    @Test(expected = JsonDataException::class)
    fun testDecodingFailedForOrdinaryFunctionWithBoxedServer() {
        val serviceToTest = givenServiceToTest(boxedByDefault = false)

        server.whenBoxedResponsesProvided()
        serviceToTest.whenOrdinaryFunctionInvoked()
    }

    @Test
    fun testDecodingResultForAnnotatedFunctionNotDependsIfBoxedByDefaultIsTrueOrFalse() {
        val notBoxedService = givenServiceToTest(boxedByDefault = false)
        val boxedService = givenServiceToTest(boxedByDefault = true)

        server.whenOrdinaryResponsesProvided()
        var userOfBoxedByDefaultService = boxedService.whenNotBoxedFunctionInvoked()
        var userOfNotBoxedByDefaultService = notBoxedService.whenNotBoxedFunctionInvoked()
        var exceptionThrownForBoxedByDefaultService = whenExceptionThrown { boxedService.whenBoxedFunctionInvoked() }
        var exceptionThrownForNotBoxedByDefaultService = whenExceptionThrown { notBoxedService.whenBoxedFunctionInvoked() }

        assert(userOfBoxedByDefaultService == userOfNotBoxedByDefaultService) {
            """
            |Expect getting same results for boxed by default services and not boxed by default services, but 
            |for not boxed function in not boxed servers got different results
            |boxed by default service: $userOfBoxedByDefaultService,
            |not boxed by default service: $userOfNotBoxedByDefaultService
            |""".trimMargin()
        }
        assert(exceptionThrownForBoxedByDefaultService == exceptionThrownForNotBoxedByDefaultService) {
            """
            |Expect getting same results for boxed by default services and not boxed by default services, but 
            |for boxed function in not boxed servers got different results. Expect exceptions being thrown for such functions, got
            |exception thrown by boxed by default service: $exceptionThrownForBoxedByDefaultService,
            |exception thrown by not boxed by default service: $exceptionThrownForNotBoxedByDefaultService
            |""".trimMargin()
        }

        server.whenBoxedResponsesProvided()
        userOfBoxedByDefaultService = boxedService.whenBoxedFunctionInvoked()
        userOfNotBoxedByDefaultService = notBoxedService.whenBoxedFunctionInvoked()
        exceptionThrownForBoxedByDefaultService = whenExceptionThrown { boxedService.whenNotBoxedFunctionInvoked() }
        exceptionThrownForNotBoxedByDefaultService = whenExceptionThrown { notBoxedService.whenNotBoxedFunctionInvoked() }

        assert(userOfBoxedByDefaultService == userOfNotBoxedByDefaultService) {
            """
            |Expect getting same results for boxed by default services and not boxed by default services, but 
            |for boxed function in boxed servers got different results
            |boxed by default service: $userOfBoxedByDefaultService,
            |not boxed by default service: $userOfNotBoxedByDefaultService
            |""".trimMargin()
        }
        assert(exceptionThrownForBoxedByDefaultService == exceptionThrownForNotBoxedByDefaultService) {
            """
            |Expect getting same results for boxed by default services and not boxed by default services, but 
            |for not boxed function in boxed servers got different results. Expect exceptions being thrown for such functions, got
            |exception thrown by boxed by default service: $exceptionThrownForBoxedByDefaultService,
            |exception thrown by not boxed by default service: $exceptionThrownForNotBoxedByDefaultService
            |""".trimMargin()
        }
    }

    @Test
    fun testDecodingSucceedForOrdinaryFunctionInBoxedByDefaultServiceWithBoxedServer() {
        val serviceToTest = givenServiceToTest(boxedByDefault = true)

        server.whenBoxedResponsesProvided()
        val user = serviceToTest.whenOrdinaryFunctionInvoked()

        user.thenResponseIsCorrect()
    }

    @Test(expected = JsonDataException::class)
    fun testDecodingFailedForOrdinaryFunctionInBoxedByDefaultServiceWithOrdinaryServer() {
        val serviceToTest = givenServiceToTest(boxedByDefault = true)

        server.whenOrdinaryResponsesProvided()
        serviceToTest.whenOrdinaryFunctionInvoked()
    }

    @Test
    fun testUnitFunctionNotFails() {
        val serviceToTest = givenServiceToTest(boxedByDefault = false)

        server.whenOrdinaryResponsesProvided()
        var exceptionThrown = whenExceptionThrown { serviceToTest.whenUnitFunctionInvoked() }
        assert(!exceptionThrown) {
            """
            |Expect no exceptions being thrown by unit function invocation, got exception
            |for not boxed by default service with not boxed server
            """.trimMargin()
        }

        server.whenBoxedResponsesProvided()
        exceptionThrown = whenExceptionThrown { serviceToTest.whenUnitFunctionInvoked() }
        assert(!exceptionThrown) {
            """
            |Expect no exceptions being thrown by unit function invocation, got exception
            |for not boxed by default service with boxed server
            """.trimMargin()
        }
    }

    @Test
    fun testUnitFunctionNotFailsForBoxedByDefaultService() {
        val serviceToTest = givenServiceToTest(boxedByDefault = true)

        server.whenOrdinaryResponsesProvided()
        var exceptionThrown = whenExceptionThrown { serviceToTest.whenUnitFunctionInvoked() }
        assert(!exceptionThrown) {
            """
            |Expect no exceptions being thrown by unit function invocation, got exception
            |for boxed by default service with not boxed server
            """.trimMargin()
        }

        server.whenBoxedResponsesProvided()
        exceptionThrown = whenExceptionThrown { serviceToTest.whenUnitFunctionInvoked() }
        assert(!exceptionThrown) {
            """
            |Expect no exceptions being thrown by unit function invocation, got exception
            |for boxed by default service with boxed server
            """.trimMargin()
        }
    }

    private fun AuthorisationService.whenBoxedFunctionInvoked(): UserDTO {
        return runBlocking { signIn(SignInRequest("Andrii")) }
    }

    private fun AuthorisationService.whenNotBoxedFunctionInvoked(): UserDTO {
        return runBlocking { getUser("authorisation") }
    }

    private fun AuthorisationService.whenOrdinaryFunctionInvoked(): UserDTO {
        return runBlocking { deleteUser("authorisation") }
    }

    private fun AuthorisationService.whenUnitFunctionInvoked() {
        return runBlocking { signUp(SignUpRequest("Andrii")) }
    }

    private fun whenExceptionThrown(action: () -> Unit): Boolean {
        return try {
            action()
            false
        } catch (e: JsonDataException) {
            true
        }
    }

    private fun UserDTO.thenResponseIsCorrect() {
        val expectedUser = UserDTO(userId = 12, userName = "Andrii")
        assert(this == expectedUser) {
            "Unexpected response, expect: $expectedUser, got: $this"
        }
    }

    private fun givenServiceToTest(boxedByDefault: Boolean): AuthorisationService {
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(ResponseBoxingFactory(Moshi.Builder().build(), boxedByDefault))
            .build()
            .create(AuthorisationService::class.java)
    }

    private fun MockWebServer.whenBoxedResponsesProvided() {
        dispatcher = object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when(request.path!!) {
                    "/signIn", "/getUser", "/deleteUser" -> {
                        MockResponse().setBody(
                            """
                            |{ "data":
                            |   {
                            |       "userId": 12,
                            |       "userName": "Andrii"
                            |   }
                            |}""".trimMargin())
                    }
                    "/signUp" -> MockResponse()
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
    }

    private fun MockWebServer.whenOrdinaryResponsesProvided() {
        dispatcher = object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when(request.path!!) {
                    "/signIn", "/getUser", "/deleteUser" -> {
                        MockResponse().setBody(
                            """
                            |{
                            |   "userId": 12,
                            |   "userName": "Andrii"
                            |}""".trimMargin())
                    }
                    "/signUp" -> MockResponse()
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
    }

    companion object {
        @JsonClass(generateAdapter = true)
        class SignInRequest(val name: String)

        @JsonClass(generateAdapter = true)
        data class UserDTO(val userId: Int, val userName: String)

        @JsonClass(generateAdapter = true)
        class SignUpRequest(val name: String)

        interface AuthorisationService {

            @Boxed
            @POST("signIn")
            suspend fun signIn(@Body body: SignInRequest): UserDTO

            @GET("getUser")
            @NotBoxed
            suspend fun getUser(@Header("authorisation") authorisation: String): UserDTO

            @Boxed
            @POST("signUp")
            suspend fun signUp(@Body body: SignUpRequest)

            @POST("deleteUser")
            suspend fun deleteUser(@Header("authorisation") authorisation: String): UserDTO
        }
    }
}