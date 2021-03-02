package de.tum.`in`.ase.apodini.types

import de.tum.`in`.ase.apodini.internal.createInstance
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.typeOf

internal object TypeDefinitionInferenceManager {
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
            val typeDefinition = inferImpl<T>(kClass)
            @Suppress("UNCHECKED_CAST")
            return Nullable(typeDefinition) as TypeDefinition<T>
        }

        return inferImpl(kClass)
    }

    private fun <T> inferImpl(kClass: KClass<*>): TypeDefinition<T> {
        val javaClass = kClass.java

        // Handle CustomType
        if (javaClass.interfaces.contains(CustomType::class.java)) {
            val value = createInstance(kClass) as CustomType<*>

            @Suppress("UNCHECKED_CAST")
            return value.definition() as TypeDefinition<T>
        }

        // Handle Iterable
        if (javaClass.interfaces.contains(Iterable::class.java)) {
            TODO()
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

    private fun <T> makeNullable(nonNullableType: KType): TypeDefinition<T?> {
        val nonNullable = infer<T>(nonNullableType)
        return Nullable(nonNullable)
    }
}

private fun <Source, T> KCallable<T>.property(inferenceManager: TypeDefinitionInferenceManager): Object.Property<Source> {
    return Object.ConcreteProperty(
        name = name,
        definition = inferenceManager.infer(returnType),
        getter = { this@property.call(this) }
    )
}