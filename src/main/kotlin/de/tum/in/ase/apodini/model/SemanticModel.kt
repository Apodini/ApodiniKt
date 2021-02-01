package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.types.TypeDefinition

class SemanticModel internal constructor(
        val endpoints: List<Endpoint>
) {
    class Endpoint internal constructor(
            val typeDefinition: TypeDefinition<*>,
            val handler: Handler<*>
    )
}