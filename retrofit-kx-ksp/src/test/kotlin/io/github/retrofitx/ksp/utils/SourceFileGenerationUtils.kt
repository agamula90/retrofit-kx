package io.github.retrofitx.ksp.utils

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.retrofitx.ksp.BASE_PACKAGE
import okhttp3.*
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.*
import okhttp3.mockwebserver.Dispatcher
import okio.Buffer
import okio.Timeout
import java.io.IOException
import java.net.Socket
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.reflect.KClass

fun getDefaultDataTransferObjects() = """
    package io.github.retrofitx.test.dto

    import com.squareup.moshi.JsonClass

    @JsonClass(generateAdapter = true)
    class SignInRequest(val name: String)

    @JsonClass(generateAdapter = true)
    class UserDTO(val userId: Int, val userName: String)

    @JsonClass(generateAdapter = true)
    class SignUpRequest(val name: String)
""".trimIndent()

fun String.getDefaultService() = """
    package $this

    import io.github.retrofitx.test.dto.SignInRequest
    import io.github.retrofitx.test.dto.SignUpRequest
    import io.github.retrofitx.test.dto.UserDTO
    import retrofit2.http.Body
    import retrofit2.http.GET
    import retrofit2.http.Header
    import retrofit2.http.POST
    import retrofit2.http.Url

    interface AuthorisationService {

        @POST("signIn")
        suspend fun signIn(@Body body: SignInRequest): UserDTO

        @POST("signOut")
        suspend fun signOut()

        @POST("signUp")
        suspend fun signUp(@Body body: SignUpRequest): UserDTO
        
        @GET("user")
        suspend fun getUser(@Header("authorisation") authorisation: String): UserDTO
        
        @GET
        suspend fun getSupportedMethods(@Url baseUrl: String): List<String>
    }
""".trimIndent()

fun String.getServiceWithRemoteOverridden() = """
    package $this

    import io.github.retrofitx.Remote
    import io.github.retrofitx.test.dto.SignInRequest
    import io.github.retrofitx.test.dto.SignUpRequest
    import io.github.retrofitx.test.dto.UserDTO
    import retrofit2.http.Body
    import retrofit2.http.GET
    import retrofit2.http.Header
    import retrofit2.http.POST
    import retrofit2.http.Url

    @Remote(url = "https://google.com/")
    interface AuthorisationService {

        @POST("signIn")
        suspend fun signIn(@Body body: SignInRequest): UserDTO

        @POST("signOut")
        suspend fun signOut()

        @POST("signUp")
        suspend fun signUp(@Body body: SignUpRequest): UserDTO
        
        @GET("user")
        suspend fun getUser(@Header("authorisation") authorisation: String): UserDTO
        
        @GET
        suspend fun getSupportedMethods(@Url baseUrl: String): List<String>
    }
""".trimIndent()

fun String.getServiceWithErrorOverridden() = """
    package $this

    import io.github.retrofitx.Remote
    import io.github.retrofitx.test.dto.SignInRequest
    import io.github.retrofitx.test.dto.SignUpRequest
    import io.github.retrofitx.test.dto.UserDTO
    import retrofit2.http.Body
    import retrofit2.http.GET
    import retrofit2.http.Header
    import retrofit2.http.POST
    import retrofit2.http.Url
    import com.squareup.moshi.JsonClass

    @JsonClass(generateAdapter = true)
    class GoogleError(val errorId: Int)

    @Remote(url = "https://google.com/", error = GoogleError::class)
    interface AuthorisationServiceWithError {

        @POST("signIn")
        suspend fun signIn(@Body body: SignInRequest): UserDTO

        @POST("signOut")
        suspend fun signOut()

        @POST("signUp")
        suspend fun signUp(@Body body: SignUpRequest): UserDTO
        
        @GET("user")
        suspend fun getUser(@Header("authorisation") authorisation: String): UserDTO
        
        @GET
        suspend fun getSupportedMethods(@Url baseUrl: String): List<String>
    }
""".trimIndent()

fun String.getServiceWithAbsoluteUrl() = """
    package $this

    import io.github.retrofitx.Remote
    import io.github.retrofitx.test.dto.SignInRequest
    import io.github.retrofitx.test.dto.UserDTO
    import io.github.retrofitx.test.dto.SignUpRequest
    import retrofit2.http.Body
    import retrofit2.http.POST
    import com.squareup.moshi.JsonClass

    @JsonClass(generateAdapter = true)
    class GoogleError(val errorId: Int)

    @Remote(url = "https://google.com/", error = GoogleError::class)
    interface AuthorisationService {

        @POST("signIn")
        suspend fun signIn(@Body body: SignInRequest): UserDTO

        @POST("https://facebook.com/signUp")
        suspend fun signUp(@Body body: SignUpRequest): UserDTO
    }
""".trimIndent()

fun String.getServiceWithBoxedFunction() = """
    package $this

    import io.github.retrofitx.test.dto.SignInRequest
    import io.github.retrofitx.test.dto.SignUpRequest
    import io.github.retrofitx.test.dto.UserDTO
    import io.github.retrofitx.Boxed
    import retrofit2.http.Body
    import retrofit2.http.GET
    import retrofit2.http.Header
    import retrofit2.http.POST
    import retrofit2.http.Url

    interface AuthorisationService {

        @POST("signIn")
        suspend fun signIn(@Body body: SignInRequest): UserDTO

        @POST("signOut")
        suspend fun signOut()

        @POST("signUp")
        suspend fun signUp(@Body body: SignUpRequest): UserDTO
        
        @GET("user")
        @Boxed
        suspend fun getUser(@Header("authorisation") authorisation: String): UserDTO
        
        @GET
        suspend fun getSupportedMethods(@Url baseUrl: String): List<String>
    }
""".trimIndent()

fun String.getServiceWithNotBoxedFunction() = """
    package $this

    import io.github.retrofitx.test.dto.SignInRequest
    import io.github.retrofitx.test.dto.SignUpRequest
    import io.github.retrofitx.test.dto.UserDTO
    import io.github.retrofitx.NotBoxed
    import retrofit2.http.Body
    import retrofit2.http.GET
    import retrofit2.http.Header
    import retrofit2.http.POST
    import retrofit2.http.Url

    interface AuthorisationService {

        @POST("signIn")
        suspend fun signIn(@Body body: SignInRequest): UserDTO

        @POST("signOut")
        suspend fun signOut()

        @POST("signUp")
        suspend fun signUp(@Body body: SignUpRequest): UserDTO
        
        @GET("user")
        @NotBoxed
        suspend fun getUser(@Header("authorisation") authorisation: String): UserDTO
        
        @GET
        suspend fun getSupportedMethods(@Url baseUrl: String): List<String>
    }
""".trimIndent()

fun String.getNotBoxedService() = """
    package $this

    import io.github.retrofitx.test.dto.SignInRequest
    import io.github.retrofitx.test.dto.SignUpRequest
    import io.github.retrofitx.test.dto.UserDTO
    import io.github.retrofitx.Boxed
    import io.github.retrofitx.NotBoxed
    import retrofit2.http.Body
    import retrofit2.http.GET
    import retrofit2.http.Header
    import retrofit2.http.POST
    import retrofit2.http.Url

    @NotBoxed
    interface AuthorisationServiceNotBoxed {

        @POST("signIn")
        suspend fun signIn(@Body body: SignInRequest): UserDTO

        @POST("signOut")
        suspend fun signOut()

        @POST("signUp")
        suspend fun signUp(@Body body: SignUpRequest): UserDTO
        
        @GET("user")
        @Boxed
        suspend fun getUser(@Header("authorisation") authorisation: String): UserDTO
        
        @GET
        suspend fun getSupportedMethods(@Url baseUrl: String): List<String>
    }
""".trimIndent()

fun String.getBoxedService() = """
    package $this

    import io.github.retrofitx.test.dto.SignInRequest
    import io.github.retrofitx.test.dto.SignUpRequest
    import io.github.retrofitx.test.dto.UserDTO
    import io.github.retrofitx.Boxed
    import io.github.retrofitx.NotBoxed
    import retrofit2.http.Body
    import retrofit2.http.GET
    import retrofit2.http.Header
    import retrofit2.http.POST
    import retrofit2.http.Url

    @Boxed
    interface AuthorisationServiceBoxed {

        @POST("signIn")
        suspend fun signIn(@Body body: SignInRequest): UserDTO

        @POST("signOut")
        suspend fun signOut()

        @POST("signUp")
        suspend fun signUp(@Body body: SignUpRequest): UserDTO
        
        @GET("user")
        @NotBoxed
        suspend fun getUser(@Header("authorisation") authorisation: String): UserDTO
        
        @GET
        suspend fun getSupportedMethods(@Url baseUrl: String): List<String>
    }
""".trimIndent()

fun getDefaultError() = """
    package io.github.retrofitx.test.dto

    import com.squareup.moshi.JsonClass
    import io.github.retrofitx.RetrofitError

    @JsonClass(generateAdapter = true)
    @RetrofitError
    class Error(val cause: String)
""".trimIndent()

fun getSupplementaryError() = """
    package io.github.retrofitx.test.dto

    import com.squareup.moshi.JsonClass
    import io.github.retrofitx.RetrofitError

    @JsonClass(generateAdapter = true)
    @RetrofitError
    class SupplementaryError(val cause: String)
""".trimIndent()

fun String.getSupplementaryService() = """
    package $this
    
    import retrofit2.http.Body
    import retrofit2.http.GET
    import retrofit2.http.POST
    import com.squareup.moshi.JsonClass

    @JsonClass(generateAdapter = true)
    class AddProductRequest(val name: String)

    @JsonClass(generateAdapter = true)
    class ProductDTO(val id: Long, val name: String)
    
    interface ProductService {
        
        @GET("products")
        suspend fun getProducts(): List<ProductDTO>
        
        @POST("addProduct")
        suspend fun addProduct(@Body body: AddProductRequest): ProductDTO
    }
""".trimIndent()

fun KotlinCompilation.Result.getErrorClass(): KClass<out Any> {
    return classLoader.loadClass("io.github.retrofitx.test.dto.Error").kotlin
}

fun KotlinCompilation.Result.getRetrofitXClass(): KClass<out Any> {
    return classLoader.loadClass("$BASE_PACKAGE.RetrofitX").kotlin
}

fun String.getServiceName(): String {
    val serviceNamePattern = Pattern.compile(".*interface ([A-Z]+[A-Za-z0-9]*) .*", Pattern.DOTALL)
    val serviceNameMatcher = serviceNamePattern.matcher(this)
    assert(serviceNameMatcher.matches()) {
        "Can't parse retrofitx service name"
    }
    return serviceNameMatcher.group(1)
}

fun Dispatcher.toCallFactory() = object: Call.Factory {
    override fun newCall(request: Request): Call = MockCall(request)

    private inner class MockCall(private val okHttpRequest: Request): Call {
        private val isCanceled = AtomicBoolean()
        private val isExecuted = AtomicBoolean()

        override fun cancel() {
            isCanceled.compareAndSet(false, true)
        }

        override fun enqueue(responseCallback: Callback) {
            try {
                val response = execute()
                responseCallback.onResponse(this, response)
            } catch (e: IOException) {
                responseCallback.onFailure(this, e)
            }
        }

        @Throws(IOException::class)
        override fun execute(): Response {
            isExecuted.compareAndSet(false, true)
            val body = okHttpRequest.body
            val bodyBuffer = Buffer()
            body?.writeTo(bodyBuffer)
            val recordedRequest = RecordedRequest(
                requestLine = okHttpRequest.method + " " + okHttpRequest.url.encodedPath + " ",
                headers = okHttpRequest.headers,
                chunkSizes = emptyList(),
                bodySize = bodyBuffer.size,
                body = bodyBuffer,
                sequenceNumber = 0,
                socket = Socket()
            )
            val response = dispatch(recordedRequest)
            if (response.socketPolicy == SocketPolicy.DISCONNECT_AFTER_REQUEST) {
                throw IOException("Connection error")
            }
            val responseBody = response.getBody()?.readString(Charset.defaultCharset())?.toResponseBody()
            val code = response.status.split(" ")[1].toInt()

            return Response.Builder()
                .headers(response.headers)
                .code(code)
                .request(okHttpRequest)
                .body(responseBody ?: Buffer().asResponseBody())
                .protocol(Protocol.HTTP_2)
                .message(response.status)
                .build()
        }

        override fun clone(): Call = MockCall(okHttpRequest)
        override fun isCanceled(): Boolean = isCanceled.get()
        override fun isExecuted(): Boolean = isExecuted.get()
        override fun request(): Request = okHttpRequest
        override fun timeout(): Timeout = Timeout.NONE.timeout(5, TimeUnit.SECONDS)
    }
}