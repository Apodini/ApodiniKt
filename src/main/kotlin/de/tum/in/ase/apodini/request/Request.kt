package de.tum.`in`.ase.apodini.request

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.compute
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.properties.DynamicProperty
import de.tum.`in`.ase.apodini.types.Type
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface Request : CoroutineContext {
    fun <T> parameter(id: UUID): T
    fun <T> environment(): T
}

suspend fun <O : Type<O>> Request.handle(
        handler: Handler<O>
): O {
    val newInstance = handler.shallowCopy()
    newInstance.modify<RequestInjectable> {  injectable ->
        injectable.shallowCopy().apply { inject(this@handle) }
    }
    return newInstance.compute(this)
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
            is DynamicProperty -> {
                val newValue = value.shallowCopy()
                newValue.modify(lookedUpType, block)
                field.set(this, newValue)
            }
            type.kotlin.isInstance(value) -> {
                @Suppress("UNCHECKED_CAST")
                val newValue = block(value as T)
                field.set(this, newValue)
            }
            else -> {}
        }
        field.isAccessible = wasAccessible
    }
}

private fun <T> T.shallowCopy(): T {
    TODO("Not implemented yet")
}