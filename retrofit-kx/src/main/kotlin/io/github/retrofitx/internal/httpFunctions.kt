/*
 * Copyright (C) 2023 Andrii Hamula
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.retrofitx.internal

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import io.github.retrofitx.UnitResponse
import io.github.retrofitx.ParseFailureException
import io.github.retrofitx.DataResponse
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException

inline fun <T, E> invokeDataFunction(
    retrofitFunction: () -> T,
    errorAdapter: JsonAdapter<E>
): DataResponse<T, E> = try {
    DataResponse.Success(retrofitFunction())
}
catch (e: HttpException) {
    val response = e.response()!!
    val code = response.code()
    try {
        val error = errorAdapter.fromJson(response.errorBody()!!.string())!!
        DataResponse.ApiError(error, code)
    } catch (e: JsonEncodingException) {
        throw ParseFailureException(isApiErrorParsingFailure = true)
    } catch (e: JsonDataException) {
        throw ParseFailureException(isApiErrorParsingFailure = true)
    } catch (e: EOFException) {
        throw ParseFailureException(isApiErrorParsingFailure = true)
    }
}
catch (e: JsonEncodingException) {
    throw ParseFailureException(isApiErrorParsingFailure = false)
}
catch (e: JsonDataException) {
    throw ParseFailureException(isApiErrorParsingFailure = false)
}
catch (e: IllegalArgumentException) {
    throw ParseFailureException(isApiErrorParsingFailure = false)
}
catch (e: EOFException) {
    throw ParseFailureException(isApiErrorParsingFailure = false)
}
catch (e: IOException) {
    DataResponse.ConnectionError(e)
}

inline fun <E> invokeUnitFunction(
    retrofitFunction: () -> Unit,
    errorAdapter: JsonAdapter<E>
): UnitResponse<E> = try {
    retrofitFunction()
    UnitResponse.Success()
}
catch (e: HttpException) {
    val response = e.response()!!
    val code = response.code()
    try {
        val error = errorAdapter.fromJson(response.errorBody()!!.string())!!
        UnitResponse.ApiError(error, code)
    } catch (e: JsonEncodingException) {
        throw ParseFailureException(isApiErrorParsingFailure = true)
    } catch (e: JsonDataException) {
        throw ParseFailureException(isApiErrorParsingFailure = true)
    } catch (e: EOFException) {
        throw ParseFailureException(isApiErrorParsingFailure = true)
    }
}
catch (e: IllegalArgumentException) {
    throw ParseFailureException(isApiErrorParsingFailure = false)
}
catch (e: EOFException) {
    throw ParseFailureException(isApiErrorParsingFailure = false)
}
catch (e: IOException) {
    UnitResponse.ConnectionError(e)
}