package de.tum.`in`.ase.apodini.internal.reflection

import de.tum.`in`.ase.apodini.properties.DynamicProperty
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
internal inline fun <reified T> Any.contains(): Boolean {
    return contains(typeOf<T>())
}

private fun Any.contains(lookedUpType: KType): Boolean {
    val concreteLookedUpType = lookedUpType.classifier as KClass<*>

    if (concreteLookedUpType.isInstance(this)) {
        return true
    }

    return contains(concreteLookedUpType)
}

private fun Any.contains(concreteLookedUpType: KClass<*>): Boolean {
    val type = this::class.java

    for (field in type.fields) {
        val wasAccessible = field.isAccessible
        field.isAccessible = true
        val value = field.get(this)
        field.isAccessible = wasAccessible

        if (concreteLookedUpType.isInstance(value)) {
            return true
        } else if (value is DynamicProperty && value.contains(concreteLookedUpType)) {
            return true
        }
    }

    return false
}