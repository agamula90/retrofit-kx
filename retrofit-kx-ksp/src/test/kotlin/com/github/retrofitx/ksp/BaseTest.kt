package com.github.retrofitx.ksp

import com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider
import com.tschuchort.compiletesting.*
import com.github.retrofitx.ksp.utils.getDefaultDataTransferObjects
import com.github.retrofitx.ksp.utils.getDefaultError
import com.github.retrofitx.ksp.utils.getServiceName
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class BaseTest(val servicesPackage: String = "com.github.retrofitx.test.remote") {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()
    private val defaultError: String = getDefaultError()

    fun whenCompiled(
        services: List<String>,
        supplementoryFiles: List<SourceFile> =
            listOf(SourceFile.kotlin(name = "dto.kt", contents = getDefaultDataTransferObjects()))
    ): KotlinCompilation.Result {
        val serviceFiles = services.map {
            SourceFile.kotlin(name = it.getServiceName() + ".kt", contents = it)
        }
        val errorFile = SourceFile.kotlin(name = "Error.kt", contents = defaultError)

        return KotlinCompilation().apply {
            inheritClassPath = true
            symbolProcessorProviders = listOf(
                RetrofitXSymbolProcessorProvider(),
                JsonClassSymbolProcessorProvider()
            )
            messageOutputStream = System.out
            kspWithCompilation = true
            kspIncremental = true
            verbose = false
            kspArgs[KSP_ARG_SERVICES_PACKAGE] = servicesPackage
            workingDir = temporaryFolder.root
            sources = serviceFiles + errorFile + supplementoryFiles
        }.compile()
    }

    fun KotlinCompilation.Result.thenCompiledSuccessfully() {
        assert(exitCode == KotlinCompilation.ExitCode.OK) {
            "Compilation failed with code: $exitCode"
        }
        assert(messages.isEmpty()) {
            "Expect no messages logged, got $messages"
        }
    }
}