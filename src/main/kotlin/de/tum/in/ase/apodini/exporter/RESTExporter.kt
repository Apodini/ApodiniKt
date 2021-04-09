package de.tum.`in`.ase.apodini.exporter

import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.model.Operation
import de.tum.`in`.ase.apodini.model.SemanticModel
import de.tum.`in`.ase.apodini.properties.options.HTTPParameterMode
import de.tum.`in`.ase.apodini.properties.options.http
import de.tum.`in`.ase.apodini.request.Request
import de.tum.`in`.ase.apodini.types.Encoder
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.util.*

class RESTExporter(
    private val port: Int = 8080,
): Exporter {
    private class ApplicationRequest(
        private val context: PipelineContext<Unit, ApplicationCall>,
        private val parameters: Map<UUID, Pair<SemanticModel.Parameter<*>, EvaluatedEndpoint.ParameterDecodingStrategy>>
    ) : Request, CoroutineScope by context, EnvironmentStore by EnvironmentStore.empty {
        override fun <T> parameter(id: UUID): T {
            val (parameter, strategy) = parameters[id] ?: throw IllegalArgumentException("Invalid Parameter Retrieved")
            val value = when (strategy) {
                is EvaluatedEndpoint.ParameterDecodingStrategy.Path -> {
                    @Suppress("UNCHECKED_CAST")
                    context.call.parameters[parameter.name]?.let { it as? T }
                }
                is EvaluatedEndpoint.ParameterDecodingStrategy.Body -> TODO()
                is EvaluatedEndpoint.ParameterDecodingStrategy.Query -> {
                    @Suppress("UNCHECKED_CAST")
                    context.call.request.queryParameters[parameter.name]?.let { it as? T }
                }
                is EvaluatedEndpoint.ParameterDecodingStrategy.Header -> {
                    @Suppress("UNCHECKED_CAST")
                    context.call.request.headers[parameter.name]?.let { it as? T }
                }
            }

            value?.let { return it }
            parameter.defaultValue?.let { defaultValue ->
                @Suppress("UNCHECKED_CAST")
                return defaultValue as T
            }

            if (parameter.type.isMarkedNullable) {
                @Suppress("UNCHECKED_CAST")
                return null as T
            }

            throw BadRequestException("Expected argument ${parameter.name}")
        }
    }

    private class EvaluatedEndpoint(
        private val endpoint: SemanticModel.Endpoint<*>,
    ) {
        private val parameters = mutableMapOf<UUID, Pair<SemanticModel.Parameter<*>, ParameterDecodingStrategy>>()

        sealed class ParameterDecodingStrategy {
            object Path : ParameterDecodingStrategy()
            class Body(val alone: Boolean) : ParameterDecodingStrategy()
            object Query : ParameterDecodingStrategy()
            object Header : ParameterDecodingStrategy()
        }

        init {
            val multipleBodyParams = when (endpoint.operation) {
                Operation.Create, Operation.Update ->
                    endpoint.parameters.count { it.httpOption == null || it.httpOption == HTTPParameterMode.body } > 1
                Operation.Read, Operation.Delete ->
                    endpoint.parameters.count { it.httpOption == HTTPParameterMode.body } > 1
            }

            endpoint.parameters.forEach {
                val strategy = when (it.httpOption) {
                    HTTPParameterMode.Body -> ParameterDecodingStrategy.Body(!multipleBodyParams)
                    HTTPParameterMode.Path -> ParameterDecodingStrategy.Path
                    HTTPParameterMode.Query -> ParameterDecodingStrategy.Query
                    HTTPParameterMode.Header -> ParameterDecodingStrategy.Header
                    null -> when (endpoint.operation) {
                        Operation.Create, Operation.Update -> ParameterDecodingStrategy.Body(!multipleBodyParams)
                        Operation.Read, Operation.Delete -> ParameterDecodingStrategy.Query
                    }
                }
                parameters[it.id] = it to strategy
            }
        }

        private fun PipelineContext<Unit, ApplicationCall>.respond() {
            val request = ApplicationRequest(this, parameters)
            runBlocking(coroutineContext) {
                val result = endpoint(request)
                val encoded = result.encode(context.request)
                context.respond(encoded)
            }
        }

        fun export(routing: Routing) {
            val path = endpoint.path.map { param ->
                when (param) {
                    is SemanticModel.PathComponent.StringPathComponent -> param.value
                    is SemanticModel.PathComponent.ParameterPathComponent -> "{${param.parameter.name}}"
                }
            }.joinToString("/")

            when (endpoint.operation) {
                Operation.Create -> routing.post(path) {
                    respond()
                }
                Operation.Read -> routing.get(path) {
                    respond()
                }
                Operation.Update -> routing.put(path) {
                    respond()
                }
                Operation.Delete -> routing.delete(path) {
                    respond()
                }
            }
        }
    }

    override fun export(model: SemanticModel) {
        val endpoints = model.endpoints.map { EvaluatedEndpoint(it) }
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                jackson()
            }

            routing {
                endpoints.forEach { it.export(this) }
            }
        }.start()
    }
}


private fun <T> SemanticModel.Result<T>.encode(request: ApplicationRequest): Any {
    val encoder = BasicEncoder()

    encoder.keyed {
        encode("data") {
            encode(this)
        }
        encode("_links") {
            keyed {
                encode("self") {
                    encodeString(linkToSelf.relativeURL(request))
                }
                links.forEach { link ->
                    encode(link.name) {
                        encodeString(link.relativeURL(request))
                    }
                }
            }
        }
    }

    return encoder.value!!
}

private fun SemanticModel.Result.Link<*>.relativeURL(request: ApplicationRequest): String {
    val pathParameters = parameterAssignment
        .mapNotNull { assignment ->
            if (assignment.parameter.httpOption == HTTPParameterMode.path) {
                assignment.parameter.id to (assignment.value as String)
            } else {
                null
            }
        }
        .toMap()

    val path = endpoint
        .path
        .joinToString("/") { component ->
            when (component) {
                is SemanticModel.PathComponent.StringPathComponent -> component.value
                is SemanticModel.PathComponent.ParameterPathComponent -> pathParameters[component.parameter.id]
                    ?: component.parameter.name
            }
        }

    return "${request.local.base}/$path"
}

private val RequestConnectionPoint.base: String
    get() {
        val defaultPort = when (scheme) {
            "http" -> 80
            "https" -> 443
            else -> null
        }

        return if (port == defaultPort) {
            "$scheme://$remoteHost"
        } else {
            "$scheme://$remoteHost:$port"
        }
    }

private class BasicEncoder : Encoder {
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
        @Suppress("UNCHECKED_CAST")
        value = TestKeyedEncoder((value as? MutableMap<String, Any?>) ?: mutableMapOf()).also(init).map
    }

    override fun unKeyed(init: Encoder.UnKeyedContainer.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        value = TestUnKeyedEncoder((value as? MutableList<Any?>) ?: mutableListOf()).also(init).list
    }
}

private class TestKeyedEncoder(val map: MutableMap<String, Any?>) : Encoder.KeyedContainer {
    override fun encode(key: String, init: Encoder.() -> Unit) {
        map[key] = BasicEncoder().also(init).value
    }
}

private class TestUnKeyedEncoder(val list: MutableList<Any?>) : Encoder.UnKeyedContainer {
    override fun encode(init: Encoder.() -> Unit) {
        list.add(BasicEncoder().also(init).value)
    }
}

val <T> SemanticModel.Parameter<T>.httpOption: HTTPParameterMode<T>?
    get() = options<HTTPParameterMode<T>> { http }