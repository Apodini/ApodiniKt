package de.tum.`in`.ase.apodini.internal

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.properties.PathParameter

internal interface ComponentVisitor : ComponentBuilder {
    sealed class Group {
        class Named(val name: String) : Group()
        class Parameter<T : Any>(val parameter: PathParameter<T>) : Group()
        object Environment : Group()
    }

    fun enterGroup(kind: Group)
    fun exitGroup()
}