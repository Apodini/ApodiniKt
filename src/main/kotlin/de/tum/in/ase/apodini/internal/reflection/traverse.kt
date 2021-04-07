package de.tum.`in`.ase.apodini.internal.reflection

import de.tum.`in`.ase.apodini.properties.DynamicProperty
import de.tum.`in`.ase.apodini.types.MirroredName
import de.tum.`in`.ase.apodini.types.contains
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
internal suspend inline fun <reified T> Any.traverseSuspended(noinline block: suspend (T) -> Unit) {
    traverseSuspended(typeOf<T>(), block)
}

internal suspend fun <T> Any.traverseSuspended(lookedUpType: KType, block: suspend (T) -> Unit) {
    val type = this::class.java
    val concreteLookedUpType = lookedUpType.classifier as KClass<*>

    for (field in type.declaredFields) {
        if (field.name == "\$\$delegatedProperties") continue

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
        if (field.name == "\$\$delegatedProperties") continue

        val wasAccessible = field.isAccessible
        field.isAccessible = true
        val value = field.get(this)
        if (concreteLookedUpType.isInstance(value)) {
            @Suppress("UNCHECKED_CAST")
            block(field.name.removeSuffix("\$delegate"), value as T)
        } else if (value is DynamicProperty) {
            value.traverse(field.name.removeSuffix("\$delegate"), lookedUpType, block)
        }
        field.isAccessible = wasAccessible
    }
}

private fun <T> Any.traverse(name: String, lookedUpType: KType, block: (String, T) -> Unit) {
    val type = this::class.java
    val concreteLookedUpType = lookedUpType.classifier as KClass<*>

    for (field in type.declaredFields) {
        if (field.name == "\$\$delegatedProperties") continue

        val wasAccessible = field.isAccessible
        field.isAccessible = true
        val value = field.get(this)
        val isNameMirrored = field.annotations?.asIterable()?.contains<MirroredName>() ?: false
        val propertyName = if (isNameMirrored) name else field.name.removeSuffix("\$delegate")

        if (concreteLookedUpType.isInstance(value)) {
            @Suppress("UNCHECKED_CAST")
            block(propertyName, value as T)
        } else if (value is DynamicProperty) {
            value.traverse(propertyName, lookedUpType, block)
        }
        field.isAccessible = wasAccessible
    }
}