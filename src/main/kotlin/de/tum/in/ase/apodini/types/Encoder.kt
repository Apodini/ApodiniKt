package de.tum.`in`.ase.apodini.types

interface Encoder {
    interface KeyedContainer {
        fun encode(key: String, init: Encoder.() -> Unit)
        fun encodeIdentifier(identifier: String, definition: Object<*>) {
            // No-op
        }
    }

    interface UnKeyedContainer {
        fun encode(init: Encoder.() -> Unit)
    }

    fun encodeString(string: String)
    fun encodeBoolean(boolean: Boolean)
    fun encodeInt(int: Int)
    fun encodeDouble(double: Double)
    fun encodeNull()

    fun keyed(init: KeyedContainer.() -> Unit)
    fun unKeyed(init: UnKeyedContainer.() -> Unit)
}