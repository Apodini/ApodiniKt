package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.types.TypeDefinition

class SemanticModel internal constructor(
        val endpoints: List<Endpoint>
) {
    abstract class Endpoint

    class ConcreteEndpoint<T>(
            val typeDefinition: TypeDefinition<T>,
            val handler: Handler<T>
    ) : Endpoint()
}