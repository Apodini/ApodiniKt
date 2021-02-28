package de.tum.`in`.ase.apodini.impl

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.ComponentDsl
import de.tum.`in`.ase.apodini.StringHandler
import kotlin.coroutines.CoroutineContext

@ComponentDsl
fun ComponentBuilder.text(response: String) {
    +Text(response)
}

private class Text(val response: String): StringHandler {
    override suspend fun CoroutineContext.compute(): String {
        return response
    }
}