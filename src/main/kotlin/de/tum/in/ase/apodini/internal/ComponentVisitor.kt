package de.tum.`in`.ase.apodini.internal

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.properties.PathParameter

internal abstract class ComponentVisitor : ComponentBuilder() {
    sealed class Group {
        class Named(val name: String) : Group()
        class Parameter(val parameter: PathParameter) : Group()
        class Environment(val store: EnvironmentStore) : Group()
    }

    abstract fun enterGroup(kind: Group)
    abstract fun exitGroup()
}