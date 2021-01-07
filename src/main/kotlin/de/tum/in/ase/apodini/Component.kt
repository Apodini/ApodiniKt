package de.tum.`in`.ase.apodini

interface Component {
    operator fun ComponentBuilder.invoke()
}