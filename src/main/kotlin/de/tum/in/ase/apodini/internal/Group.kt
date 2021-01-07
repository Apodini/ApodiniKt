package de.tum.`in`.ase.apodini.internal

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.ComponentDsl

@ComponentDsl
fun ComponentBuilder.group(name: String, init: ComponentBuilder.() -> Unit) {
    +Group(name, init)
}

private class Group(
    private val name: String,
    private val init: ComponentBuilder.() -> Unit
): InternalComponent() {

    override fun ComponentVisitor.visit() {
        enterGroup(ComponentVisitor.Group.Named(name))
        init()
        exitGroup()
    }

}