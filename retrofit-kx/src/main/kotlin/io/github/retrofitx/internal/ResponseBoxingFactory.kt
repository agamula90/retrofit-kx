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

import com.squareup.moshi.*
import io.github.retrofitx.Boxed
import io.github.retrofitx.NotBoxed
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.Type

class ResponseBoxingFactory(
    private val moshi: Moshi,
    private val boxedByDefault: Boolean = false
) : Converter.Factory() {

    private val moshiConverterFactory = MoshiConverterFactory.create(moshi)

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        val shouldUnbox = annotations.any { it.annotationClass == Boxed::class } ||
                (boxedByDefault && annotations.all { it.annotationClass != NotBoxed::class })

        return when {
            shouldUnbox -> UnboxBodyConverter(moshi.adapter<Any>(type, emptySet()))
            else -> {
                moshiConverterFactory.responseBodyConverter(
                    type,
                    annotations.withoutRetrofitKxAnnotations(),
                    retrofit
                )
            }
        }
    }

    private fun Array<out Annotation>.withoutRetrofitKxAnnotations(): Array<out Annotation> {
        return filterNot { it.annotationClass in listOf(Boxed::class, NotBoxed::class) }.toTypedArray()
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        return moshiConverterFactory.requestBodyConverter(
            type,
            parameterAnnotations,
            methodAnnotations.withoutRetrofitKxAnnotations(),
            retrofit
        )
    }

    private inner class UnboxBodyConverter<T>(
        private val adapter: JsonAdapter<T>
    ) : Converter<ResponseBody, T> {

        override fun convert(value: ResponseBody): T {
            return value.use {
                val source = it.source()
                // Moshi has no document-level API so the responsibility of BOM skipping falls to whatever
                // is delegating to it. Since it's a UTF-8-only library as well we only honor the UTF-8 BOM.
                if (source.rangeEquals(0, UTF8_BOM)) {
                    source.skip(UTF8_BOM.size.toLong())
                }
                val reader = JsonReader.of(source)
                reader.beginObject()
                reader.skipName()
                val result: T = adapter.fromJson(reader)!!
                reader.endObject()
                if (reader.peek() != JsonReader.Token.END_DOCUMENT) {
                    throw JsonDataException("JSON document was not fully consumed.")
                }
                result
            }
        }
    }

    companion object {
        private val UTF8_BOM: ByteString = "EFBBBF".decodeHex()
    }
}