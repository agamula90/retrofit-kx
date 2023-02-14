package io.github.retrofitx.ksp.utils

import io.github.retrofitx.ksp.PATH_ANNOTATIONS
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaType

fun KClass<*>.getDeclaredMemberFunctionByTemplate(
    function: KFunction<*>,
    ignoreAnnotations: Boolean = false,
    name: (KFunction<*>) -> String = KFunction<*>::name,
    ignoreReturnType: Boolean = true
): KFunction<*> {
    val functionName = name(function)
    return declaredMemberFunctions.first {
        val hasSameParameters = it.valueParameters.size == function.valueParameters.size &&
                it.valueParameters.all { parameter ->
                    function.valueParameters.any { functionParameter ->
                        parameter.name == functionParameter.name &&
                        parameter.type == functionParameter.type &&
                        (ignoreAnnotations || parameter.annotations == functionParameter.annotations)
                }
        }
        it.name == functionName && hasSameParameters && (ignoreReturnType || it.returnType == function.returnType)
    }
}

fun KType.assertReturnTypeEquals(second: KType) {
    assert(javaType.typeName == second.javaType.typeName) {
        """
        |Expect return types being of same type, got
        |first return type: ${javaType.typeName}
        |second return type: ${second.javaType.typeName}
        """.trimMargin()
    }
}

fun List<Annotation>.assertEqualsWithoutPathPrefix(
    other: List<Annotation>,
    pathPrefix: String = ""
) {
    val firstPathAnnotations = filter { it.annotationClass in PATH_ANNOTATIONS }
    val secondPathAnnotations = other.filter { it.annotationClass in PATH_ANNOTATIONS }

    assert(firstPathAnnotations.size == secondPathAnnotations.size && firstPathAnnotations.size <= 1) {
        """
        |Expect equal count (<1) of path annotations, got
        |first path annotations count: ${firstPathAnnotations.size}
        |second path annotations count: ${secondPathAnnotations.size}
        """.trimMargin()
    }
    if (firstPathAnnotations.size == 1) {
        firstPathAnnotations[0].assertEquals(
            second = secondPathAnnotations[0],
            excludedPropertyNames = listOf("value")
        )

        val firstPathProperty = firstPathAnnotations[0].annotationClass
            .declaredMemberProperties.first { it.name == "value" }
        val secondPathProperty = secondPathAnnotations[0].annotationClass
            .declaredMemberProperties.first { it.name == "value" }

        val firstPath = firstPathProperty.call(firstPathAnnotations[0]).toString()
        val secondPath = secondPathProperty.call(secondPathAnnotations[0]).toString()
        assert(secondPath == pathPrefix + firstPath) {
            """
            |Paths don't match, expect path being ${pathPrefix + firstPath},
            |got $secondPath
            |""".trimMargin()
        }
    }
    val firstRegularAnnotations = filterNot { firstPathAnnotations.contains(it) }

    for (firstRegularAnnotation in firstRegularAnnotations) {
        val secondRegularAnnotations = other.filter { it.annotationClass == firstRegularAnnotation.annotationClass }
        assert(secondRegularAnnotations.size == 1) {
            """
            |Expect one and only one annotation of type ${firstRegularAnnotation.annotationClass.qualifiedName}
            |but found ${secondRegularAnnotations.size} of type ${firstRegularAnnotation.annotationClass.qualifiedName}
            """.trimMargin()
        }
        firstRegularAnnotation.assertEquals(secondRegularAnnotations[0])
    }
}

fun Annotation.assertEquals(second: Annotation, excludedPropertyNames: List<String> = emptyList()) {
    val firstAnnotationClass = annotationClass
    val secondAnnotationClass = second.annotationClass
    val firstAnnotationClassName = firstAnnotationClass.simpleName
    val secondAnnotationClassName = secondAnnotationClass.simpleName

    assert(firstAnnotationClass == secondAnnotationClass) {
        """
        |Expect same annotation class for comparable annotations, got
        |first annotation class: $firstAnnotationClassName
        |second annotation class: $secondAnnotationClassName
        """.trimMargin()
    }
    val firstAnnotationProperties = firstAnnotationClass.declaredMemberProperties.filterNot {
        excludedPropertyNames.contains(it.name)
    }
    val secondAnnotationProperties = secondAnnotationClass.declaredMemberProperties.filterNot {
        excludedPropertyNames.contains(it.name)
    }

    assert(firstAnnotationProperties.size == secondAnnotationProperties.size) {
        """
        |Expect same count of filtered properties for comparable annotations, got
        |first annotation properties count: ${firstAnnotationProperties.size}
        |second annotation properties count: ${secondAnnotationProperties.size}
        """.trimMargin()
    }

    for (i in firstAnnotationProperties.indices) {
        val firstPropertyResult = firstAnnotationProperties[i].call(this)
        val secondPropertyResult = secondAnnotationProperties[i].call(second)
        assert(firstPropertyResult == secondPropertyResult) {
            """
            |Expect comparable annotations to have same properties, got
            |first property: (${firstAnnotationProperties[i].name} = $firstPropertyResult)
            |second property: (${secondAnnotationProperties[i].name} = $secondPropertyResult)
            """.trimMargin()
        }
    }

    val firstAnnotationFunctions = firstAnnotationClass.declaredMemberFunctions.toList()
    val secondAnnotationFunctions = secondAnnotationClass.declaredMemberFunctions.toList()

    assert(firstAnnotationFunctions.size == secondAnnotationFunctions.size) {
        """
        |Expect same count of functions for comparable annotations, got
        |first annotation functions count: ${firstAnnotationProperties.size}
        |second annotation functions count: ${secondAnnotationProperties.size}
        """.trimMargin()
    }

    for (i in firstAnnotationFunctions.indices) {
        val firstFunctionResult = firstAnnotationFunctions[i].call(this)
        val secondFunctionResult = secondAnnotationFunctions[i].call(second)
        assert(firstFunctionResult == secondFunctionResult) {
            """
            |Expect comparable annotations to have same function results, got
            |first function: (${firstAnnotationFunctions[i].name} = $firstFunctionResult)
            |second function: (${secondAnnotationFunctions[i].name} = $secondFunctionResult)
            """.trimMargin()
        }
    }
}

fun KParameter.assertEquals(second: KParameter) {
    assert(type.javaType.typeName == second.type.javaType.typeName) {
        """
        |Expect parameters to have same types, got
        |first parameter type: ${type.javaType.typeName}
        |second parameter type: ${second.type.javaType.typeName}
        """.trimMargin()
    }
    val firstPathAnnotations = annotations
    val secondPathAnnotations = second.annotations
    assert(firstPathAnnotations == secondPathAnnotations) {
        """
        |Expect parameters to have same annotations, got
        |first parameter annotations: ${firstPathAnnotations.joinToString { it.annotationClass.qualifiedName!! }}
        |second parameter annotations: ${secondPathAnnotations.joinToString { it.annotationClass.qualifiedName!! }}
        """.trimMargin()
    }
}

fun KType.assertIsGenericWithParameterTypes(
    className: String,
    parameterTypes: Map<String, KType> = emptyMap()
) {
    val classifier = classifier as? KClass<*>
    if (classifier == null) {
        assert(false) {
            "Expect type being $className generic, but got simple class"
        }
        return
    }
    assert(classifier.qualifiedName == className) {
        "Expect type being $className generic, but got ${classifier.qualifiedName} generic class"
    }
    for ((typeName, type) in parameterTypes) {
        val indexOfParameter = classifier.typeParameters.indexOfFirst { it.name == typeName }
        if (indexOfParameter == -1) {
            assert(false) {
                "Parameter type with name $typeName not found in $this class"
            }
            return
        }
        val parameterType = arguments[indexOfParameter].type
        assert(parameterType == type) {
            "Expect type of $typeName parameter being $type, got $parameterType"
        }
    }
}