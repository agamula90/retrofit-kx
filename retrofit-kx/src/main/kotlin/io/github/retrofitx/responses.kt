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
package io.github.retrofitx

import java.io.IOException

/**
 * This exception will be thrown when success response parsing failed or api error parsing failed
 *
 * @see [DataResponse]
 * @see [UnitResponse]
 */
class ParseFailureException(isApiErrorParsingFailure: Boolean) : RuntimeException("[isApiErrorParsingFailure:$isApiErrorParsingFailure]")

/**
 * This class is wrapper around json response with data.
 * - Success contains parsed response value if request succeeded
 * - ConnectionError contains [IOException] that caused request to fail
 * - ApiError contains instance of error object that caused request to fail
 */
sealed class DataResponse<out T, out E> {
    class Success<T, E>(val data: T): DataResponse<T, E>()
    class ConnectionError<T, E>(val cause: IOException): DataResponse<T, E>()
    class ApiError<T, E>(val cause: E, val errorCode: Int): DataResponse<T, E>()
}

/**
 * This class is wrapper around json response with no data.
 * - Success - used when request execution succeeded
 * - ConnectionError contains [IOException] that caused request to fail
 * - ApiError contains instance of error object that caused request to fail
 */
sealed class UnitResponse<out E> {
    class Success<E>: UnitResponse<E>()
    class ConnectionError<E>(val cause: IOException): UnitResponse<E>()
    class ApiError<E>(val cause: E, val errorCode: Int): UnitResponse<E>()
}