package de.tum.`in`.ase.apodini.modifiers

import de.tum.`in`.ase.apodini.Component
import de.tum.`in`.ase.apodini.ComponentBuilder

interface Modifier {
    fun ComponentBuilder.wrap(component: Component)
}
