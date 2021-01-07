package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.types.Type

interface ComponentBuilder {
    @ComponentDsl
    operator fun Component.unaryPlus()

    @ComponentDsl
    operator fun <T : Type<T>> Handler<T>.unaryPlus()
}