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

import kotlin.reflect.KClass

/**
 * Retrofit doesn't allow to override base url, so retrofit-kx introduce this annotation to fix this behavior.
 *
 * You can use this annotation to override baseurl per service, like this:
 * ```
 * @Remote(url = "https://google.com/")
 * interface ProductService {
 *
 *      @POST("products")
 *      suspend fun getProducts(): List<Product>
 * }
 * ```
 *
 * For such declaration get products will use "https://google.com/products" url with getProducts function no matter which base url was set on RetrofitX.
 *
 * You can override error class per remote if error for such remote has other format, than default RetrofitX error.
 *
 * ```
 * class GoogleError(val errorId: Int)
 *
 * @Remote(url = "https://google.com/", error = GoogleError::class)
 * interface ProductService {
 *
 *      @POST("products")
 *      suspend fun getProducts(): List<Product>
 * }
 * ```
 *
 * See [RetrofitError] on how to specify default RetrofitX error
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
annotation class Remote(val url: String, val error: KClass<*> = Unit::class)
