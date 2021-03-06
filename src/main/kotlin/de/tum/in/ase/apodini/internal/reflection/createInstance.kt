package de.tum.`in`.ase.apodini.internal.reflection

import sun.misc.Unsafe
import kotlin.reflect.KClass

private val unsafe by lazy {
    val field = Unsafe::class.java.getDeclaredField("theUnsafe")
    field.isAccessible = true
    (field.get(null) as Unsafe).also { field.isAccessible = false }
}

internal fun <T : Any> createInstance(type: KClass<T>): T {
    @Suppress("UNCHECKED_CAST")
    return unsafe.allocateInstance(type.java) as T
}