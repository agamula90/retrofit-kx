package com.github.retrofitx.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.github.retrofitx.Boxed
import com.github.retrofitx.NotBoxed
import com.github.retrofitx.ksp.utils.*
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.valueParameters

class InternalServiceTest: BaseTest() {

    @Test
    fun testRemoteUrlAppliedToEachFunctionWithoutUrlParameter() {
        val serviceToTest = servicesPackage.getServiceWithRemoteOverridden()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.thenCompiledSuccessfully()
        compilationResult.thenRemoteUrlAppliedOnlyForFunctions(
            userServiceName = serviceToTest.getServiceName(),
            functions = listOf("signIn", "signOut", "signUp", "getUser")
        )
    }

    @Test
    fun testRemoteUrlNotAppliedToFunctionsWithAbsoluteUrls() {
        val serviceToTest = servicesPackage.getServiceWithAbsoluteUrl()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.thenCompiledSuccessfully()
        compilationResult.thenRemoteUrlAppliedOnlyForFunctions(
            userServiceName = serviceToTest.getServiceName(),
            functions = listOf("signIn")
        )
    }

    @Test
    fun testServiceMethodsByDefaultHaveNoBoxAnnotations() {
        val serviceToTest = servicesPackage.getDefaultService()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.thenCompiledSuccessfully()
        val serviceClass = compilationResult.getInternalServiceClass(serviceToTest.getServiceName())
        val allFunctionNames = serviceClass.declaredMemberFunctions.map { it.name }
        serviceClass.thenFunctionsHaveNoBoxAnnotations(allFunctionNames)
    }

    @Test
    fun testUserServiceAnnotationsAreSameAsInternalServiceAnnotationsIfUserServiceDoesNotHaveBoxAnnotations() {
        val serviceToTest = servicesPackage.getServiceWithBoxedFunction()

        val compilationResult = whenCompiled(services = listOf(serviceToTest))

        compilationResult.thenCompiledSuccessfully()
        val serviceClass = compilationResult.getInternalServiceClass(serviceToTest.getServiceName())
        val allFunctionNames = serviceClass.declaredMemberFunctions.map { it.name }
        val functionsWithBoxedAnnotation = listOf("getUser")
        serviceClass.thenBoxedAppliedForFunctions(functions = functionsWithBoxedAnnotation)
        serviceClass.thenFunctionsHaveNoBoxAnnotations(allFunctionNames.filterNot { functionsWithBoxedAnnotation.contains(it) })
    }

    @Test
    fun testBoxAnnotationsFromUserServicePropagatesToInternalServiceFunctions() {
        val boxedService = servicesPackage.getBoxedService()
        val notBoxedService = servicesPackage.getNotBoxedService()

        val compilationResult = whenCompiled(services = listOf(boxedService, notBoxedService))

        compilationResult.thenCompiledSuccessfully()
        val boxedServiceClass = compilationResult.getInternalServiceClass(boxedService.getServiceName())
        var allFunctionNames = boxedServiceClass.declaredMemberFunctions.map { it.name }
        val functionsWithOwnAnnotation = listOf("getUser")
        boxedServiceClass.thenBoxedAppliedForFunctions(
            functions = allFunctionNames.filterNot { functionsWithOwnAnnotation.contains(it) }
        )
        boxedServiceClass.thenNotBoxedAppliedForFunctions(functionsWithOwnAnnotation)

        val notBoxedServiceClass = compilationResult.getInternalServiceClass(notBoxedService.getServiceName())
        allFunctionNames = notBoxedServiceClass.declaredMemberFunctions.map { it.name }
        notBoxedServiceClass.thenNotBoxedAppliedForFunctions(
            functions = allFunctionNames.filterNot { functionsWithOwnAnnotation.contains(it) }
        )
        notBoxedServiceClass.thenBoxedAppliedForFunctions(functions = functionsWithOwnAnnotation)
    }

    private fun KotlinCompilation.Result.thenRemoteUrlAppliedOnlyForFunctions(
        userServiceName: String,
        functions: List<String>
    ) {
        val internalServiceName = "$INTERNAL_PACKAGE.$INTERNAL_SERVICE_PREFIX${userServiceName}"

        val userServiceClass = classLoader.loadClass("$servicesPackage.$userServiceName").kotlin
        val internalServiceClass = classLoader.loadClass(internalServiceName).kotlin

        assert(userServiceClass.declaredMemberFunctions.size == internalServiceClass.declaredMemberFunctions.size) {
            """
            |Expect both original and internal services to have same functions count, got 
            |count of functions for original service: ${userServiceClass.declaredMemberFunctions.size}
            |count of functions for internal service: ${internalServiceClass.declaredMemberFunctions.size}
            """.trimMargin()
        }

        val functionsForRemoteOverride = userServiceClass.declaredMemberFunctions.filter { it.name in functions }
        val functionsForCopy = userServiceClass.declaredMemberFunctions.filterNot { it in functionsForRemoteOverride }

        for (function in functionsForRemoteOverride) {
            val internalFunction = internalServiceClass.getDeclaredMemberFunctionByTemplate(function)
            for (i in function.valueParameters.indices) {
                function.valueParameters[i].assertEquals(internalFunction.valueParameters[i])
            }
            function.returnType.assertReturnTypeEquals(internalFunction.returnType)
            function.annotations.assertEqualsWithoutPathPrefix(internalFunction.annotations, "https://google.com/")
        }

        for (function in functionsForCopy) {
            val internalFunction = internalServiceClass.getDeclaredMemberFunctionByTemplate(function)
            for (i in function.valueParameters.indices) {
                function.valueParameters[i].assertEquals(internalFunction.valueParameters[i])
            }
            function.returnType.assertReturnTypeEquals(internalFunction.returnType)
            function.annotations.assertEqualsWithoutPathPrefix(internalFunction.annotations)
        }
    }

    private fun KotlinCompilation.Result.getInternalServiceClass(userServiceName: String): KClass<out Any> {
        val internalServiceName = "$INTERNAL_PACKAGE.$INTERNAL_SERVICE_PREFIX${userServiceName}"
        return classLoader.loadClass(internalServiceName).kotlin
    }

    private fun KClass<*>.thenFunctionsHaveNoBoxAnnotations(
        functions: List<String>
    ) {
        val filteredFunctions = declaredMemberFunctions.filter { functions.contains(it.name) }

        for (function in filteredFunctions) {
            val boxingAnnotations = function.annotations.filter { it is Boxed || it is NotBoxed }
            assert(boxingAnnotations.isEmpty()) {
                """
                |Expect functions to have no boxing annotations, got
                |but function ${function.name} has ${boxingAnnotations.size} boxing annotations
                """.trimMargin()
            }
        }
    }

    private fun KClass<*>.thenBoxedAppliedForFunctions(
        functions: List<String>
    ) {
        val filteredFunctions = declaredMemberFunctions.filter { functions.contains(it.name) }

        for (function in filteredFunctions) {
            val boxingAnnotations = function.annotations.filterIsInstance<Boxed>()
            assert(boxingAnnotations.size == 1) {
                "Expect Boxed annotation on ${function.name}, but it's not found"
            }
        }
    }

    private fun KClass<*>.thenNotBoxedAppliedForFunctions(
        functions: List<String>
    ) {
        val filteredFunctions = declaredMemberFunctions.filter { functions.contains(it.name) }

        for (function in filteredFunctions) {
            val boxingAnnotations = function.annotations.filterIsInstance<NotBoxed>()
            assert(boxingAnnotations.size == 1) {
                "Expect NotBoxed annotation on ${function.name}, but it's not found"
            }
        }
    }
}