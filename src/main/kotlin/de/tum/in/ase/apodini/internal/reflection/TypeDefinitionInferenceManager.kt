package de.tum.`in`.ase.apodini.internal.reflection

import de.tum.`in`.ase.apodini.types.*
import de.tum.`in`.ase.apodini.types.Array
import de.tum.`in`.ase.apodini.types.contains
import de.tum.`in`.ase.apodini.types.Enum
import java.lang.IllegalArgumentException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

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

    private val reference = object : InferenceManagerReference {
        override val manager: TypeDefinitionInferenceManager
            get() = this@TypeDefinitionInferenceManager

        override fun cache(definition: TypeDefinition<*>, kClass: KClass<*>) {
            classes[kClass] = definition
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
        val builder = StandardTypeDefinitionBuilder(name, documentation, kClass, reference)

        // Handle CustomType
        if (javaClass.implements(CustomType::class.java)) {
            val value = createInstance(kClass) as CustomType<*>
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
            return builder.enum {
                caseFields.forEach { field ->
                    val wasAccessible = field.isAccessible
                    field.isAccessible = true
                    @Suppress("UNCHECKED_CAST")
                    case(field.name, field.get(null) as T).also { field.isAccessible = wasAccessible }
                }
            }
        }

        // Handle Objects
        return builder.`object` { inferFromStructure() }
    }
}

private interface InferenceManagerReference {
    val manager: TypeDefinitionInferenceManager
    fun cache(definition: TypeDefinition<*>, kClass: KClass<*>)
}

private fun Class<*>.iterableElement(arguments: List<KType?>): KType? {
    if (this == Iterable::class.java) {
        return arguments.first()
    }

    if (isArray) {
        return arguments.firstOrNull() ?: DummyKType(componentType)
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
    private val defaultName: String,
    private val defaultDocumentation: String?,
    private val kClass: KClass<*>,
    private val inferenceManagerReference: InferenceManagerReference
) : TypeDefinitionBuilder {
    override fun <T> `object`(
        name: String?,
        documentation: String?,
        init: ObjectDefinitionBuilder<T>.() -> Unit
    ): Object<T> {
        val definition = Object<T>(name ?: defaultName, documentation ?: defaultDocumentation)
        inferenceManagerReference.cache(definition, kClass)

        return StandardObjectBuilder(
            definition,
            kClass,
            inferenceManagerReference.manager
        ).apply(init).build()
    }

    override fun <T> enum(
        name: String?,
        documentation: String?,
        init: EnumDefinitionBuilder<T>.() -> Unit
    ) = StandardEnumBuilder<T>(
        name ?: defaultName,
        documentation ?: defaultDocumentation
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
    private val definition: Object<T>,
    private val kClass: KClass<*>,
    private val inferenceManager: TypeDefinitionInferenceManager
) : ObjectDefinitionBuilder<T>() {

    override fun inferFromStructure() {
        val fields = kClass.declaredMemberProperties

        definition.internalProperties.addAll(
            fields.mapNotNull {
                it.property(inferenceManager)
            }
        )
    }

    override fun identifier(getter: T.() -> String) {
        definition.internalIdentifier = getter
    }

    override fun <V : Any, O : String?> inherits(type: KType, getter: T.() -> O) {
        val inferred = inferenceManager.infer<V>(type) as? Object<V>
            ?: throw IllegalArgumentException("Cannot inherit from non-object type")

        definition.inheritance = Object.Relationship(null, inferred, getter)
    }

    override fun <V : Any, O : String?> relationship(name: String?, type: KType, getter: T.() -> O) {
        val inferred = inferenceManager.infer<V>(type) as? Object<V>
            ?: throw IllegalArgumentException("Cannot point relationship to non-object type")

        definition.internalRelationships.add(Object.Relationship(name, inferred, getter))
    }

    override fun <V> property(name: String, type: KType, documentation: String?, getter: T.() -> V) {
        definition.internalProperties.add(Object.Property(name, documentation, inferenceManager.infer(type), getter))
    }

    fun build(): Object<T> {
        if (definition.internalIdentifier == null && definition.inheritance == null) {
            definition.internalIdentifier = definition.inferMostLikelyIdentifier()
        }

        return definition
    }
}

private val commonIdParameterNames = setOf("id", "identifier")

private fun <T> Object<T>.inferMostLikelyIdentifier(): (T.() -> String)? {
    val stringTypes = properties.mapNotNull { it.identifierCandidate() }

    stringTypes
        .firstOrNull { commonIdParameterNames.contains(it.property.name) }
        ?.run {
            return { identifier() }
        }

    // TODO: add more edge cases
    return null
}

private sealed class IdentifierCandidate<T> {
    abstract val property: Object.Property<T, *>
    abstract fun T.identifier(): String

    class StringCandidate<T>(override val property: Object.Property<T, String>) : IdentifierCandidate<T>() {
        override fun T.identifier(): String {
            return property.getter(this)
        }
    }

    class IntCandidate<T>(override val property: Object.Property<T, Int>) : IdentifierCandidate<T>() {
        override fun T.identifier(): String {
            return property.getter(this).toString()
        }

    }

    class BooleanCandidate<T>(override val property: Object.Property<T, Boolean>) : IdentifierCandidate<T>() {
        override fun T.identifier(): String {
            return property.getter(this).toString()
        }
    }

    class DoubleCandidate<T>(override val property: Object.Property<T, Double>) : IdentifierCandidate<T>() {
        override fun T.identifier(): String {
            return property.getter(this).toString()
        }
    }

    class CustomScalar<T, A>(override val property: Object.Property<T, A>, val scalar: Scalar<A, *>) : IdentifierCandidate<T>() {
        override fun T.identifier(): String {
            val result = property.getter(this)
            return with(scalar) {
                when (kind) {
                    StringType -> result.extract() as String
                    IntType -> (result.extract() as Int).toString()
                    BooleanType -> (result.extract() as Boolean).toString()
                    DoubleType -> (result.extract() as Double).toString()
                }
            }
        }
    }
}

private fun <T, A> Object.Property<T, A>.identifierCandidate(): IdentifierCandidate<T>? {
    @Suppress("UNCHECKED_CAST")
    return when (definition) {
        StringType -> IdentifierCandidate.StringCandidate(this as Object.Property<T, String>)
        IntType -> IdentifierCandidate.IntCandidate(this as Object.Property<T, Int>)
        BooleanType -> IdentifierCandidate.BooleanCandidate(this as Object.Property<T, Boolean>)
        DoubleType -> IdentifierCandidate.DoubleCandidate(this as Object.Property<T, Double>)
        is Scalar<*, *> -> IdentifierCandidate.CustomScalar(this, definition as Scalar<A, *>)
        is Enum -> null
        is Object -> null
        is Array<*> -> null
        is Nullable<*> -> null
    }
}

private class StandardEnumBuilder<T>(
    private val name: String,
    private val documentation: String?
) : EnumDefinitionBuilder<T> {
    private val caseNameToCase = mutableMapOf<String, T>()
    private val caseToCaseName = mutableMapOf<T, String>()

    override fun case(name: String, value: T) {
        caseNameToCase[name] = value
        caseToCaseName[value] = name
    }

    fun build(): Enum<T> {
        return Enum(
            name,
            cases = caseNameToCase.keys,
            caseNameFactory = { caseToCaseName.getValue(it) },
            caseFactory = { caseNameToCase.getValue(it) },
            documentation
        )
    }

}

private fun Class<*>.implements(interfaceClass: Class<*>): Boolean {
    if (interfaces.contains(interfaceClass)) {
        return true
    }

    val relevant = listOfNotNull(superclass) + interfaces
    return relevant.any { it.implements(interfaceClass) }
}

private fun <Source, T> KCallable<T>.property(inferenceManager: TypeDefinitionInferenceManager): Object.Property<Source, *>? {
    if (annotations.contains<Hidden>()) {
        return null
    }

    val name = annotations.annotation<Renamed>()?.name ?: name
    val documentation = annotations.annotation<Documented>()?.documentation
    return Object.Property(
        name = name,
        documentation = documentation,
        definition = inferenceManager.infer(returnType),
        getter = {
            val wasAccessible = this@property.isAccessible
            this@property.isAccessible = true
            this@property.call(this).also { this@property.isAccessible = wasAccessible }
        }
    )
}