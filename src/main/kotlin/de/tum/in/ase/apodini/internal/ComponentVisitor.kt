package de.tum.`in`.ase.apodini.internal

import de.tum.`in`.ase.apodini.ComponentBuilder

internal interface ComponentVisitor : ComponentBuilder {
    sealed class Group {
        class Named(val name: String) : Group()
        object Environment : Group()
    }

    fun enterGroup(kind: Group)
    fun exitGroup()
}