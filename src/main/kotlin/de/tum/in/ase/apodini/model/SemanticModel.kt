package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.environment.*
import de.tum.`in`.ase.apodini.exporter.Exporter
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.internal.reflection.modify
import de.tum.`in`.ase.apodini.internal.reflection.shallowCopy
import de.tum.`in`.ase.apodini.internal.reflection.traverseSuspended
import de.tum.`in`.ase.apodini.internal.toCamelCase
import de.tum.`in`.ase.apodini.logging.logger
import de.tum.`in`.ase.apodini.properties.DynamicProperty
import de.tum.`in`.ase.apodini.properties.Parameter as ParameterProperty
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import de.tum.`in`.ase.apodini.request.Request
import de.tum.`in`.ase.apodini.types.*
import kotlinx.coroutines.CoroutineScope
import java.lang.Exception
import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class SemanticModel internal constructor(
    val exporters: List<Exporter>,
    val globalEnvironment: EnvironmentStore
) {
    internal val internalEndpoints = mutableListOf<Endpoint<*>>()

    val endpoints: List<Endpoint<*>>
        get() = internalEndpoints.toList()

    sealed class PathComponent {
        data class StringPathComponent internal constructor(val value: String) : PathComponent()
        data class ParameterPathComponent internal constructor(val parameter: Parameter<String>) : PathComponent()
    }

    data class Parameter<T> internal constructor(
        val id: UUID,
        val name: String,
        val type: KType,
        val defaultValue: T?,
        val options: OptionSet<ParameterProperty<T>>
    )

    data class Result<T> internal constructor(
        val value: T,
        val definition: TypeDefinition<T>,
        val linkToSelf: Link<*>,
        val links: List<Link<*>>
    ) {
        data class Link<T> internal constructor(
            val name: String,
            val endpoint: Endpoint<T>,
            val parameterAssignment: List<ParameterAssignment<*>>
        )

        data class ParameterAssignment<T> internal constructor(
            val parameter: Parameter<T>,
            val value: T
        )

        fun encode(encoder: Encoder) {
            with(definition) {
                encoder.encode(value)
            }
        }
    }

    data class Endpoint<T> internal constructor(
        private val semanticModel: SemanticModel,
        val path: List<PathComponent>,
        val typeDefinition: TypeDefinition<T>,
        val handler: Handler<T>,
        val documentation: String?,
        val environment: EnvironmentStore,
        val parameters: List<Parameter<*>>,
    ) {
        data class Link<T> internal constructor(
            val name: String,
            val destination: Endpoint<T>
        )

        private val parentPath by lazy { path.reversed().drop(1).reversed() }

        val operation by lazy {
            with(environment) {
                operation()
            }
        }

        val parent by lazy {
            semanticModel.endpoints.firstOrNull { it.path == parentPath }
        }

        val children by lazy {
            semanticModel.endpoints.filter { it.parentPath == path }
        }

        val neighbors by lazy {
            semanticModel.endpoints.filter { it.parentPath == parentPath && it != this }
        }

        val links by lazy {
            linkedEndpoints.map { it.link() }
        }

        private val selfEndpoint by lazy {
            relation(this).partiallyAppliedEndpointFromRequest("self")!!
        }

        private val linkedEndpoints by lazy {
            val fromChildren = children
                .filter { it.operation == operation }
                .mapNotNull { relation(it).partiallyAppliedEndpointFromRequest() }

            val definition = typeDefinition.objectDefinition() ?: return@lazy fromChildren
            val endpointsFromRelations = definition
                .relationships
                .mapNotNull { relationship ->
                    @Suppress("UNCHECKED_CAST")
                    (relationship as Object.Relationship<T, *>).partiallyApplied(semanticModel)
                }

            fromChildren + endpointsFromRelations
        }

        suspend operator fun invoke(request: Request): Result<T> {
            val newInstance = this@Endpoint.handler.shallowCopy()
            val delegatedRequest = DelegatedRequest(request, newInstance, semanticModel.globalEnvironment, environment)

            try {
                newInstance.modify<RequestInjectable> { injectable ->
                    injectable.shallowCopy().apply { inject(delegatedRequest) }
                }
                newInstance.traverseSuspended<DynamicProperty> { property ->
                    property.update()
                }

                val value = with(newInstance) { delegatedRequest.compute() }
                val selfLink = selfEndpoint.link(value, request)!!
                val links = linkedEndpoints.mapNotNull { it.link(value, request) }
                return Result(value, typeDefinition, selfLink, links)
            } catch (exception: Exception) {
                delegatedRequest.logger.error {
                    """
                        Failed to respond due to exception: ${exception.localizedMessage}
                        
                        ${exception.stackTraceToString()}
                    """.trimIndent()
                }
                throw exception
            }
        }
    }
}

private fun <A, B> Object.Relationship<A, B>.partiallyApplied(
    semanticModel: SemanticModel
): PartiallyAppliedEndpoint<A, B>? {
    return semanticModel.endpointRepresenting(destination)?.let { destinationEndpoint ->
        PartiallyAppliedEndpoint<A, B>(
            name ?: destination.name.toCamelCase(),
            destinationEndpoint.endpoint
        ) { value, request ->
            value
                .identifier()
                ?.let { identifier ->
                    destinationEndpoint.endpoint.parameters.mapNotNull { parameter ->
                        if (parameter == destinationEndpoint.identifier) {
                            SemanticModel.Result.ParameterAssignment(destinationEndpoint.identifier, identifier)
                        } else {
                            parameter.assigned(request)
                        }
                    }
                }
        }
    }
}

private data class IdentifiedEndpoint<T>(
    val endpoint: SemanticModel.Endpoint<T>,
    val identifier: SemanticModel.Parameter<String>
)

@OptIn(ExperimentalStdlibApi::class)
private val stringType = typeOf<String>()
private val commonIdParameterNames = setOf("id", "identifier")

private fun <T> SemanticModel.endpointRepresenting(type: Object<T>): IdentifiedEndpoint<T>? {
    @Suppress("UNCHECKED_CAST")
    val endpointsWithCorrectReturnType = endpoints
        .filter { it.typeDefinition.objectDefinition() == type } as List<SemanticModel.Endpoint<T>>

    // Get the first one that includes the name id and a string
    endpointsWithCorrectReturnType
        .mapNotNull { endpoint ->
            val identifier = endpoint
                .parameters
                .firstOrNull { commonIdParameterNames.contains(it.name.toCamelCase()) && it.type == stringType }
                ?: return@mapNotNull null

            @Suppress("UNCHECKED_CAST")
            IdentifiedEndpoint(endpoint, identifier as SemanticModel.Parameter<String>)
        }
        .minByOrNull { it.endpoint.parameters.indexOf(it.identifier) }
        ?.let { return it }

    // Get the first one where the last component in the path is an argument
    endpointsWithCorrectReturnType
        .mapNotNull { endpoint ->
            val identifierComponent = endpoint.path.lastOrNull() as? SemanticModel.PathComponent.ParameterPathComponent ?: return@mapNotNull null
            IdentifiedEndpoint(endpoint, identifierComponent.parameter)
        }
        .firstOrNull()
        ?.let { return it }

    // TODO: Consider more complex strategies

    return null
}

private fun TypeDefinition<*>.objectDefinition(): Object<*>? {
    return when (this) {
        is Object -> this
        is Nullable -> definition.objectDefinition()
        else -> null
    }
}

private class DelegatedRequest(
    private val request: Request,
    handler: Handler<*>,
    global: EnvironmentStore,
    endpoint: EnvironmentStore
) : EnvironmentStore by request
        .extend(global)
        .override(endpoint)
        .extend({
            request {
                request
            }
            handler {
                handler
            }
        }),
    CoroutineScope by request,
    Request {

    override fun <T> parameter(id: UUID): T {
        return request.parameter(id)
    }
}

private data class EndpointRelation<A, B>(
    val source: SemanticModel.Endpoint<A>,
    val destination: SemanticModel.Endpoint<B>
)

private data class PartiallyAppliedEndpoint<A, B>(
    private val name: String,
    private val destination: SemanticModel.Endpoint<B>,
    private val parameterAssignment: (A, Request) -> List<SemanticModel.Result.ParameterAssignment<*>>?
) {
    fun link(value: A, request: Request): SemanticModel.Result.Link<B>? {
        return parameterAssignment(value, request)
            ?.let { SemanticModel.Result.Link(name, destination, it) }
    }

    fun link(): SemanticModel.Endpoint.Link<B> {
        return SemanticModel.Endpoint.Link(name, destination)
    }
}

private fun <A, B> SemanticModel.Endpoint<A>.relation(destination: SemanticModel.Endpoint<B>): EndpointRelation<A, B>  {
    return EndpointRelation(this, destination)
}

private fun <A, B> EndpointRelation<A, B>.partiallyAppliedEndpointFromRequest(hardCodedName: String? = null): PartiallyAppliedEndpoint<A, B>?  {
    val pathDifference = destination.path.removePrefix(source.path)
    val name = hardCodedName
        ?: pathDifference.lastOrNull<SemanticModel.PathComponent, SemanticModel.PathComponent.StringPathComponent>()?.value?.toCamelCase()
        ?: destination.handler::class.simpleName?.replace("Handler", "")?.toCamelCase()
        ?: destination.typeDefinition.name?.toCamelCase()
        ?: return null

    return PartiallyAppliedEndpoint(name, destination) { _, request ->
        destination.parameters.mapNotNull { it.assigned(request) }
    }
}

private inline fun <T, reified A : T> Iterable<T>.lastOrNull(): A? {
    return lastOrNull { it is A }?.let { it as A }
}

private fun <T> SemanticModel.Parameter<T>.assigned(request: Request): SemanticModel.Result.ParameterAssignment<T>? {
    return try {
        SemanticModel.Result.ParameterAssignment(this, request.parameter(id))
    } catch (exception: Exception) {
        null
    }
}

private fun <T> List<T>.removePrefix(other: List<T>): List<T> {
    if (other.isEmpty() || isEmpty()) return this

    if (other.first() == first()) {
        return drop(1).removePrefix(other.drop(1))
    }

    return this
}