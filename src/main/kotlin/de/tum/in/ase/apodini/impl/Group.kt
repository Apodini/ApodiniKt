package de.tum.`in`.ase.apodini.impl

import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.internal.ComponentVisitor
import de.tum.`in`.ase.apodini.internal.InternalComponent
import de.tum.`in`.ase.apodini.properties.PathParameter

fun ComponentBuilder.group(name: String, init: ComponentBuilder.() -> Unit) {
    +Group(Group.Kind.Named(name), init)
}

fun ComponentBuilder.group(vararg names: String, init: ComponentBuilder.() -> Unit) {
    group(names.asList(), init)
}

private fun ComponentBuilder.group(names: Collection<String>, init: ComponentBuilder.() -> Unit) {
    if (names.isEmpty()) {
        init()
    } else {
        group(names.first()) {
            group(names.drop(1), init)
        }
    }
}

fun ComponentBuilder.group(parameter: PathParameter, init: ComponentBuilder.() -> Unit) {
    +Group(Group.Kind.Parameter(parameter), init)
}

fun ComponentBuilder.group(c0: String, c1: PathParameter, init: ComponentBuilder.() -> Unit) {
    group(c0) {
        group(c1) {
            init()
        }
    }
}

fun ComponentBuilder.group(c0: PathParameter, c1: String, init: ComponentBuilder.() -> Unit) {
    group(c0) {
        group(c1) {
            init()
        }
    }
}

fun ComponentBuilder.group(c0: PathParameter, c1: PathParameter, init: ComponentBuilder.() -> Unit) {
    group(c0) {
        group(c1) {
            init()
        }
    }
}

fun ComponentBuilder.group(c0: String, c1: PathParameter, c2: PathParameter, init: ComponentBuilder.() -> Unit) {
    group(c0, c1) {
        group(c2) {
            init()
        }
    }
}

fun ComponentBuilder.group(c0: PathParameter, c1: String, c2: PathParameter, init: ComponentBuilder.() -> Unit) {
    group(c0, c1) {
        group(c2) {
            init()
        }
    }
}

fun ComponentBuilder.group(c0: PathParameter, c1: PathParameter, c2: String, init: ComponentBuilder.() -> Unit) {
    group(c0, c1) {
        group(c2) {
            init()
        }
    }
}

fun ComponentBuilder.group(c0: String, c1: String, c2: PathParameter, init: ComponentBuilder.() -> Unit) {
    group(c0, c1) {
        group(c2) {
            init()
        }
    }
}

fun ComponentBuilder.group(c0: String, c1: PathParameter, c2: String, init: ComponentBuilder.() -> Unit) {
    group(c0, c1) {
        group(c2) {
            init()
        }
    }
}

fun ComponentBuilder.group(c0: PathParameter, c1: String, c2: String, init: ComponentBuilder.() -> Unit) {
    group(c0, c1) {
        group(c2) {
            init()
        }
    }
}

fun ComponentBuilder.group(c0: PathParameter, c1: PathParameter, c2: PathParameter, init: ComponentBuilder.() -> Unit) {
    group(c0, c1) {
        group(c2) {
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

        class Parameter(val value: PathParameter) : Kind() {
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