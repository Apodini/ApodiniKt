package de.tum.`in`.ase.apodini.types

import kotlin.reflect.KType
import kotlin.reflect.typeOf

@TypeDefinitionDsl
interface TypeDefinitionBuilder {
    @TypeDefinitionDsl
    fun <T> `object`(
        name: String? = null,
        documentation: String? = null,
        init: ObjectDefinitionBuilder<T>.() -> Unit
    ): Object<T>

    @TypeDefinitionDsl
    fun <T> enum(
        name: String? = null,
        documentation: String? = null,
        init: EnumDefinitionBuilder<T>.() -> Unit
    ): Enum<T>

    @TypeDefinitionDsl
    fun <T> string(
        name: String? = null,
        documentation: String? = null,
        extract: T.() -> String
    ): Scalar<T, String>

    @TypeDefinitionDsl
    fun <T> int(
        name: String? = null,
        documentation: String? = null,
        extract: T.() -> Int
    ): Scalar<T, Int>

    @TypeDefinitionDsl
    fun <T> boolean(
        name: String? = null,
        documentation: String? = null,
        extract: T.() -> Boolean
    ): Scalar<T, Boolean>

    @TypeDefinitionDsl
    fun <T> double(
        name: String? = null,
        documentation: String? = null,
        extract: T.() -> Double
    ): Scalar<T, Double>
}

@TypeDefinitionDsl
abstract class ObjectDefinitionBuilder<T> {
    @TypeDefinitionDsl
    abstract fun inferFromStructure()

    @PublishedApi
    internal abstract fun <V> property(name: String, type: KType, documentation: String?, getter: T.() -> V)

    @TypeDefinitionDsl
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified V> property(name: String, documentation: String? = null, noinline getter: T.() -> V) {
        property(name, typeOf<V>(), documentation, getter)
    }
}

@TypeDefinitionDsl
interface EnumDefinitionBuilder<T> {
    @TypeDefinitionDsl
    fun case(name: String, value: T)
}