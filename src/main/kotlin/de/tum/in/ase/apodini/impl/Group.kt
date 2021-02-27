package de.tum.`in`.ase.apodini.impl

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.ComponentDsl
import de.tum.`in`.ase.apodini.internal.ComponentVisitor
import de.tum.`in`.ase.apodini.internal.InternalComponent
import de.tum.`in`.ase.apodini.properties.PathParameter

@ComponentDsl
fun ComponentBuilder.group(name: String, init: ComponentBuilder.() -> Unit) {
    +Group(Group.Kind.Named(name), init)
}

@ComponentDsl
fun ComponentBuilder.group(vararg names: String, init: ComponentBuilder.() -> Unit) {
    group(names.asList(), init)
}

@ComponentDsl
private fun ComponentBuilder.group(names: Collection<String>, init: ComponentBuilder.() -> Unit) {
    if (names.isEmpty()) {
        init()
    } else {
        group(names.first()) {
            group(names.drop(1), init)
        }
    }
}

@ComponentDsl
fun <T : Any> ComponentBuilder.group(parameter: PathParameter<T>, init: ComponentBuilder.() -> Unit) {
    +Group(Group.Kind.Parameter(parameter), init)
}

@ComponentDsl
fun <A : Any> ComponentBuilder.group(name: String, parameter: PathParameter<A>, init: ComponentBuilder.() -> Unit) {
    group(name) {
        group(parameter) {
            init()
        }
    }
}

@ComponentDsl
fun <A : Any> ComponentBuilder.group(parameter: PathParameter<A>, name: String, init: ComponentBuilder.() -> Unit) {
    group(parameter) {
        group(name) {
            init()
        }
    }
}

@ComponentDsl
fun <A : Any, B : Any> ComponentBuilder.group(parameterA: PathParameter<A>, parameterB: PathParameter<B>, init: ComponentBuilder.() -> Unit) {
    group(parameterA) {
        group(parameterB) {
            init()
        }
    }
}

private class Group(
    private val kind: Kind,
    private val init: ComponentBuilder.() -> Unit
): InternalComponent() {
    sealed class Kind {
        class Named(val value: String) : Kind() {
            override fun asComponentVisitorGroup(): ComponentVisitor.Group {
                return ComponentVisitor.Group.Named(value)
            }
        }

        class Parameter<T : Any>(val value: PathParameter<T>) : Kind() {
            override fun asComponentVisitorGroup(): ComponentVisitor.Group {
                return ComponentVisitor.Group.Parameter(value)
            }
        }

        abstract fun asComponentVisitorGroup(): ComponentVisitor.Group
    }

    override fun ComponentVisitor.visit() {
        enterGroup(kind.asComponentVisitorGroup())
        init()
        exitGroup()
    }

}