package com.github.retrofitx.ksp

import com.squareup.kotlinpoet.ClassName

class ServiceMetadata(
    val serviceName: String,
    var errorClassName: ClassName,
    var baseUrl: String? = null,
    var boxed: Boolean? = null,
) {
    override fun equals(other: Any?): Boolean {
        return other is ServiceMetadata && other.serviceName == serviceName
    }

    override fun hashCode(): Int {
        return serviceName.hashCode()
    }
}