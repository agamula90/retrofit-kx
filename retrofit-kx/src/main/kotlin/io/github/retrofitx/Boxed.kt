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

/**
 * Boxing in terms of retrofit-kx means that response contains redundant wrapper over useful data.
 *
 * If server returns
 *
 * {"products": `[`{"id": 123, "name": "Product1"}, {"id": 124, "name": "Product2"}`]`}
 *
 * with retrofit you'll need 2 classes:
 *
 * ```
 * class Product(val id: Int, val name: String)
 * class ProductsWrapper(val products: List<Product>)
 *
 * and api
 *
 * @POST("products")
 * suspend fun getProducts(): ProductsWrapper
 * ```
 * To get rid of ProductsWrapper use [Boxed] annotation with getProducts function:
 * ```
 * @POST("products")
 * @Boxed
 * suspend fun getProducts(): List<Product>
 * ```
 *
 * Boxing precedence:
 *
 * - You can create RetrofitX with boxedByDefault = true to mark all services boxed
 * - You can override RetrofitX boxing with [Boxed] annotation set on specific service
 * - You can override service boxing with [Boxed] annotation set on specific function
 *
 * Use [NotBoxed] if you want to exclude service / function from boxed pattern.
 *
 * @see NotBoxed
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Boxed