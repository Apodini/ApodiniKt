package de.tum.`in`.ase.apodini

interface Component {
    @ComponentDsl
    operator fun ComponentBuilder.invoke()
}