package com.github.retrofitx.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

fun KSAnnotation.toAnnotationSpec(): AnnotationSpec {
    val classDeclaration = annotationType.resolve().unwrapTypeAlias().declaration as KSClassDeclaration
    val builder = AnnotationSpec.builder(classDeclaration.toClassName())
    for (argument in arguments) {
        val member = CodeBlock.builder()
        val name = argument.name!!.getShortName()
        member.add("%L = ", name)
        val argumentValue = argument.value!!
        member.add(argumentValue)
        if (name == "value") {
            builder.tag(
                type = AnnotationSpecTags.Value::class,
                tag = argumentValue
            )
        }
        builder.addMember(member.build())
    }
    return builder.build()
}

private fun KSType.unwrapTypeAlias(): KSType {
    return if (declaration is KSTypeAlias) {
        (declaration as KSTypeAlias).type.resolve()
    } else {
        this
    }
}

fun AnnotationSpec.withValue(value: String): AnnotationSpec {
    val oldValue = tags[AnnotationSpecTags.Value::class]!!
    val codeBlockForOldValue = buildCodeBlock {
        add("%L = ", "value")
        add(oldValue)
    }
    val members = members.map {
        when(it) {
            codeBlockForOldValue -> CodeBlock.of("value = %S", value)
            else -> it
        }
    }
    return AnnotationSpec
        .builder(typeName as ClassName)
        .useSiteTarget(useSiteTarget)
        .apply {
            members.forEach(this::addMember)
        }
        .build()
}

fun KSType.toTypeName(): TypeName {
    val args = arguments.map { it.type!!.resolve().toTypeName() }
    val className = declaration.toClassName()
    return when {
        args.isEmpty() -> className
        else -> className.parameterizedBy(args)
    }
}

fun KSDeclaration.toClassName(): ClassName {
    return ClassName(packageName.asString(), simpleName.asString())
}

fun KSDeclaration.toTypeName(logger: KSPLogger? = null): TypeName {
    /*val typeParameterNames = typeParameters.map {
        val parameters = it.bounds.map { it.resolve().declaration.toTypeName() }.toList()
        ClassName(it.packageName.getShortName(), it.simpleName.getShortName()).parameterizedBy()
    }
    val className = ClassName(packageName.asString(), simpleName.asString())

    logger?.logging("type: ${typeParameterNames.joinToString()}, className: $className")

    return when {
        typeParameterNames.isEmpty() -> className
        else -> className.parameterizedBy(typeParameterNames)
    }*/
    val className = ClassName(packageName.asString(), simpleName.asString())
    logger?.logging("type: ${typeParameters.joinToString {it.qualifiedName?.asString() ?: "nullParent"} }, className: $className")
    return when {
        true -> className
        else -> className//.parameterizedBy(typeParameterNames)
    }
}

fun String.toClassName(): ClassName {
    val indexOfLastPoint = lastIndexOf('.')
    return ClassName(substring(0, indexOfLastPoint), substring(indexOfLastPoint + 1))
}

private fun CodeBlock.Builder.add(value: Any) {
    when (value) {
        is List<*> -> {
            add("arrayOf(⇥⇥")
            value.forEachIndexed { index, innerValue ->
                if (index > 0) add(", ")
                add(innerValue!!)
            }
            add("⇤⇤)")
        }
        is KSType -> {
            val declaration = value.declaration
            val isEnum = (declaration as KSClassDeclaration).classKind == ClassKind.ENUM_ENTRY
            if (isEnum) {
                val parent = declaration.parentDeclaration as KSClassDeclaration
                val entry = declaration.simpleName.getShortName()
                add("%T.%L", parent.toClassName(), entry)
            } else {
                add("%T::class", declaration.toClassName())
            }
        }
        is KSName -> {
            add(
                "%T.%L", ClassName.bestGuess(value.getQualifier()),
                value.getShortName()
            )
        }
        is KSAnnotation -> add("%L", value.toAnnotationSpec())
        else -> add(value.toCodeBlock())
    }
}

/**
 * Creates a [CodeBlock] with parameter `format` depending on the given `value` object.
 * Handles a number of special cases, such as appending "f" to `Float` values, and uses
 * `%L` for other types.
 */
private fun Any.toCodeBlock() = when (this) {
    is Class<*> -> CodeBlock.of("%T::class", this)
    is Enum<*> -> CodeBlock.of("%T.%L", javaClass, name)
    is String -> CodeBlock.of("%S", this)
    is Float -> CodeBlock.of("%Lf", this)
    is Double -> CodeBlock.of("%L", this)
    is Char -> CodeBlock.of("$this.toChar()")
    is Byte -> CodeBlock.of("$this.toByte()")
    is Short -> CodeBlock.of("$this.toShort()")
    // Int or Boolean
    else -> CodeBlock.of("%L", this)
}