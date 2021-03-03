package de.tum.`in`.ase.apodini.internal.reflection

import de.tum.`in`.ase.apodini.properties.DynamicProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
internal inline fun <reified T> Any.modify(noinline block: (T) -> T) {
    modify(typeOf<T>(), block)
}

private fun <T> Any.modify(lookedUpType: KType, block: (T) -> T) {
    val type = this::class.java

    for (field in type.fields) {
        val wasAccessible = field.isAccessible
        field.isAccessible = true
        when (val value = field.get(this)) {
            type.kotlin.isInstance(value) -> {
                @Suppress("UNCHECKED_CAST")
                val newValue = block(value as T)
                field.set(this, newValue)
            }
            is DynamicProperty -> {
                val newValue = value.shallowCopy()
                newValue.modify(lookedUpType, block)
                field.set(this, newValue)
            }
            else -> {}
        }
        field.isAccessible = wasAccessible
    }
}