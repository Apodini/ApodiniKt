package de.tum.`in`.ase.apodini.impl

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.StringHandler
import de.tum.`in`.ase.apodini.types.Scalar
import de.tum.`in`.ase.apodini.types.Type
import de.tum.`in`.ase.apodini.types.TypeDefinition
import kotlin.coroutines.CoroutineContext

fun ComponentBuilder.text(response: String) {
    +Text(response)
}

private class Text(val response: String): StringHandler {
    override suspend fun CoroutineContext.impl(): String {
        return response
    }
}