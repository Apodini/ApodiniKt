package de.tum.`in`.ase.apodini.exporter

import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.model.Operation
import de.tum.`in`.ase.apodini.model.SemanticModel
import de.tum.`in`.ase.apodini.properties.options.HTTPParameterMode
import de.tum.`in`.ase.apodini.properties.options.http
import de.tum.`in`.ase.apodini.request.Request
import de.tum.`in`.ase.apodini.types.Encoder
import de.tum.`in`.ase.apodini.types.Object
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
import java.util.*

class REST(
    private val port: Int = 80,
): Exporter {
    override fun export(model: SemanticModel) {
        val endpoints = model.endpoints.map { EvaluatedEndpoint(model, it) }
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                jackson()
            }

            routing {
                endpoints.forEach { it.export(this, endpoints) }
            }
        }.start()
    }
}

private class RESTApplicationRequest(
    val context: PipelineContext<Unit, ApplicationCall>,
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
    val semanticModel: SemanticModel,
    val endpoint: SemanticModel.Endpoint<*>,
) {
    val parameters = mutableMapOf<UUID, Pair<SemanticModel.Parameter<*>, ParameterDecodingStrategy>>()

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

    private suspend fun PipelineContext<Unit, ApplicationCall>.respond(endpoints: List<EvaluatedEndpoint>) {
        val request = RESTApplicationRequest(this, parameters)
        val result = endpoint(request)
        val encoded = result.encode(request, semanticModel, endpoints)
        context.respond(encoded)
    }

    fun export(routing: Routing, endpoints: List<EvaluatedEndpoint>) {
        val path = endpoint.path.map { param ->
            when (param) {
                is SemanticModel.PathComponent.StringPathComponent -> param.value
                is SemanticModel.PathComponent.ParameterPathComponent -> "{${param.parameter.name}}"
            }
        }.joinToString("/")

        when (endpoint.operation) {
            Operation.Create -> routing.post(path) {
                respond(endpoints)
            }
            Operation.Read -> routing.get(path) {
                respond(endpoints)
            }
            Operation.Update -> routing.put(path) {
                respond(endpoints)
            }
            Operation.Delete -> routing.delete(path) {
                respond(endpoints)
            }
        }
    }
}

private fun <T> SemanticModel.Result<T>.encode(
    request: RESTApplicationRequest,
    semanticModel: SemanticModel,
    endpoints: List<EvaluatedEndpoint>
): Any {
    val encoder = BasicEncoder(semanticModel, request, endpoints)

    if (definition is Object) {
        encode(encoder)
    } else {
        encoder.keyed {
            encode("data") {
                encode(this)
            }
        }
    }

    encoder.keyed {
        encode("_links") {
            keyed {
                encode("self") {
                    encodeString(linkToSelf.relativeURL(request.context.context.request, endpoints))
                }
                links.forEach { link ->
                    encode(link.name) {
                        encodeString(link.relativeURL(request.context.context.request, endpoints))
                    }
                }
            }
        }
    }

    return encoder.value!!
}

private fun SemanticModel.Result.Link<*>.relativeURL(request: ApplicationRequest, endpoints: List<EvaluatedEndpoint>): String {
    val evaluatedEndpoint = endpoints.first { it.endpoint == endpoint }

    val pathParameters = parameterAssignment
        .mapNotNull { assignment ->
            val id = assignment.parameter.id
            val strategy = evaluatedEndpoint.parameters[id]!!.second
            if (strategy == EvaluatedEndpoint.ParameterDecodingStrategy.Path) {
                id to (assignment.value as String)
            } else {
                null
            }
        }
        .toMap()

    val queryParameters = parameterAssignment
        .mapNotNull { assignment ->
            val id = assignment.parameter.id
            val strategy = evaluatedEndpoint.parameters[id]!!.second
            if (strategy == EvaluatedEndpoint.ParameterDecodingStrategy.Query && assignment.value != null) {
                assignment.parameter.name to (assignment.value as String)
            } else {
                null
            }
        }
        .toMap()

    return URLBuilder()
        .apply {
            host = request.local.remoteHost
            protocol = when (request.local.scheme) {
                "http" -> URLProtocol.HTTP
                "https" -> URLProtocol.HTTPS
                else -> URLProtocol(request.local.scheme, request.port() - 1)
            }
            port = request.port()
            encodedPath = endpoint
                .path
                .joinToString("/") { component ->
                    when (component) {
                        is SemanticModel.PathComponent.StringPathComponent -> component.value
                        is SemanticModel.PathComponent.ParameterPathComponent -> pathParameters[component.parameter.id]
                            ?: "{${component.parameter.name}}"
                    }
                }

            queryParameters.forEach { (name, value) ->
                parameters[name] = value
            }
        }
        .buildString()
}

private class BasicEncoder(
    val semanticModel: SemanticModel,
    val request: RESTApplicationRequest,
    val endpoints: List<EvaluatedEndpoint>
) : Encoder {
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
        value = TestKeyedEncoder(semanticModel, request, endpoints, (value as? MutableMap<String, Any?>) ?: mutableMapOf()).also(init).map
    }

    override fun unKeyed(init: Encoder.UnKeyedContainer.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        value = TestUnKeyedEncoder(semanticModel, request, endpoints, (value as? MutableList<Any?>) ?: mutableListOf()).also(init).list
    }
}

private class TestKeyedEncoder(
    val semanticModel: SemanticModel,
    val request: RESTApplicationRequest,
    val endpoints: List<EvaluatedEndpoint>,
    val map: MutableMap<String, Any?>
) : Encoder.KeyedContainer {
    override fun encode(key: String, init: Encoder.() -> Unit) {
        map[key] = BasicEncoder(semanticModel, request, endpoints).also(init).value
    }

    override fun encodeIdentifier(identifier: String, definition: Object<*>) {
        val link = semanticModel.link(definition, identifier, request) ?: return
        encode("_links") {
            keyed {
                encode("_self") {
                    encodeString(link.relativeURL(request.context.context.request, endpoints))
                }
            }
        }
    }
}

private class TestUnKeyedEncoder(
    val semanticModel: SemanticModel,
    val request: RESTApplicationRequest,
    val endpoints: List<EvaluatedEndpoint>,
    val list: MutableList<Any?>
) : Encoder.UnKeyedContainer {
    override fun encode(init: Encoder.() -> Unit) {
        list.add(BasicEncoder(semanticModel, request, endpoints).also(init).value)
    }
}

val <T> SemanticModel.Parameter<T>.httpOption: HTTPParameterMode<T>?
    get() = options<HTTPParameterMode<T>> { http }