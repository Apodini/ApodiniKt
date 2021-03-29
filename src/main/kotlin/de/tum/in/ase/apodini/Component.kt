package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.internal.ModifiedComponent
import de.tum.`in`.ase.apodini.modifiers.ModifiableComponent
import de.tum.`in`.ase.apodini.modifiers.Modifier

interface Component: ModifiableComponent<Component> {
    @ComponentDsl
    operator fun ComponentBuilder.invoke()

    override fun modify(modifier: Modifier): Component {
        return ModifiedComponent(this, modifier)
    }
}