package de.tum.`in`.ase.apodini.modifiers

import de.tum.`in`.ase.apodini.Component
import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentBuilder
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.internal.ComponentVisitor
import de.tum.`in`.ase.apodini.internal.InternalComponent

fun <T : ModifiableComponent<T>> ModifiableComponent<T>.withEnvironment(init: EnvironmentBuilder.() -> Unit): T {
    return modify(EnvironmentModifier(EnvironmentStore(init)))
}

private class EnvironmentModifier(
    private val store: EnvironmentStore
) : Modifier {
    override fun ComponentBuilder.wrap(component: Component) {
        +EnvironmentComponent(store) {
            +component
        }
    }
}

private class EnvironmentComponent(
    private val store: EnvironmentStore,
    private val init: ComponentBuilder.() -> Unit
) : InternalComponent() {
    override fun ComponentVisitor.visit() {
        enterGroup(ComponentVisitor.Group.Environment(store))
        init()
        exitGroup()
    }
}