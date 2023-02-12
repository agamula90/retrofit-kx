package com.github.retrofitx.ksp

import com.github.retrofitx.ksp.utils.*
import com.tschuchort.compiletesting.*
import org.jetbrains.kotlin.descriptors.runtime.components.tryLoadClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RetrofitXConfigTest {

    @get:Rule val temporaryFolder: TemporaryFolder = TemporaryFolder()
    private val defaultError: String = getDefaultError()

    @Test
    fun testCompilationWithoutRequiredArgsFailed() {
        val servicesToTest = "com.github.test".getDefaultService()

        val compilationResult = whenCompiled(
            services = listOf(servicesToTest),
            servicesPackage = null,
            defaultError
        )

        assert(compilationResult.messages.contains("required args not found")) {
            "Compilation without required ksp args succeeded"
        }
    }

    @Test
    fun testCompilationWithBrokenServicePackageFailed() {
        val serviceToTest = "com.github.test".getDefaultService()

        val compilationResult = whenCompiled(
            services = listOf(serviceToTest),
            servicesPackage = "com..github.test",
            defaultError
        )

        assert(compilationResult.messages.contains("no services found")) {
            "Compilation with broken package succeeded"
        }
    }

    @Test
    fun testCompilationWithWrongServicePackageFailed() {
        val serviceToTest = "com.github.test".getDefaultService()

        val compilationResult = whenCompiled(
            services = listOf(serviceToTest),
            servicesPackage = "com.github.test2",
            defaultError
        )

        assert(compilationResult.messages.contains("no services found")) {
            "Compilation with empty services package succeeded"
        }
    }

    @Test
    fun testCompilationWithoutRetrofitErrorFailed() {
        val servicesPackage = "com.github.test"

        val compilationResult = whenCompiled(
            services = listOf(servicesPackage.getDefaultService()),
            servicesPackage = servicesPackage
        )

        assert(compilationResult.messages.contains("Expect one and only one RetrofitError being defined")) {
            "Compilation without retrofit error succeeded"
        }
    }

    @Test
    fun testCompilationWithMultipleRetrofitErrorsFailed() {
        val servicesPackage = "com.github.test"

        val compilationResult = whenCompiled(
            services = listOf(servicesPackage.getDefaultService()),
            servicesPackage = servicesPackage,
            defaultError,
            getSupplementaryError()
        )

        assert(compilationResult.messages.contains("Expect one and only one RetrofitError being defined")) {
            "Compilation with multiple retrofit errors succeeded"
        }
    }

    @Test
    fun testServiceNameParsedFromServiceContent() {
        val servicesPackage = "com.github.test"
        val services = listOf(
            servicesPackage.getDefaultService(),
            servicesPackage.getSupplementaryService()
        )

        val serviceNames = services.map { it.getServiceName() }

        serviceNames[0].assertServiceNameEquals("AuthorisationService")
        serviceNames[1].assertServiceNameEquals("ProductService")
    }

    private fun String.assertServiceNameEquals(expectedServiceName: String) {
        assert(this == expectedServiceName) {
            "Service name parsing failed. Expect service name = $expectedServiceName, got: $this"
        }
    }

    @Test
    fun testAllRequiredClassesGeneratedWhenRetrofitXConfiguredSuccessfully() {
        val servicesPackage = "com.github.test"
        val servicesToTest = listOf(servicesPackage.getDefaultService(), servicesPackage.getSupplementaryService())

        val compilationResult = whenCompiled(
            services = servicesToTest,
            servicesPackage = servicesPackage,
            defaultError
        )

        val internalServiceClasses = servicesToTest.map { "$INTERNAL_PACKAGE.$INTERNAL_SERVICE_PREFIX${it.getServiceName()}" }
        val retrofitXServiceClasses = servicesToTest.map { "$BASE_PACKAGE.${it.getServiceName()}" }
        val retrofitXClass = "$BASE_PACKAGE.RetrofitX"
        compilationResult.thenClassesGenerated(internalServiceClasses + retrofitXServiceClasses + retrofitXClass)
    }

    private fun whenCompiled(
        services: List<String>,
        servicesPackage: String?,
        vararg error: String = arrayOf(),
    ): KotlinCompilation.Result {
        val sourceFiles = mutableListOf<SourceFile>()
        sourceFiles.addAll(
            services.map {
                SourceFile.kotlin(name = it.getServiceName() + ".kt", contents = it)
            }
        )
        sourceFiles.addAll(
            error.mapIndexed { index, content ->
                SourceFile.kotlin(name = "Error$index.kt", contents = content)
            }
        )
        sourceFiles.add(SourceFile.kotlin(name = "dto.kt", contents = getDefaultDataTransferObjects()))

        return KotlinCompilation().apply {
            inheritClassPath = true
            symbolProcessorProviders = listOf(RetrofitXSymbolProcessorProvider())
            messageOutputStream = System.out
            kspWithCompilation = true
            kspIncremental = true
            if (servicesPackage != null) kspArgs[KSP_ARG_SERVICES_PACKAGE] = servicesPackage
            workingDir = temporaryFolder.root
            sources = sourceFiles
        }.compile()
    }

    private fun KotlinCompilation.Result.thenClassesGenerated(classes: List<String>) {
        assert(generatedFiles.isNotEmpty()) {
            "Nothing generated"
        }

        classes.forEach { classQualifiedName ->
            assert(classLoader.tryLoadClass(classQualifiedName) != null) {
                "Loaging class $classQualifiedName failed"
            }
        }
    }
}