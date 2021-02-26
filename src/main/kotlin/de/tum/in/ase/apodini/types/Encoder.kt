package de.tum.`in`.ase.apodini.types

import java.io.Serializable

interface Encoder {
    interface KeyedContainer {
        fun <T : CustomType<T>> encode(key: String, value: T)
    }

    interface UnKeyedContainer {
        fun <T : CustomType<T>> encode(value: T)
    }

    fun <T : Serializable> encode(value: T)

    fun keyed(init: KeyedContainer.() -> Unit)
    fun unKeyed(init: UnKeyedContainer.() -> Unit)
}