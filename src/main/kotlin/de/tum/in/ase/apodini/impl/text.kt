package de.tum.`in`.ase.apodini.impl

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.modifiers.ModifiableComponent
import kotlin.coroutines.CoroutineContext

fun ComponentBuilder.text(response: String): ModifiableComponent<*> {
    return +Text(response)
}

private class Text(val response: String): Handler<String> {
    override suspend fun CoroutineContext.compute(): String {
        return response
    }
}