package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.environment.*
import de.tum.`in`.ase.apodini.exporter.Exporter
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.internal.reflection.modify
import de.tum.`in`.ase.apodini.internal.reflection.shallowCopy
import de.tum.`in`.ase.apodini.internal.reflection.traverseSuspended
import de.tum.`in`.ase.apodini.logging.logger
import de.tum.`in`.ase.apodini.properties.DynamicProperty
import de.tum.`in`.ase.apodini.properties.Parameter as ParameterProperty
import de.tum.`in`.ase.apodini.properties.PathParameter
import de.tum.`in`.ase.apodini.properties.environment
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import de.tum.`in`.ase.apodini.request.Request
import de.tum.`in`.ase.apodini.types.Encoder
import de.tum.`in`.ase.apodini.types.TypeDefinition
import kotlinx.coroutines.CoroutineScope
import java.lang.Exception
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KType

class SemanticModel internal constructor(
    val exporters: List<Exporter>,
    val globalEnvironment: EnvironmentStore
) {
    internal val internalEndpoints = mutableListOf<Endpoint<*>>()

    val endpoints: List<Endpoint<*>>
        get() = internalEndpoints

    sealed class PathComponent {
        data class StringPathComponent internal constructor(val value: String) : PathComponent()
        data class ParameterPathComponent internal constructor(val parameter: PathParameter) : PathComponent()
    }

    data class Parameter<T> internal constructor(
        val id: UUID,
        val name: String,
        val type: KType,
        val defaultValue: T?,
        val options: OptionSet<ParameterProperty<T>>
    )

    data class Endpoint<T> internal constructor(
        private val semanticModel: SemanticModel,
        val path: List<PathComponent>,
        val typeDefinition: TypeDefinition<T>,
        val handler: Handler<T>,
        val documentation: String?,
        val environment: EnvironmentStore,
        val parameters: List<Parameter<*>>,
    ) {
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

        suspend operator fun invoke(request: Request, encoder: Encoder) {
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
                with(typeDefinition) {
                    encoder.encode(value)
                }
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