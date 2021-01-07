package de.tum.`in`.ase.apodini.internal

import de.tum.`in`.ase.apodini.Component
import de.tum.`in`.ase.apodini.ComponentBuilder

internal abstract class InternalComponent : Component {
    final override fun ComponentBuilder.invoke() {
        throw IllegalArgumentException(
            "Unexpected call to `invoke` to decompose a Leaf. Do not call `InternalComponent.invoke()` directly."
        )
    }

    abstract fun ComponentVisitor.visit()
}