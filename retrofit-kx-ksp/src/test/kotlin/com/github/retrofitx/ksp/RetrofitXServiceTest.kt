package com.github.retrofitx.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.github.retrofitx.DataResponse
import com.github.retrofitx.UnitResponse
import com.github.retrofitx.ksp.utils.*
import org.junit.Test
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberFunctions

class RetrofitXServiceTest: BaseTest() {

    @Test
    fun testServiceStructureIsCorrect() {
        val serviceToTest = servicesPackage.getDefaultService()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.thenCompiledSuccessfully()
        compilationResult.thenRetrofitXServiceHasExpectedFunctionsCount(
            userServiceName = serviceToTest.getServiceName()
        )
        compilationResult.thenEachUserFunctionHasMappingInRetrofitXService(
            userServiceName = serviceToTest.getServiceName()
        )
    }

    @Test
    fun testServiceFunctionsAreParameterisedByDefaultError() {
        val serviceToTest = servicesPackage.getDefaultService()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.assertServiceFunctionReturnTypesAreParameterisedByErrorClassName(
            userServiceName = serviceToTest.getServiceName(),
            errorClassName = "com.github.retrofitx.test.dto.Error"
        )
    }

    @Test
    fun testServiceFunctionsAreParameterisedByDefaultErrorWhenRemoteErrorNotSet() {
        val serviceToTest = servicesPackage.getServiceWithRemoteOverridden()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.assertServiceFunctionReturnTypesAreParameterisedByErrorClassName(
            userServiceName = serviceToTest.getServiceName(),
            errorClassName = "com.github.retrofitx.test.dto.Error"
        )
    }

    @Test
    fun testServiceFunctionsAreParameterisedByRemoteErrorWhenRemoteErrorSet() {
        val serviceToTest = servicesPackage.getServiceWithErrorOverridden()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.assertServiceFunctionReturnTypesAreParameterisedByErrorClassName(
            userServiceName = serviceToTest.getServiceName(),
            errorClassName = "$servicesPackage.GoogleError"
        )
    }

    private fun KotlinCompilation.Result.thenRetrofitXServiceHasExpectedFunctionsCount(
        userServiceName: String
    ) {
        val expectedFunctionsCount = getExpectedFunctionsCountInRetrofitXService(userServiceName)
        val retrofitXServiceName = "$BASE_PACKAGE.$userServiceName"
        val retrofitXServiceClass = classLoader.loadClass(retrofitXServiceName).kotlin

        assert(retrofitXServiceClass.declaredMemberFunctions.size == expectedFunctionsCount) {
            """
            |Expect $retrofitXServiceName to have $expectedFunctionsCount functions,
            |but it has ${retrofitXServiceClass.declaredMemberFunctions.size}
            """.trimMargin()
        }
    }

    private fun KotlinCompilation.Result.getExpectedFunctionsCountInRetrofitXService(
        userServiceName: String
    ): Int {
        val userServiceClass = classLoader.loadClass("$servicesPackage.$userServiceName").kotlin
        val (successOrientedFuns, resultOrientedFuns) = userServiceClass.declaredMemberFunctions.partition {
            it.returnType.toString() == "kotlin.Unit"
        }
        return successOrientedFuns.size * 2 + resultOrientedFuns.size
    }

    private fun KotlinCompilation.Result.thenEachUserFunctionHasMappingInRetrofitXService(
        userServiceName: String
    ) {
        val userServiceClass = classLoader.loadClass("$servicesPackage.$userServiceName").kotlin
        val (successOrientedFuns, resultOrientedFuns) = userServiceClass.declaredMemberFunctions.partition {
            it.returnType.toString() == "kotlin.Unit"
        }
        val retrofitXServiceClass = classLoader.loadClass("$BASE_PACKAGE.$userServiceName").kotlin

        // key = retrofitX function, value = user function name
        val processedRetrofitXFunctions = mutableMapOf<KFunction<*>, String>()
        for (resultOrientedFun in resultOrientedFuns) {
            val retrofitXServiceFunction = retrofitXServiceClass.getDeclaredMemberFunctionByTemplate(
                function = resultOrientedFun,
                ignoreAnnotations = true
            )
            retrofitXServiceFunction.returnType.assertIsGenericWithParameterTypes(
                className = DataResponse::class.qualifiedName!!,
                parameterTypes = mapOf("T" to resultOrientedFun.returnType)
            )
            processedRetrofitXFunctions.markAsProcessed(retrofitXServiceFunction, resultOrientedFun.name)
        }

        for (successOrientedFun in successOrientedFuns) {
            val retrofitXServiceFunction = retrofitXServiceClass.getDeclaredMemberFunctionByTemplate(
                function = successOrientedFun,
                ignoreAnnotations = true
            )
            retrofitXServiceFunction.returnType.assertIsGenericWithParameterTypes(
                className = UnitResponse::class.qualifiedName!!
            )

            val safeFunction = retrofitXServiceClass.getDeclaredMemberFunctionByTemplate(
                function = successOrientedFun,
                ignoreAnnotations = true,
                name = { it.name + "Safe" }
            )
            assert(safeFunction.returnType.toString() == "kotlin.Unit") {
                """
                |Expect safe version of function to have Unit type as result, but
                |got ${safeFunction.returnType} return type for ${safeFunction.name} function
                """.trimMargin()
            }
            processedRetrofitXFunctions.markAsProcessed(retrofitXServiceFunction, successOrientedFun.name)
            processedRetrofitXFunctions.markAsProcessed(safeFunction, successOrientedFun.name)
        }
    }

    private fun MutableMap<KFunction<*>, String>.markAsProcessed(function: KFunction<*>, userFunctionName: String) {
        val previousUserFunctionName = putIfAbsent(function, userFunctionName)
        if (previousUserFunctionName != null) {
            assert(false) {
                """
                |Expect only one function from user service to be mapped to retrofitX service function, but
                |${function.name} mapped from $userFunctionName and $previousUserFunctionName
                """.trimMargin()
            }
        }
    }

    private fun KotlinCompilation.Result.assertServiceFunctionReturnTypesAreParameterisedByErrorClassName(
        userServiceName: String,
        errorClassName: String
    ) {
        val userServiceClass = classLoader.loadClass("$servicesPackage.$userServiceName").kotlin
        val (successOrientedFuns, resultOrientedFuns) = userServiceClass.declaredMemberFunctions.partition {
            it.returnType.toString() == "kotlin.Unit"
        }
        val retrofitXServiceClass = classLoader.loadClass("$BASE_PACKAGE.$userServiceName").kotlin
        val errorType = classLoader.loadClass(errorClassName).kotlin.createType()

        for (successOrientedFun in successOrientedFuns) {
            val retrofitXServiceFunction = retrofitXServiceClass.getDeclaredMemberFunctionByTemplate(
                function = successOrientedFun,
                ignoreAnnotations = true
            )
            retrofitXServiceFunction.returnType.assertIsGenericWithParameterTypes(
                className = UnitResponse::class.qualifiedName!!,
                parameterTypes = mapOf("E" to errorType)
            )
        }

        for (resultOrientedFun in resultOrientedFuns) {
            val retrofitXServiceFunction = retrofitXServiceClass.getDeclaredMemberFunctionByTemplate(
                function = resultOrientedFun,
                ignoreAnnotations = true
            )
            retrofitXServiceFunction.returnType.assertIsGenericWithParameterTypes(
                className = DataResponse::class.qualifiedName!!,
                parameterTypes = mapOf("E" to errorType)
            )
        }
    }
}