package de.tum.`in`.ase.apodini.internal.reflection

import de.tum.`in`.ase.apodini.types.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties

internal class TypeDefinitionInferenceManager {
    @OptIn(ExperimentalStdlibApi::class)
    private val kTypes = mutableMapOf<KType, TypeDefinition<*>>(
        typeOf<String>() to StringType,
        typeOf<Boolean>() to BooleanType,
        typeOf<Int>() to IntType,
        typeOf<Double>() to DoubleType,
    )

    private val classes = mutableMapOf<KClass<*>, TypeDefinition<*>>().also { map ->
        kTypes.forEach { (type, definition) ->
            if (!type.isMarkedNullable && type.arguments.isEmpty()) {
                map[type.classifier as KClass<*>] = definition
            }
        }
    }

    fun <T> infer(type: KType): TypeDefinition<T> {
        kTypes[type]?.let { definition ->
            @Suppress("UNCHECKED_CAST")
            return definition as TypeDefinition<T>
        }

        return inferImpl<T>(type).also { kTypes[type] = it }
    }

    private fun <T> inferImpl(type: KType): TypeDefinition<T> {
        val kClass = type.classifier as KClass<*>

        // Handle Nullable
        if (type.isMarkedNullable) {
            val typeDefinition = infer<T>(NonnullKType(type))
            @Suppress("UNCHECKED_CAST")
            return Nullable(typeDefinition) as TypeDefinition<T>
        }

        return infer(kClass, type.arguments)
    }


    private fun <T> infer(kClass: KClass<*>, arguments: List<KTypeProjection>): TypeDefinition<T> {
        if (arguments.isEmpty()) {
            classes[kClass]?.let { definition ->
                @Suppress("UNCHECKED_CAST")
                return definition as TypeDefinition<T>
            }

            return inferImpl<T>(kClass, arguments).also { classes[kClass] = it }
        }

        return inferImpl(kClass, arguments)
    }

    private fun <T> inferImpl(kClass: KClass<*>, arguments: List<KTypeProjection>): TypeDefinition<T> {
        if (arguments.isEmpty()) {
            classes[kClass]?.let { definition ->
                @Suppress("UNCHECKED_CAST")
                return definition as TypeDefinition<T>
            }
        }

        val javaClass = kClass.java
        val name = kClass.annotations.annotation<Renamed>()?.name ?: kClass.simpleName!!
        val documentation = kClass.annotations.annotation<Documented>()?.documentation

        // Handle CustomType
        if (javaClass.implements(CustomType::class.java)) {
            val value = createInstance(kClass) as CustomType<*>
            val builder = StandardTypeDefinitionBuilder(name, documentation, kClass, this)
            return with(value) {
                @Suppress("UNCHECKED_CAST")
                builder.definition() as TypeDefinition<T>
            }
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
                name,
                cases = caseNames,
                caseNameFactory = { caseToCaseName.getValue(it) },
                caseFactory = { caseNameToCase.getValue(it) }
            )
        }

        // Handle Objects
        return StandardTypeDefinitionBuilder(
            name,
            documentation,
            kClass,
            this
        ).`object` { inferFromStructure() }
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

data class NonnullKType(val type: KType) : KType by type {
    override val isMarkedNullable: Boolean
        get() = false
}

data class DummyKType(val type: Type) : KType {
    override val annotations: List<Annotation>
        get() = emptyList()

    override val arguments: List<KTypeProjection>
        get() = emptyList()

    override val classifier: KClassifier
        get() {
            val javaClass = Class.forName(type.typeName)
            return javaClass.kotlin
        }

    override val isMarkedNullable: Boolean
        get() = false
}

private class StandardTypeDefinitionBuilder(
    val defaultName: String,
    val defaultDocumentation: String?,
    val kClass: KClass<*>,
    val inferenceManager: TypeDefinitionInferenceManager
) : TypeDefinitionBuilder {
    override fun <T> `object`(
        name: String?,
        documentation: String?,
        init: ObjectDefinitionBuilder<T>.() -> Unit
    ) = StandardObjectBuilder<T>(
        name ?: defaultName,
        documentation ?: defaultDocumentation,
        kClass,
        inferenceManager
    ).apply(init).build()

    override fun <T> string(
        name: String?,
        documentation: String?,
        extract: T.() -> String
    ) = Scalar(name ?: defaultName, StringType, documentation ?: defaultDocumentation, extract)

    override fun <T> int(
        name: String?,
        documentation: String?,
        extract: T.() -> Int
    ) = Scalar(name ?: defaultName, IntType, documentation ?: defaultDocumentation, extract)

    override fun <T> boolean(
        name: String?,
        documentation: String?,
        extract: T.() -> Boolean
    ) = Scalar(name ?: defaultName, BooleanType, documentation ?: defaultDocumentation, extract)

    override fun <T> double(
        name: String?,
        documentation: String?,
        extract: T.() -> Double
    ) = Scalar(name ?: defaultName, DoubleType, documentation ?: defaultDocumentation, extract)
}

private class StandardObjectBuilder<T>(
    val name: String,
    val documentation: String?,
    val kClass: KClass<*>,
    val inferenceManager: TypeDefinitionInferenceManager
) : ObjectDefinitionBuilder<T>() {
    private val properties = mutableListOf<Object.Property<T>>()

    override fun inferFromStructure() {
        val fields = kClass.declaredMemberProperties

        properties.addAll(
            fields.mapNotNull {
                it.property(inferenceManager)
            }
        )
    }

    override fun <V> property(name: String, type: KType, documentation: String?, getter: T.() -> V) {
        properties.add(Object.ConcreteProperty(name, documentation, inferenceManager.infer<V>(type), getter))
    }

    fun build(): Object<T> {
        return Object(name, properties, documentation)
    }
}

private fun Class<*>.implements(interfaceClass: Class<*>): Boolean {
    if (interfaces.contains(interfaceClass)) {
        return true
    }

    val relevant = listOfNotNull(superclass) + interfaces
    return relevant.any { it.implements(interfaceClass) }
}

private fun <Source, T> KCallable<T>.property(inferenceManager: TypeDefinitionInferenceManager): Object.Property<Source>? {
    if (annotations.contains<Hidden>()) {
        return null
    }

    val name = annotations.annotation<Renamed>()?.name ?: name
    val documentation = annotations.annotation<Documented>()?.documentation
    return Object.ConcreteProperty(
        name = name,
        documentation = documentation,
        definition = inferenceManager.infer(returnType),
        getter = { this@property.call(this) }
    )
}

private inline fun <reified T : Annotation> Iterable<Annotation>.contains(): Boolean {
    val type = T::class
    return any { type.isInstance(it) }
}

private inline fun <reified T : Annotation> Iterable<Annotation>.annotation(): T? {
    forEach { annotation ->
        (annotation as? T)?.let { return it }
    }
    return null
}