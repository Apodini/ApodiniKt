package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.internal.HandlerWrapper
import de.tum.`in`.ase.apodini.internal.ModifiedComponent
import de.tum.`in`.ase.apodini.modifiers.ModifiableComponent
import de.tum.`in`.ase.apodini.modifiers.Modifier
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface Component: ModifiableComponent<Component> {
    @ComponentDsl
    operator fun ComponentBuilder.invoke()

    override fun modify(modifier: Modifier): Component {
        return ModifiedComponent(this, modifier)
    }

    companion object {
        operator fun invoke(init: ComponentBuilder.() -> Unit): Component {
            return AnyComponent(init)
        }

        @PublishedApi
        internal operator fun <T> invoke(handler: Handler<T>, returnType: KType): Component {
            return HandlerWrapper(handler, returnType)
        }

        @ExperimentalStdlibApi
        inline operator fun <reified T> invoke(handler: Handler<T>): Component {
            return invoke(handler, typeOf<T>())
        }
    }
}

private class AnyComponent(val init: ComponentBuilder.() -> Unit) : Component {
    override fun ComponentBuilder.invoke() {
        init()
    }
}