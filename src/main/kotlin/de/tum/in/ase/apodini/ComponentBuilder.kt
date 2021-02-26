package de.tum.`in`.ase.apodini

interface ComponentBuilder {
    @ComponentDsl
    operator fun Component.unaryPlus()

    @ComponentDsl
    operator fun <T> Handler<T>.unaryPlus()
}