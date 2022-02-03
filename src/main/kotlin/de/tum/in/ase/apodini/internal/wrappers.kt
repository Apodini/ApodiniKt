package de.tum.`in`.ase.apodini.internal

import de.tum.`in`.ase.apodini.Component
import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.modifiers.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KType

internal class HandlerWrapper<T>(private val handler: Handler<T>, private val returnType: KType) : Component {
    override fun ComponentBuilder.invoke() {
        add(handler, returnType)
    }
}

internal class ModifiedComponent(private val component: Component, private val modifier: Modifier) : Component {
    override fun ComponentBuilder.invoke() {
        with(modifier) {
            wrap(component)
        }
    }
}

internal class ModifiedHandler<T>(val handler: Handler<T>, val modifiers: List<Modifier>) : Handler<T> {
    override suspend fun CoroutineScope.handle(): T {
        throw IllegalArgumentException(
            "Unexpected call to `compute` to a handler wrapped in modifiers"
        )
    }

    override fun modify(modifier: Modifier): Handler<T> {
        return ModifiedHandler(handler, modifiers + modifier)
    }
}