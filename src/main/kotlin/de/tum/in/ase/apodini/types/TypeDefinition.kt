package de.tum.`in`.ase.apodini.types

sealed class TypeDefinition<T>(
    val documentation: String?
) {
    abstract val name: String?
    abstract fun Encoder.encode(value: T)
}

sealed class ScalarType<T>(override val name: String) : TypeDefinition<T>(null)

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
    documentation: String?,
    internal val extract: T.() -> Encoded
) : TypeDefinition<T>(documentation) {
    override val name = name ?: kind.name

    override fun Encoder.encode(value: T) {
        val encoded = value.extract()
        with(kind) {
            encode(encoded)
        }
    }

    fun erased(): Scalar<T, Encoded> {
        return Scalar(null, kind, documentation, extract)
    }
}

class Enum<T> internal constructor(
    override val name: String,
    val cases: Iterable<String>,
    internal val caseNameFactory: (T) -> String,
    internal val caseFactory: (String) -> T,
    documentation: String?
) : TypeDefinition<T>(documentation) {
    override fun Encoder.encode(value: T) {
        encodeString(caseNameFactory(value))
    }
}

class Object<T> internal constructor(
    override val name: String,
    documentation: String?
) : TypeDefinition<T>(documentation) {
    data class Relationship<A, B>(
        val name: String?,
        val destination: Object<B>,
        val identifier: A.() -> String?
    )

    class Property<Source, T>(
        val name: String,
        val documentation: String? = null,
        val definition: TypeDefinition<T>,
        internal val getter: Source.() -> T
    ) {
        fun Encoder.KeyedContainer.encode(source: Source) {
            encode(name) {
                with(definition) {
                    encode(getter(source))
                }
            }
        }
    }

    internal var internalIdentifier: (T.() -> String)? = null

    var inheritance: Relationship<T, *>? = null
        internal set

    internal val internalRelationships = mutableListOf<Relationship<T, *>>()
    internal val internalProperties = mutableListOf<Property<T, *>>()

    val relationships: List<Relationship<T, *>>
        get() = internalRelationships.toList()

    val properties: List<Property<T, *>>
        get() = internalProperties.toList()

    override fun Encoder.encode(value: T) {
        keyed {
            properties.forEach { property ->
                with(property) {
                    encode(value)
                }
            }

            val identifier = inheritance?.identifier ?: internalIdentifier
            identifier?.let { value.it() }?.let { encodeIdentifier(it, this@Object) }
        }
    }
}

data class Array<T> internal constructor(val definition: TypeDefinition<T>) : TypeDefinition<Iterable<T>>(null) {
    override val name: String? = null

    override fun Encoder.encode(value: Iterable<T>) {
        // TODO: handle Arrays that are not iterable
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
    override val name: String? = null

    override fun Encoder.encode(value: T?) {
        value?.let { unwrapped ->
            with(definition) {
                encode(unwrapped)
            }
        } ?: encodeNull()
    }
}