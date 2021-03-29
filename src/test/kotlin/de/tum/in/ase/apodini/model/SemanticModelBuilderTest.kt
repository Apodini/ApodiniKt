package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.WebService
import de.tum.`in`.ase.apodini.impl.text
import de.tum.`in`.ase.apodini.types.Nullable
import de.tum.`in`.ase.apodini.types.StringType
import junit.framework.TestCase
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine

internal class SemanticModelBuilderTest : TestCase() {

    fun testHelloWorld() {
        val semanticModel = semanticModel {
            text("Hello World")
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, StringType)
    }

    fun testCustomHandlerWithOptionalString() {
        val semanticModel = semanticModel {
            +object : Handler<String?> {
                override suspend fun CoroutineContext.compute(): String? {
                    return null
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, Nullable(StringType))
    }

    private fun semanticModel(init: ComponentBuilder.() -> Unit): SemanticModel {
        return WrapperService(init).semanticModel()
    }
}

private class WrapperService(private val init: ComponentBuilder.() -> Unit) : WebService {
    override fun ComponentBuilder.invoke() {
        init()
    }
}