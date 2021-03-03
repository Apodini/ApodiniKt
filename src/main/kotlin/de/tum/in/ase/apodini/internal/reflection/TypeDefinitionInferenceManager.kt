package de.tum.`in`.ase.apodini.internal.reflection

import de.tum.`in`.ase.apodini.internal.createInstance
import de.tum.`in`.ase.apodini.types.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties

internal class TypeDefinitionInferenceManager {
    @OptIn(ExperimentalStdlibApi::class)
    private val types = mutableMapOf<KType, TypeDefinition<*>>(
        typeOf<String>() to StringType,
        typeOf<Boolean>() to BooleanType,
        typeOf<Int>() to IntType,
        typeOf<Double>() to DoubleType,
    )

    fun <T> infer(type: KType): TypeDefinition<T> {
        types[type]?.let {
            @Suppress("UNCHECKED_CAST")
            return it as TypeDefinition<T>
        }

        return inferImpl<T>(type).also { types[type] = it }
    }

    private fun <T> inferImpl(type: KType): TypeDefinition<T> {
        val kClass = type.classifier as KClass<*>

        // Handle Nullable
        if (type.isMarkedNullable) {
            val typeDefinition = inferImpl<T>(kClass, type.arguments)
            @Suppress("UNCHECKED_CAST")
            return Nullable(typeDefinition) as TypeDefinition<T>
        }

        return inferImpl(kClass, type.arguments)
    }

    private fun <T> inferImpl(kClass: KClass<*>, arguments: List<KTypeProjection>): TypeDefinition<T> {
        val javaClass = kClass.java

        // Handle CustomType
        if (javaClass.implements(CustomType::class.java)) {
            val value = createInstance(kClass) as CustomType<*>

            @Suppress("UNCHECKED_CAST")
            return value.definition() as TypeDefinition<T>
        }

        // Handle Iterable
        val iterableElement = javaClass.iterableElement(arguments.map { it.type })
        if (iterableElement != null) {
            val definition = infer<T>(iterableElement)

            @Suppress("UNCHECKED_CAST")
            return Array(definition) as TypeDefinition<T>
        }

        // Handle Enums
        if (javaClass.isEnum) {
            val caseFields = javaClass.declaredFields.filter { it.isEnumConstant }
            val caseNames = caseFields.map { it.name }
            val cases = caseFields.map { field ->
                @Suppress("UNCHECKED_CAST")
                field.get(null) as T
            }

            val caseNameToCase = caseNames.zip(cases).toMap()
            val caseToCaseName = cases.zip(caseNames).toMap()

            return Enum(
                kClass.simpleName!!,
                cases = caseNames,
                caseNameFactory = { caseToCaseName.getValue(it) },
                caseFactory = { caseNameToCase.getValue(it) }
            )
        }

        // Handle Objects
        val fields = kClass.declaredMemberProperties
        return Object(
            kClass.simpleName!!,
            properties = fields.map {
                it.property(inferenceManager = this)
            }
        )
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun Class<*>.iterableElement(arguments: List<KType?>): KType? {
    if (this == Iterable::class.java) {
        return arguments.first()
    }

    val parameters = mapOf(*typeParameters.map { it.name }.zip(arguments).toTypedArray())
    val relevant = listOfNotNull(genericSuperclass) + genericInterfaces

    relevant
        .mapNotNull { it as? ParameterizedType }
        .forEach { type ->
            val newArguments = type.actualTypeArguments.map { argument ->
                when (argument) {
                    is TypeVariable<*> -> parameters[argument.name]
                    else -> DummyKType(argument)
                }
            }
            val nextClass = Class.forName(type.rawType.typeName)
            nextClass.iterableElement(newArguments)?.let { return it }
        }

    return null
}

data class DummyKType(val type: Type) : KType {
    override val annotations: List<Annotation>
        get() = emptyList()

    override val arguments: List<KTypeProjection>
        get() = emptyList()

    override val classifier: KClassifier?
        get() {
            val javaClass = Class.forName(type.typeName)
            return javaClass.kotlin
        }

    override val isMarkedNullable: Boolean
        get() = false

}

private fun Class<*>.implements(interfaceClass: Class<*>): Boolean {
    if (interfaces.contains(interfaceClass)) {
        return true
    }

    val relevant = listOfNotNull(superclass) + interfaces
    return relevant.any { it.implements(interfaceClass) }
}

private fun <Source, T> KCallable<T>.property(inferenceManager: TypeDefinitionInferenceManager): Object.Property<Source> {
    return Object.ConcreteProperty(
        name = name,
        definition = inferenceManager.infer(returnType),
        getter = { this@property.call(this) }
    )
}