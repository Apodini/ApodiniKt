package de.tum.`in`.ase.apodini.types

import java.io.Serializable

interface Type<Self> {
    fun definition(): TypeDefinition<Self>
}

sealed class TypeDefinition<T>(
    val documentation: String?
) {
    abstract fun Encoder.encode(value: T)
}

class Scalar<T : Serializable> private constructor(
    val name: String,
    documentation: String? = null
) : TypeDefinition<T>(documentation) {

    override fun Encoder.encode(value: T) {
        encode(value)
    }

}

class Enum<T : Serializable> private constructor(
    val name: String,
    val cases: Iterable<String>,
    documentation: String? = null
) : TypeDefinition<T>(documentation) {

    override fun Encoder.encode(value: T) {
        encode(value)
    }

}

class Object<T> private constructor(
    val name: String,
    val properties: Map<String, TypeDefinition<*>>,
    documentation: String? = null
) : TypeDefinition<T>(documentation) {

    override fun Encoder.encode(value: T) {
        TODO("Not yet implemented")
    }

}

class Array<T : Type<T>> private constructor() : TypeDefinition<Iterable<T>>(null) {
    override fun Encoder.encode(value: Iterable<T>) {
        unKeyed {
            value.forEach {
                encode(it)
            }
        }
    }
}

class Nullable<T : Type<T>> private constructor() : TypeDefinition<T?>(null) {
    override fun Encoder.encode(value: T?) {
        value?.let {
            encode(it)
        }
    }
}