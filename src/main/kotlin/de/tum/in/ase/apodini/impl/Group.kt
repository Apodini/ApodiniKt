package de.tum.`in`.ase.apodini.impl

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.ComponentDsl
import de.tum.`in`.ase.apodini.internal.ComponentVisitor
import de.tum.`in`.ase.apodini.internal.InternalComponent

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