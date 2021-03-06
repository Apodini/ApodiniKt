package de.tum.`in`.ase.apodini.types

import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface TypeDefinitionBuilder {
    fun <T> `object`(
        name: String? = null,
        documentation: String? = null,
        init: ObjectDefinitionBuilder<T>.() -> Unit
    ): Object<T>

    fun <T> enum(
        name: String? = null,
        documentation: String? = null,
        init: EnumDefinitionBuilder<T>.() -> Unit
    ): Enum<T>

    fun <T> string(
        name: String? = null,
        documentation: String? = null,
        extract: T.() -> String
    ): Scalar<T, String>

    fun <T> int(
        name: String? = null,
        documentation: String? = null,
        extract: T.() -> Int
    ): Scalar<T, Int>

    fun <T> boolean(
        name: String? = null,
        documentation: String? = null,
        extract: T.() -> Boolean
    ): Scalar<T, Boolean>

    fun <T> double(
        name: String? = null,
        documentation: String? = null,
        extract: T.() -> Double
    ): Scalar<T, Double>
}

abstract class ObjectDefinitionBuilder<T> {
    abstract fun inferFromStructure()

    @PublishedApi
    internal abstract fun <V> property(name: String, type: KType, documentation: String?, getter: T.() -> V)

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified V> property(name: String, documentation: String? = null, noinline getter: T.() -> V) {
        property(name, typeOf<V>(), documentation, getter)
    }
}

interface EnumDefinitionBuilder<T> {
    fun case(name: String, value: T)
}