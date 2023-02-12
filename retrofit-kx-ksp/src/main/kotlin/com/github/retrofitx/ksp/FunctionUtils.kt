package com.github.retrofitx.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.github.retrofitx.Boxed
import com.github.retrofitx.DataResponse
import com.github.retrofitx.NotBoxed
import com.github.retrofitx.UnitResponse
import com.github.retrofitx.internal.ServicesCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import okhttp3.Call
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.http.*
import java.util.concurrent.CountDownLatch
import kotlin.reflect.KClass

@OptIn(KspExperimental::class)
fun KSFunctionDeclaration.toInternalServiceFunSpec(
    annotations: List<AnnotationSpec>,
    metadata: ServiceMetadata
): FunSpec {
    val serviceAnnotations = mutableListOf<AnnotationSpec>()
    var isBoxingAnnotationSet = false

    for (annotationSpec in annotations) {
        var serviceAnnotation = annotationSpec
        val annotationClassName = serviceAnnotation.typeName as ClassName
        val annotationName = annotationClassName.simpleName
        val annotationPackageName = annotationClassName.packageName

        when {
            annotationPackageName == BASE_PACKAGE && annotationName.isBoxingAnnotation() -> {
                isBoxingAnnotationSet = true
                serviceAnnotation = AnnotationSpec.builder(annotationClassName).build()
            }
            annotationPackageName == RETROFIT_PACKAGE -> {
                val httpAnnotation = PATH_ANNOTATIONS.firstOrNull {
                    it.qualifiedName == annotationClassName.canonicalName
                }

                if (
                    httpAnnotation != null &&
                    metadata.baseUrl != null &&
                    parameters.none { it.isAnnotationPresent(Url::class) }
                ) {
                    val httpPath = annotationSpec.tags[AnnotationSpecTags.Value::class].toString()
                    if (httpPath.isRelative()) {
                        serviceAnnotation = serviceAnnotation.withValue(metadata.baseUrl + httpPath)
                    }
                }
            }
        }
        serviceAnnotations.add(serviceAnnotation)
    }

    val isServiceBoxed = metadata.boxed
    if (!isBoxingAnnotationSet && isServiceBoxed != null) {
        val boxingClass: KClass<out Annotation> = when {
            !isServiceBoxed -> NotBoxed::class
            else -> Boxed::class
        }

        serviceAnnotations.add(AnnotationSpec.builder(boxingClass).build())
    }

    val parameterSpec = parameters.map { parameter ->
        val parameterDeclaration = parameter.type.resolve()
        val parameterName = parameter.name!!.asString()
        val parameterAnnotations = parameter.annotations.map { it.toAnnotationSpec() }.toList()

        ParameterSpec(
            name = parameterName, type = parameterDeclaration.toTypeName()
        ).toBuilder().addAnnotations(parameterAnnotations).build()
    }

    val returnTypeDeclaration = returnType!!.resolve()

    return FunSpec.builder(simpleName.asString())
        .addAnnotations(serviceAnnotations)
        .addModifiers(KModifier.SUSPEND, KModifier.ABSTRACT)
        .apply {
            addParameters(parameterSpec)
        }
        .returns(returnTypeDeclaration.toTypeName())
        .build()
}

private fun String.isBoxingAnnotation(): Boolean {
    return this in listOf(NotBoxed::class.simpleName, Boxed::class.simpleName)
}

private fun String.isRelative() = !startsWith("http://") && !startsWith("https://")

fun TypeSpec.toRetrofitXServiceDeclaration(metadata: ServiceMetadata): TypeSpec {
    val typeName = name!!.substring(INTERNAL_SERVICE_PREFIX.length)

    val (successOrientedFuns, resultOrientedFuns) = funSpecs.partition {
        it.returnType.isUnit()
    }

    val newResultOrientedFuns = resultOrientedFuns.map {
        val parameters = it.parameters.map {
            ParameterSpec(name = it.name, type = it.type as ClassName)
        }
        FunSpec.builder(it.name)
            .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
            .apply {
                addParameters(parameters)
            }
            .returns(DataResponse::class.asClassName().parameterizedBy(it.returnType!!, metadata.errorClassName))
            .build()
    }
    val newSuccessOrientedFuns = successOrientedFuns.flatMap {
        val parameters = it.parameters.map {
            ParameterSpec(name = it.name, type = it.type as ClassName)
        }
        val fireForgetFun = FunSpec.builder(it.name)
            .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
            .apply {
                addParameters(parameters)
            }
            .returns(UnitResponse::class.asClassName().parameterizedBy(metadata.errorClassName))
            .build()
        val safeFun = FunSpec.builder("${it.name}Safe")
            .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
            .apply {
                addParameters(parameters)
            }
            .returns(Unit::class)
            .build()
        listOf(fireForgetFun, safeFun)
    }

    return TypeSpec.interfaceBuilder(typeName)
        .addFunctions(newResultOrientedFuns + newSuccessOrientedFuns)
        .build()
}

private fun TypeName?.isUnit(): Boolean {
    val className = this as? ClassName?
    return className?.canonicalName == "kotlin.Unit"
}

fun TypeSpec.toRetrofitXServiceImplementations(metadata: ServiceMetadata): TypeSpec {
    val typeName = "${name}Impl"

    val errorAdapter = JsonAdapter::class.asClassName()
        .parameterizedBy(metadata.errorClassName)
        .toPrivateProperty("errorAdapter")

    val internalServices = ClassName(INTERNAL_PACKAGE, "$INTERNAL_SERVICE_PREFIX$name")
        .toPrivateProperty("internalServices")

    val primaryConstructorProperties = listOf(errorAdapter, internalServices)
    val primaryConstructorSpec = FunSpec.constructorBuilder().apply {
        addParameters(
            primaryConstructorProperties.map {
                ParameterSpec.builder(it.name, it.type).build()
            }
        )
    }.build()

    val functionTypes = funSpecs.groupBy {
        val returnType = (it.returnType as? ParameterizedTypeName)?.rawType?.simpleName
        when (returnType) {
            DataResponse::class.simpleName -> FunctionType.RESULT_ORIENTED
            UnitResponse::class.simpleName -> FunctionType.SUCCESS_ORIENTED
            else -> FunctionType.SAFE
        }
    }

    return TypeSpec.classBuilder(typeName)
        .primaryConstructor(primaryConstructorSpec)
        .addProperties(primaryConstructorProperties)
        .addModifiers(KModifier.INTERNAL)
        .addSuperinterface(ClassName(INTERNAL_PACKAGE, name!!))
        .apply {
            addFunctions(
                funSpecs = functionTypes.flatMap { functionType ->
                    val funSpecs = functionType.value
                    when (functionType.key) {
                        FunctionType.RESULT_ORIENTED -> funSpecs.map { it.toResultOrientedImplementation() }
                        FunctionType.SUCCESS_ORIENTED -> funSpecs.map { it.toSuccessOrientedImplementation() }
                        FunctionType.SAFE -> funSpecs.map { it.toSafeImplementation() }
                    }
                }
            )
        }
        .build()
}

private fun FunSpec.toResultOrientedImplementation(): FunSpec {
    return override(
        """
        |return invokeDataFunction(
        |    retrofitFunction = { internalServices.$name(${parameters.joinToString { it.name }}) }, 
        |    errorAdapter = errorAdapter
        |)
        """.trimMargin().fixInline()
    )
}

/**
 * KotlinPoet formatting is odd for inlined function body: inlined code shifts with 2 indentations.
 * It's inlined if String starts from "return" part.
 * To workaround this odd formatting we add 1 space to disable body inlining.
 */
private fun String.fixInline(): String {
    return if (startsWith("return")) {
        lines().filterNot { it.isEmpty() }.joinToString(separator = "\n").prependIndent(" ")
    } else {
        this
    }
}

private fun FunSpec.override(code: String): FunSpec {
    return FunSpec.builder(name)
        .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
        .addParameters(parameters)
        .addCode(code)
        .returns(returnType!!)
        .build()
}

private fun FunSpec.toSuccessOrientedImplementation(): FunSpec {
    return override(
        """
        |return invokeUnitFunction(
        |    retrofitFunction = { internalServices.$name(${parameters.joinToString { it.name }}) }, 
        |    errorAdapter = errorAdapter
        |)
        """.trimMargin().fixInline()
    )
}

private fun FunSpec.toSafeImplementation(): FunSpec {
    val internalServiceFunctionName = name.substringBeforeLast("Safe")
    return override(
        """
        |try { 
        |    internalServices.$internalServiceFunctionName(${parameters.joinToString { it.name }}) 
        |} catch (e: Exception) {
        |    e.printStackTrace(System.err)
        |}
        """.trimMargin()
    )
}

private fun TypeName.toPrivateProperty(propertyName: String): PropertySpec {
    return PropertySpec.builder(
        name = propertyName,
        type = this,
        KModifier.PRIVATE
    ).initializer(propertyName).build()
}

fun getRetrofitX(metadatas: List<ServiceMetadata>): FileSpec.Builder {
    val baseUrlType = String::class.asClassName().copy(nullable = true)

    val baseUrlProperty = PropertySpec
        .builder("baseUrl", baseUrlType, KModifier.PRIVATE)
        .mutable()
        .setter(
            setter = FunSpec
                .setterBuilder()
                .addParameter("value", baseUrlType)
                .addCode(
                    """
                    |val newBaseUrl = value!!
                    |field = newBaseUrl
                    |val retrofit = createRetrofit(newBaseUrl)
                    |servicesCache = ServicesCache(retrofit)
                    |isInitialised.countDown()
                    """.trimMargin()
                )
                .build()
        )
        .initializer(CodeBlock.of("null"))
        .build()

    val isInitialised = PropertySpec
        .builder("isInitialised", CountDownLatch::class, KModifier.PRIVATE)
        .initializer(CodeBlock.of("CountDownLatch(1)"))
        .build()

    val retrofitCallFactory = PropertySpec
        .builder("retrofitCallFactory", Call.Factory::class, KModifier.PRIVATE)
        .build()

    val servicesCache = PropertySpec
        .builder(
            name = "servicesCache",
            type = ServicesCache::class.asClassName().copy(nullable = true),
            KModifier.PRIVATE
        )
        .initializer(CodeBlock.of("null"))
        .mutable()
        .build()

    val converterFactory = PropertySpec
        .builder(
            name = "converterFactory",
            type = Converter.Factory::class,
            KModifier.PRIVATE
        )
        .build()
    val retrofitXServices = metadatas.map {
        PropertySpec
            .builder(
                name = it.serviceName.replaceFirstChar { it.lowercase() },
                type = ClassName(BASE_PACKAGE, it.serviceName)
            )
            .build()
    }

    val distinctErrorClasses = metadatas.map { it.errorClassName.canonicalName }.distinct()
    val errorClasses = distinctErrorClasses.mapIndexed { index, errorClassName ->
        PropertySpec
            .builder(
                name = "errorAdapter$index",
                type = JsonAdapter::class.asClassName().parameterizedBy(errorClassName.toClassName()),
                KModifier.PRIVATE
            )
            .build()
    }

    val properties = mutableListOf<PropertySpec>().apply {
        add(baseUrlProperty)
        add(isInitialised)
        add(retrofitCallFactory)
        add(servicesCache)
        add(converterFactory)
        addAll(errorClasses)
        addAll(retrofitXServices)
    }

    val createRetrofitFun = FunSpec.builder("createRetrofit")
        .addParameter(ParameterSpec.builder("baseUrl", String::class).build())
        .addModifiers(KModifier.PRIVATE)
        .addCode(
            """
            |return Retrofit.Builder()
            |    .callFactory(retrofitCallFactory)
            |    .baseUrl(baseUrl)
            |    .addConverterFactory(converterFactory)
            |    .build()
            """.trimMargin()
        )
        .returns(Retrofit::class)
        .build()

    val stringConstructor = FunSpec
        .constructorBuilder()
        .addParameter(ParameterSpec.builder("baseUrl", String::class).build())
        .addParameter(
            ParameterSpec.builder("okHttpClient", OkHttpClient::class)
                .defaultValue(CodeBlock.of("OkHttpClient()"))
                .build()
        )
        .addParameter(
            ParameterSpec.builder(name = "moshi", type = Moshi::class)
                .defaultValue(CodeBlock.of("Moshi.Builder().build()"))
                .build()
        )
        .addParameter(
            ParameterSpec.builder(name = "boxedByDefault", type = Boolean::class)
                .defaultValue(CodeBlock.of("false"))
                .build()
        )
        .addCode(
            """
            |this.converterFactory = ResponseBoxingFactory(moshi, boxedByDefault)
            |this.retrofitCallFactory = okHttpClient
            |this.baseUrl = baseUrl
            """.trimMargin()
        )
        .initErrorAdaptersAndRetrofitXServices(metadatas, distinctErrorClasses)
        .build()

    val stringFlowConstructor = FunSpec
        .constructorBuilder()
        .addParameter(ParameterSpec.builder("baseUrl", Flow::class.parameterizedBy(String::class)).build())
        .addParameter(ParameterSpec.builder("scope", CoroutineScope::class).build())
        .addParameter(
            ParameterSpec.builder("okHttpClient", OkHttpClient::class)
                .defaultValue(CodeBlock.of("OkHttpClient()"))
                .build()
        )
        .addParameter(
            ParameterSpec.builder(name = "moshi", type = Moshi::class)
                .defaultValue(CodeBlock.of("Moshi.Builder().build()"))
                .build()
        )
        .addParameter(
            ParameterSpec.builder(name = "boxedByDefault", type = Boolean::class)
                .defaultValue(CodeBlock.of("false"))
                .build()
        )
        .addCode(
            """
            |this.converterFactory = ResponseBoxingFactory(moshi, boxedByDefault)
            |this.retrofitCallFactory = okHttpClient
            |scope.launch(Dispatchers.IO) {
            |    baseUrl.collect { factoryBaseUrl -> this@RetrofitX.baseUrl = factoryBaseUrl }
            |}
            """.trimMargin()
        )
        .initErrorAdaptersAndRetrofitXServices(metadatas, distinctErrorClasses)
        .build()

    val retrofitXType = TypeSpec.classBuilder("RetrofitX")
        .addProperties(properties)
        .addFunction(createRetrofitFun)
        .addFunction(stringConstructor)
        .addFunction(stringConstructor.toInternalConstructor())
        .addFunction(stringFlowConstructor)
        .addFunction(stringFlowConstructor.toInternalConstructor())
        .build()
    
    val imports = listOf(
        "$INTERNAL_PACKAGE.ResponseBoxingFactory",
        "kotlinx.coroutines.Dispatchers",
        "kotlinx.coroutines.launch",
        "java.lang.reflect.Proxy"
    )

    val internalServices = metadatas.map { "$INTERNAL_SERVICE_PREFIX${it.serviceName}" }
    val retrofitXServiceImplementations = metadatas.map { "${it.serviceName}Impl" }
    
    return FileSpec.builder(BASE_PACKAGE, "RetrofitX")
        .addImport(INTERNAL_PACKAGE, internalServices)
        .addImport(INTERNAL_PACKAGE, retrofitXServiceImplementations)
        .apply {
            for (import in imports) {
                val lastIndexOfPoint = import.lastIndexOf('.')
                addImport(
                    packageName = import.substring(0, lastIndexOfPoint),
                    import.substring(lastIndexOfPoint + 1)
                )
            }
        }
        .addType(retrofitXType)
}

private fun FunSpec.toInternalConstructor(): FunSpec {
    val internalStringBuilder = toBuilder()
    internalStringBuilder.parameters.replaceAll {
        when (it.name) {
            "okHttpClient" -> ParameterSpec.builder("okHttpClient", Call.Factory::class).build()
            else -> it
        }
    }
    return internalStringBuilder.addModifiers(KModifier.INTERNAL).build()
}

private fun FunSpec.Builder.initErrorAdaptersAndRetrofitXServices(
    metadatas: List<ServiceMetadata>,
    distinctErrorClasses: List<String>
): FunSpec.Builder {
    for (errorClass in distinctErrorClasses.withIndex()) {
        val code = buildString {
            append("\n")
            append("errorAdapter${errorClass.index} = moshi.adapter(${errorClass.value}::class.java)")
            if (errorClass.index == distinctErrorClasses.lastIndex) {
                append("\n")
            }
        }
        addCode(code)
    }

    for (i in metadatas.indices) {
        val serviceMetadata = metadatas[i]
        val indexOfError = distinctErrorClasses.indexOf(serviceMetadata.errorClassName.canonicalName)

        val internalServiceVariable = "internal${serviceMetadata.serviceName}"
        val retrofitXServiceInitialisationBlock = getRetrofitXServiceInitialisationBlock(
            serviceMetadata = serviceMetadata,
            errorVariableName = "errorAdapter$indexOfError",
            internalServiceVariable = internalServiceVariable
        )
        addCode(
            """
            |
            |val $internalServiceVariable = $INTERNAL_SERVICE_PREFIX${serviceMetadata.serviceName}::class.java.run {
            |    Proxy.newProxyInstance(classLoader, arrayOf(this)) { _, method, args ->
            |        isInitialised.await()
            |        val service = servicesCache!!.getServiceOrCreate(this)
            |        method.invoke(service, *args)
            |    } as $INTERNAL_SERVICE_PREFIX${serviceMetadata.serviceName}
            |}
            |$retrofitXServiceInitialisationBlock
            """.trimMargin()
        )
    }
    return this
}

private fun getRetrofitXServiceInitialisationBlock(
    serviceMetadata: ServiceMetadata,
    errorVariableName: String,
    internalServiceVariable: String
): String {
    val retrofitXServiceVariable = serviceMetadata.serviceName.replaceFirstChar { it.lowercase() }
    return "$retrofitXServiceVariable = ${serviceMetadata.serviceName}Impl($errorVariableName, $internalServiceVariable)"
}