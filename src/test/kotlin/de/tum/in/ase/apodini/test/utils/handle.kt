package de.tum.`in`.ase.apodini.test.utils

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.request.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.util.*

internal fun handle(vararg parameters: Pair<String, Any?>, init: ComponentBuilder.() -> Unit): Any? {
    return handle(mapOf(*parameters), init)
}

internal fun handle(parameters: Map<String, Any?>, init: ComponentBuilder.() -> Unit): Any? {
    val semanticModel = semanticModel(init)

    val endpoint = semanticModel.endpoints.first()
    return runBlocking {
        val mockRequest = object : Request, CoroutineScope by this, EnvironmentStore by EnvironmentStore.empty {
            override fun <T> parameter(id: UUID): T {
                @Suppress("UNCHECKED_CAST")
                return parameters[endpoint.parameters.first { it.id == id }.name] as T
            }
        }

        val encoder = TestEncoder()
        val result = endpoint(mockRequest)
        result.encode(encoder)
        encoder.value
    }
}