package de.tum.`in`.ase.apodini.test

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.request.Request
import de.tum.`in`.ase.apodini.types.Encoder
import junit.framework.TestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.util.*

internal inline fun <reified T> handle(handler: Handler<T>, vararg parameters: Pair<String, Any?>): Any? {
    return handle(handler, mapOf(*parameters))
}

internal inline fun <reified T> handle(handler: Handler<T>, parameters: Map<String, Any?>): Any? {
    return handle(parameters) {
        +handler
    }
}

internal fun handle(vararg parameters: Pair<String, Any?>, init: ComponentBuilder.() -> Unit): Any? {
    return handle(mapOf(*parameters), init)
}

internal fun handle(parameters: Map<String, Any?>, init: ComponentBuilder.() -> Unit): Any? {
    val semanticModel = semanticModel(init)
    TestCase.assertEquals(semanticModel.endpoints.count(), 1)

    val endpoint = semanticModel.endpoints.first()
    return runBlocking {
        val mockRequest = object : Request, CoroutineScope by this, EnvironmentStore by EnvironmentStore.empty {
            override fun <T> parameter(id: UUID): T {
                @Suppress("UNCHECKED_CAST")
                return parameters[endpoint.parameters.first { it.id == id }.name] as T
            }
        }

        val encoder = TestEncoder()
        endpoint(mockRequest, encoder)
        encoder.value
    }
}

private class TestEncoder : Encoder {
    var value: Any? = null

    override fun encodeString(string: String) {
        value = string
    }

    override fun encodeBoolean(boolean: Boolean) {
        value = boolean
    }

    override fun encodeInt(int: Int) {
        value = int
    }

    override fun encodeDouble(double: Double) {
        value = double
    }

    override fun encodeNull() {
        value = null
    }

    override fun keyed(init: Encoder.KeyedContainer.() -> Unit) {
        value = TestKeyedEncoder((value as? MutableMap<String, Any?>) ?: mutableMapOf()).also(init).map
    }

    override fun unKeyed(init: Encoder.UnKeyedContainer.() -> Unit) {
        value = TestUnkeyedEncoder((value as? MutableList<Any?>) ?: mutableListOf()).also(init).list
    }
}

class TestKeyedEncoder(val map: MutableMap<String, Any?>) : Encoder.KeyedContainer {
    override fun encode(key: String, init: Encoder.() -> Unit) {
        map[key] = TestEncoder().also(init).value
    }
}

class TestUnkeyedEncoder(val list: MutableList<Any?>) : Encoder.UnKeyedContainer {
    override fun encode(init: Encoder.() -> Unit) {
        list.add(TestEncoder().also(init).value)
    }
}
