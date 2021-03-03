package de.tum.`in`.ase.apodini.request

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.internal.createInstance
import de.tum.`in`.ase.apodini.properties.DynamicProperty
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface Request : CoroutineContext, EnvironmentStore {
    fun <T> parameter(id: UUID): T
}

suspend fun <T> Request.handle(
    handler: Handler<T>
): T {
    val newInstance = handler.shallowCopy()
    newInstance.modify<RequestInjectable> { injectable ->
        injectable.shallowCopy().apply { inject(this@handle) }
    }
    newInstance.traverse<DynamicProperty> { property ->
        property.update()
    }
    return with(newInstance) { compute() }
}

@OptIn(ExperimentalStdlibApi::class)
private inline fun <reified T> Any.modify(noinline block: (T) -> T) {
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

@OptIn(ExperimentalStdlibApi::class)
private suspend inline fun <reified T> Any.traverse(noinline block: suspend (T) -> Unit) {
    traverse(typeOf<T>(), block)
}

private suspend fun <T> Any.traverse(lookedUpType: KType, block: suspend (T) -> Unit) {
    val type = this::class.java

    for (field in type.fields) {
        val wasAccessible = field.isAccessible
        field.isAccessible = true
        when (val value = field.get(this)) {
            type.kotlin.isInstance(value) -> {
                @Suppress("UNCHECKED_CAST")
                block(value as T)
            }
            is DynamicProperty -> {
                value.traverse(lookedUpType, block)
            }
            else -> {}
        }
        field.isAccessible = wasAccessible
    }
}

private fun <T> T.shallowCopy(): T {
    if (this == null)
        return this

    return nonNullShallowCopy()
}

private fun <T : Any> T.nonNullShallowCopy(): T {
    val type = this::class

    if (type.isData) {
        val copy = type.members.first { it.name == "copy" }
        @Suppress("UNCHECKED_CAST")
        return copy.call(this) as T
    }

    return createInstance(type).also { newObject ->
        for (field in type.java.fields) {
            val wasAccessible = field.isAccessible
            field.isAccessible = true
            field.set(newObject, field.get(this))
            field.isAccessible = wasAccessible
        }
    }
}