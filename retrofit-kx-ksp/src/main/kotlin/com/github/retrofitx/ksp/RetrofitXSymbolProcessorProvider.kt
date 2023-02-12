package com.github.retrofitx.ksp

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider
import com.github.retrofitx.Boxed
import com.github.retrofitx.NotBoxed
import com.github.retrofitx.Remote
import com.github.retrofitx.RetrofitError
import retrofit2.http.*
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal const val KSP_ARG_SERVICES_PACKAGE = "servicesPackage"
internal const val RETROFIT_PACKAGE = "retrofit2.http"
internal const val BASE_PACKAGE = "com.github.retrofitx"
internal const val INTERNAL_SERVICE_PREFIX = "Retrofit"
internal const val INTERNAL_PACKAGE = "$BASE_PACKAGE.internal"
internal val PATH_ANNOTATIONS = listOf(GET::class, POST::class, PATCH::class, HEAD::class, DELETE::class, OPTIONS::class, PUT::class)

class RetrofitXSymbolProcessorProvider: SymbolProcessorProvider {
    private val moshiSymbolProcessorProvider = JsonClassSymbolProcessorProvider()

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val servicePackage = environment.options[KSP_ARG_SERVICES_PACKAGE]
        if (servicePackage == null) {
            environment.logger.error("required args not found")
        }
        return ChainSymbolProcessor(
            processors = listOf(
                moshiSymbolProcessorProvider.create(environment),
                ServicesSymbolProcessor(
                    servicesPackage = servicePackage.orEmpty(),
                    codeGenerator = environment.codeGenerator,
                    logger = environment.logger
                )
            )
        )
    }
}

private class ServicesSymbolProcessor(
    private val servicesPackage: String,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
): SymbolProcessor {

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        try {
            val userServices = resolver.getDeclarationsFromPackage(servicesPackage)
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.INTERFACE }

            val internalServicesDeclaration = resolver.getDeclarationsFromPackage(INTERNAL_PACKAGE)
                .filter { it.containingFile?.fileName == "services.kt" }
                .firstOrNull()
            if (internalServicesDeclaration != null) return emptyList()

            val errorClassSymbols = resolver.getSymbolsWithAnnotation(RetrofitError::class.qualifiedName!!).toList()
            if (errorClassSymbols.size != 1) {
                throw ProcessingFailureException("Expect one and only one RetrofitError being defined")
            }
            val errorClassDeclaration = errorClassSymbols[0] as? KSClassDeclaration
                ?: throw ProcessingFailureException(
                    message = "Expect class being annotation with RetrofitError annotation"
                )
            val errorClassName = errorClassDeclaration.toClassName()
            val errorClasses = mutableSetOf<String>()
            val errorClassFiles = mutableListOf<KSFile>().apply { add(errorClassDeclaration.containingFile!!) }

            val internalServicesFileBuilder = FileSpec.builder(
                packageName = INTERNAL_PACKAGE,
                fileName = "services"
            )
            val retrofitXServiceDeclarations = mutableListOf<TypeSpec>()
            val userDefinedFiles = mutableListOf<KSFile>()
            val metadatas = mutableListOf<ServiceMetadata>()

            userServicesLoop@ for (userDefinedService in userServices) {
                val userDeclaredFunctions = userDefinedService.getDeclaredFunctions()
                    .filter { it.modifiers.contains(Modifier.SUSPEND) }
                val userFunctionAnnotations = mutableMapOf<KSFunctionDeclaration, Sequence<AnnotationSpec>>()
                for (function in userDeclaredFunctions) {
                    val annotationSpecs = function.annotations.map { it.toAnnotationSpec() }
                    if (annotationSpecs.hasNoHttpAnnotation()) {
                        continue@userServicesLoop
                    }
                    userFunctionAnnotations[function] = annotationSpecs
                }

                val serviceMetadata = userDefinedService.toServiceMetadata(errorClassName)
                errorClasses.add(serviceMetadata.errorClassName.canonicalName)
                metadatas.add(serviceMetadata)
                userDefinedFiles.add(userDefinedService.containingFile!!)

                val internalServices = createInternalServices(
                    userDeclaredFunctions = userDeclaredFunctions,
                    userFunctionAnnotations = userFunctionAnnotations,
                    metadata = serviceMetadata
                )
                internalServicesFileBuilder.addType(typeSpec = internalServices)

                val retrofitXServiceDeclaration = internalServices.toRetrofitXServiceDeclaration(
                    metadata = serviceMetadata
                )
                retrofitXServiceDeclarations.add(retrofitXServiceDeclaration)
                internalServicesFileBuilder.addType(retrofitXServiceDeclaration.toRetrofitXServiceImplementations(
                    metadata = serviceMetadata
                ))
            }

            if (retrofitXServiceDeclarations.isEmpty()) {
                throw ProcessingFailureException("no services found")
            }

            internalServicesFileBuilder.apply {
                addImport(BASE_PACKAGE, retrofitXServiceDeclarations.map { it.name!! })
            }.writeTo(
                out = codeGenerator.createNewFile(
                    dependencies = Dependencies(
                        aggregating = true,
                        sources = userDefinedFiles.toTypedArray()
                    ),
                    packageName = INTERNAL_PACKAGE,
                    fileName = "services",
                    extensionName = "kt"
                )
            )

            FileSpec.builder(packageName = BASE_PACKAGE, fileName = "services")
                .apply { retrofitXServiceDeclarations.forEach(::addType) }
                .writeTo(
                    out = codeGenerator.createNewFile(
                        dependencies = Dependencies(
                            aggregating = true,
                            sources = userDefinedFiles.toTypedArray()
                        ),
                        packageName = BASE_PACKAGE,
                        fileName = "services",
                        extensionName = "kt"
                    )
                )

            errorClassFiles.addAll(
                elements = errorClasses
                    .filterNot { it == errorClassName.canonicalName }
                    .map { resolver.getClassDeclarationByName(it)!!.containingFile!! }
            )

            getRetrofitX(metadatas).writeTo(
                out = codeGenerator.createNewFile(
                    dependencies = Dependencies(
                        aggregating = true,
                        sources = userDefinedFiles.toTypedArray() + errorClassFiles.toTypedArray()
                    ),
                    packageName = BASE_PACKAGE,
                    fileName = "RetrofitX",
                    extensionName = "kt"
                )
            )
        } catch (e: ProcessingFailureException) {
            logger.error(e.message)
        }
        return emptyList()
    }

    /**
     * Retrofit support default methods on services, however it doesn't seem useful at all,
     * so we treat such services as invalid and won't process them
     */
    private fun Sequence<AnnotationSpec>.hasNoHttpAnnotation(): Boolean {
        return none { (it.typeName as ClassName).packageName == RETROFIT_PACKAGE }
    }

    private fun KSClassDeclaration.toServiceMetadata(defaultErrorClassName: ClassName): ServiceMetadata {
        val serviceMetadata = ServiceMetadata(
            errorClassName = defaultErrorClassName,
            serviceName = toClassName().simpleName
        )
        var isWithBoxingDefined = false
        for (annotation in annotations) {
            val annotationClassName = annotation.toAnnotationSpec().typeName as ClassName
            if (!annotationClassName.packageName.startsWith(BASE_PACKAGE)) continue
            when(val annotationName = annotationClassName.canonicalName) {
                Remote::class.qualifiedName -> {
                    val baseUrl = annotation.arguments[0].value.toString()
                    val errorClass = (annotation.arguments[1].value!! as KSType).declaration.qualifiedName!!.asString()
                    val lastIndexOfPoint = errorClass.lastIndexOf('.')
                    val errorClassPackage = errorClass.substring(0, lastIndexOfPoint)
                    val errorClassName = ClassName(errorClassPackage, errorClass.substring(lastIndexOfPoint + 1))
                    if (errorClassName.canonicalName != "kotlin.Unit") {
                        serviceMetadata.errorClassName = errorClassName
                    }
                    serviceMetadata.baseUrl = baseUrl
                }
                NotBoxed::class.qualifiedName -> {
                    assertWithBoxingInitialised(isWithBoxingDefined)
                    isWithBoxingDefined = true
                    serviceMetadata.boxed = false
                }
                Boxed::class.qualifiedName -> {
                    assertWithBoxingInitialised(isWithBoxingDefined)
                    isWithBoxingDefined = true
                    serviceMetadata.boxed = true
                }
                else -> {
                    throw ProcessingFailureException("Can't process annotation: $annotationName")
                }
            }
        }
        return serviceMetadata
    }

    private fun assertWithBoxingInitialised(isWithBoxingDefined: Boolean) {
        if (isWithBoxingDefined) {
            throw ProcessingFailureException(
                message = "Can't set both @WithBoxing and @IgnoreBoxing annotations on same service"
            )
        }
    }

    private fun createInternalServices(
        userDeclaredFunctions: Sequence<KSFunctionDeclaration>,
        userFunctionAnnotations: Map<KSFunctionDeclaration, Sequence<AnnotationSpec>>,
        metadata: ServiceMetadata
    ) = TypeSpec.interfaceBuilder("$INTERNAL_SERVICE_PREFIX${metadata.serviceName}")
        .addModifiers(KModifier.INTERNAL)
        .addFunctions(
            userDeclaredFunctions
                .map {
                    it.toInternalServiceFunSpec(
                        annotations = userFunctionAnnotations[it]!!.toList(),
                        metadata = metadata
                    )
                }
                .toList()
        )
        .build()

    private fun FileSpec.Builder.writeTo(out: OutputStream) {
        val fileSpec = indent("    ").build()
        OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer -> fileSpec.writeTo(writer) }
    }
}