package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.properties.Parameter
import de.tum.`in`.ase.apodini.properties.PathParameter
import de.tum.`in`.ase.apodini.types.TypeDefinition

class SemanticModel internal constructor(
        val endpoints: List<Endpoint>
) {
    sealed class PathComponent {
        data class StringPathComponent(val value: String) : PathComponent()
        data class ParameterPathComponent(val parameter: PathParameter) : PathComponent()
    }

    abstract class Endpoint

    class ConcreteEndpoint<T>(
            val path: Iterable<PathComponent>,
            val typeDefinition: TypeDefinition<T>,
            val handler: Handler<T>,
            val parameters: Iterable<Parameter<*>>,
    ) : Endpoint()
}