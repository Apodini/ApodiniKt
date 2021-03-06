package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.environment.extend
import de.tum.`in`.ase.apodini.environment.override
import de.tum.`in`.ase.apodini.environment.request
import de.tum.`in`.ase.apodini.exporter.Exporter
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.internal.reflection.modify
import de.tum.`in`.ase.apodini.internal.reflection.shallowCopy
import de.tum.`in`.ase.apodini.internal.reflection.traverseSuspended
import de.tum.`in`.ase.apodini.properties.DynamicProperty
import de.tum.`in`.ase.apodini.properties.Parameter as ParameterProperty
import de.tum.`in`.ase.apodini.properties.PathParameter
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import de.tum.`in`.ase.apodini.request.Request
import de.tum.`in`.ase.apodini.types.Encoder
import de.tum.`in`.ase.apodini.types.TypeDefinition
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
        val environment: EnvironmentStore,
        val parameters: List<Parameter<*>>,
    ) {
        private val parentPath by lazy { path.reversed().drop(1).reversed() }

        val parent by lazy {
            semanticModel.endpoints.firstOrNull { it.path == parentPath }
        }

        val children by lazy {
            semanticModel.endpoints.filter { it.parentPath == path }
        }

        val neighbors by lazy {
            semanticModel.endpoints.filter { it.parentPath == parentPath }
        }

        suspend operator fun Request.invoke(encoder: Encoder) {
            val request = DelegatedRequest(this, semanticModel.globalEnvironment, environment)

            val newInstance = handler.shallowCopy()
            newInstance.modify<RequestInjectable> { injectable ->
                injectable.shallowCopy().apply { inject(request) }
            }
            newInstance.traverseSuspended<DynamicProperty> { property ->
                property.update()
            }

            val value = with(newInstance) { compute() }
            with(typeDefinition) {
                encoder.encode(value)
            }
        }
    }
}

private class DelegatedRequest(
    private val request: Request,
    global: EnvironmentStore,
    endpoint: EnvironmentStore
) : EnvironmentStore by request.extend(global).override(endpoint).extend({ request { request } }),
    CoroutineContext by request,
    Request {

    override fun <T> parameter(id: UUID): T {
        return request.parameter(id)
    }
}