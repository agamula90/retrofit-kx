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
package com.github.retrofitx.internal

import retrofit2.Retrofit

class ServicesCache(private val retrofit: Retrofit) {
    private val services = HashMap<Class<*>, Any>()
    private val lock = Any()

    fun getServiceOrCreate(key: Class<*>): Any {
        // fast path. If key found, return service without any concurrency guards
        val value = services[key]
        if (value != null) return value
        synchronized(lock) {
            // long path. If key found, return cached service, else create + cache service
            var nullableService = services[key]
            if (nullableService == null) {
                nullableService = retrofit.create(key)
                services[key] = nullableService
            }
            return nullableService!!
        }
    }

    fun size(): Int {
        return services.size
    }
}