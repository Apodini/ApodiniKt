package de.tum.`in`.ase.apodini.types

import java.io.Serializable

interface Encoder {
    interface KeyedContainer {
        fun encode(key: String, init: Encoder.() -> Unit)
    }

    interface UnKeyedContainer {
        fun encode(init: Encoder.() -> Unit)
    }

    fun <T : Serializable> encode(value: T)

    fun keyed(init: KeyedContainer.() -> Unit)
    fun unKeyed(init: UnKeyedContainer.() -> Unit)
}