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

    abstract fun identifier(getter: T.() -> String)

    @PublishedApi
    internal abstract fun <V : Any, O : String?> inherits(type: KType, getter: T.() -> O)

    @JvmName("inherits")
    @TypeDefinitionDsl
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified V : Any> inherits(noinline getter: T.() -> String) {
        inherits<V, String>(typeOf<V>(), getter)
    }

    @TypeDefinitionDsl
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified V : Any> inheritsOptional(noinline getter: T.() -> String?) {
        inherits<V, String?>(typeOf<V>(), getter)
    }

    @PublishedApi
    internal abstract fun <V : Any, O : String?> relationship(name: String? = null, type: KType, getter: T.() -> O)

    @TypeDefinitionDsl
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified V : Any> relationship(name: String? = null, noinline getter: T.() -> String) {
        relationship<V, String>(name, typeOf<V>(), getter)
    }

    @TypeDefinitionDsl
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified V : Any> optionalRelationship(name: String? = null, noinline getter: T.() -> String?) {
        relationship<V, String?>(name, typeOf<V>(), getter)
    }

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