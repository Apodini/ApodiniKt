package de.tum.`in`.ase.apodini.types

sealed class TypeDefinition<T>(
    val documentation: String?
) {
    abstract fun Encoder.encode(value: T)
}

sealed class ScalarType<T>(val name: String) : TypeDefinition<T>(null)

internal object StringType : ScalarType<String>("String") {
    override fun Encoder.encode(value: String) {
        encodeString(value)
    }
}

internal object IntType : ScalarType<Int>("Int") {
    override fun Encoder.encode(value: Int) {
        encodeInt(value)
    }
}

internal object BooleanType : ScalarType<Boolean>("Boolean") {
    override fun Encoder.encode(value: Boolean) {
        encodeBoolean(value)
    }
}

internal object DoubleType : ScalarType<Double>("Double") {
    override fun Encoder.encode(value: Double) {
        encodeDouble(value)
    }
}

class Scalar<T, Encoded> internal constructor(
    name: String? = null,
    val kind: ScalarType<Encoded>,
    documentation: String? = null,
    private val extract: T.() -> Encoded
) : TypeDefinition<T>(documentation) {
    val name = name ?: kind

    override fun Encoder.encode(value: T) {
        val encoded = value.extract()
        with(kind) {
            encode(encoded)
        }
    }

    fun erased(): Scalar<T, Encoded> {
        return Scalar(null, kind, documentation, extract)
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

class Enum<T> internal constructor(
    val name: String,
    val cases: Iterable<String>,
    internal val caseNameFactory: (T) -> String,
    internal val caseFactory: (String) -> T,
    documentation: String? = null
) : TypeDefinition<T>(documentation) {
    override fun Encoder.encode(value: T) {
        encodeString(caseNameFactory(value))
    }
}

class Object<T> internal constructor(
    val name: String,
    documentation: String? = null
) : TypeDefinition<T>(documentation) {
    internal val internalProperties = mutableListOf<Property<T, *>>()

    val properties: Collection<Property<T, *>>
        get() = internalProperties

    class Property<Source, T>(
        val name: String,
        val documentation: String? = null,
        val definition: TypeDefinition<T>,
        private val getter: Source.() -> T
    ) {
        fun Encoder.KeyedContainer.encode(source: Source) {
            encode(name) {
                with(definition) {
                    encode(getter(source))
                }
            }
        }
    }

    override fun Encoder.encode(value: T) {
        keyed {
            properties.forEach { property ->
                with(property) {
                    encode(value)
                }
            }
        }
    }
}

data class Array<T> internal constructor(val definition: TypeDefinition<T>) : TypeDefinition<Iterable<T>>(null) {
    override fun Encoder.encode(value: Iterable<T>) {
        unKeyed {
            value.forEach { element ->
                encode {
                    with(definition) {
                        encode(element)
                    }
                }
            }
        }
    }
}

data class Nullable<T> internal constructor(val definition: TypeDefinition<T>) : TypeDefinition<T?>(null) {
    override fun Encoder.encode(value: T?) {
        value?.let { unwrapped ->
            with(definition) {
                encode(unwrapped)
            }
        } ?: encodeNull()
    }
}