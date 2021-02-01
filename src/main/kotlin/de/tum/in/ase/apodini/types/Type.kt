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

sealed class ScalarType<T>(val name: String) : TypeDefinition<T>(null)

private object StringType : ScalarType<String>("String") {
    override fun Encoder.encode(value: String) {
        encode(value)
    }
}

private object IntType : ScalarType<Int>("Int") {
    override fun Encoder.encode(value: Int) {
        encode(value)
    }
}

private object BooleanType : ScalarType<Boolean>("Boolean") {
    override fun Encoder.encode(value: Boolean) {
        encode(value)
    }
}

private object DoubleType : ScalarType<Double>("Double") {
    override fun Encoder.encode(value: Double) {
        encode(value)
    }
}

class Scalar<T, Encoded> private constructor(
    name: String? = null,
    private val kind: ScalarType<Encoded>,
    documentation: String? = null,
    private val extract: T.() -> Encoded
) : TypeDefinition<T>(documentation) {
    private val name = name ?: kind

    override fun Encoder.encode(value: T) {
        val encoded = value.extract()
        with(kind) {
            encode(encoded)
        }
    }

    companion object {
        fun <T> string(
                name: String? = null,
                documentation: String? = null,
                extract: (T) -> String
        ): Scalar<T, String> = Scalar(name, StringType, documentation, extract)

        fun <T> int(
                name: String? = null,
                documentation: String? = null,
                extract: (T) -> Int
        ): Scalar<T, Int> = Scalar(name, IntType, documentation, extract)

        fun <T> boolean(
                name: String? = null,
                documentation: String? = null,
                extract: (T) -> Boolean
        ): Scalar<T, Boolean> = Scalar(name, BooleanType, documentation, extract)

        fun <T> double(
                name: String? = null,
                documentation: String? = null,
                extract: (T) -> Double
        ): Scalar<T, Double> = Scalar(name, DoubleType, documentation, extract)
    }
}

class Enum<T : Serializable> constructor(
    val name: String,
    val cases: Iterable<String>,
    documentation: String? = null
) : TypeDefinition<T>(documentation) {

    override fun Encoder.encode(value: T) {
        encode(value)
    }

}

class Object<T> constructor(
    val name: String,
    val properties: Map<String, TypeDefinition<*>>,
    documentation: String? = null
) : TypeDefinition<T>(documentation) {

    override fun Encoder.encode(value: T) {
        TODO("Not yet implemented")
    }

}

class Array<T : Type<T>> constructor() : TypeDefinition<Iterable<T>>(null) {
    override fun Encoder.encode(value: Iterable<T>) {
        unKeyed {
            value.forEach {
                encode(it)
            }
        }
    }
}

class Nullable<T : Type<T>> constructor() : TypeDefinition<T?>(null) {
    override fun Encoder.encode(value: T?) {
        value?.let {
            encode(it)
        }
    }
}