package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.environment.extend
import de.tum.`in`.ase.apodini.environment.override
import de.tum.`in`.ase.apodini.exporter.Exporter
import de.tum.`in`.ase.apodini.properties.Parameter as ParameterProperty
import de.tum.`in`.ase.apodini.properties.PathParameter
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import de.tum.`in`.ase.apodini.request.Request
import de.tum.`in`.ase.apodini.request.handle
import de.tum.`in`.ase.apodini.types.Encoder
import de.tum.`in`.ase.apodini.types.TypeDefinition
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KType

class SemanticModel internal constructor(
        val exporters: List<Exporter>,
        val endpoints: List<Endpoint>,
        val globalEnvironment: EnvironmentStore
) {
    sealed class PathComponent {
        data class StringPathComponent(val value: String) : PathComponent()
        data class ParameterPathComponent(val parameter: PathParameter) : PathComponent()
    }

    data class Parameter<T>(
            val id: UUID,
            val name: String,
            val type: KType,
            val defaultValue: T?,
            val options: OptionSet<ParameterProperty<T>>
    )

    abstract class Endpoint

    class ConcreteEndpoint<T>(
            val path: Iterable<PathComponent>,
            val typeDefinition: TypeDefinition<T>,
            val handler: Handler<T>,
            val environment: EnvironmentStore,
            val parameters: Iterable<Parameter<*>>,
    ) : Endpoint() {
        suspend operator fun invoke(encoder: Encoder, request: Request) {
            val value = DelegatedRequest(request, EnvironmentStore.empty, environment).handle(handler)
            with(typeDefinition) {
                encoder.encode(value)
            }
        }
    }
}

class DelegatedRequest(
    private val request: Request,
    global: EnvironmentStore,
    endpoint: EnvironmentStore
) : EnvironmentStore by request.extend(global).override(endpoint), CoroutineContext by request, Request {
    override fun <T> parameter(id: UUID): T {
        return request.parameter(id)
    }
}