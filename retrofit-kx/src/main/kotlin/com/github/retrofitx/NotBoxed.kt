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
package com.github.retrofitx

/**
 *
 * This annotation used to override boxing behaviour per service / function
 *
 * For example if RetrofitX created with boxedByDefault = true, all services, created with this instance will be boxed.
 *
 * To exclude ProductService from boxing pattern, set [NotBoxed] annotation on it
 * ```
 * @NotBoxed
 * interface ProductService {
 *
 *      @POST("products")
 *      suspend fun getProducts(): List<Product>
 * }
 * ```
 *
 * See [Boxed] doc for:
 * - introduction of boxing term.
 * - boxing precedence
 *
 * @see Boxed
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class NotBoxed