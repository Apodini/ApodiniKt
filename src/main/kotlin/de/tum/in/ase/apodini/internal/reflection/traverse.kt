package de.tum.`in`.ase.apodini.internal.reflection

import de.tum.`in`.ase.apodini.properties.DynamicProperty
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
internal suspend inline fun <reified T> Any.traverseSuspended(noinline block: suspend (T) -> Unit) {
    traverseSuspended(typeOf<T>(), block)
}

private suspend fun <T> Any.traverseSuspended(lookedUpType: KType, block: suspend (T) -> Unit) {
    val type = this::class.java
    val concreteLookedUpType = lookedUpType.classifier as KClass<*>

    for (field in type.fields) {
        val wasAccessible = field.isAccessible
        field.isAccessible = true
        val value = field.get(this)
        if (concreteLookedUpType.isInstance(value)) {
            @Suppress("UNCHECKED_CAST")
            block(value as T)
        } else if (value is DynamicProperty) {
            value.traverseSuspended(lookedUpType, block)
        }
        field.isAccessible = wasAccessible
    }
}

@OptIn(ExperimentalStdlibApi::class)
internal inline fun <reified T> Any.traverse(noinline block: (String, T) -> Unit) {
    traverse(typeOf<T>(), block)
}

private fun <T> Any.traverse(lookedUpType: KType, block: (String, T) -> Unit) {
    val type = this::class.java
    val concreteLookedUpType = lookedUpType.classifier as KClass<*>

    for (field in type.declaredFields) {
        val wasAccessible = field.isAccessible
        field.isAccessible = true
        val value = field.get(this)
        if (concreteLookedUpType.isInstance(value)) {
            @Suppress("UNCHECKED_CAST")
            block(field.name.removeSuffix("\$delegate"), value as T)
        } else if (value is DynamicProperty) {
            value.traverse(lookedUpType, block)
        }
        field.isAccessible = wasAccessible
    }
}