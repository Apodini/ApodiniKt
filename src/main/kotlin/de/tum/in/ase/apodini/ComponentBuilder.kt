package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.modifiers.ModifiableComponent
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@ComponentDsl
abstract class ComponentBuilder {
    protected abstract fun add(component: Component): ModifiableComponent<*>

    @PublishedApi
    internal abstract fun <T> add(handler: Handler<T>, returnType: KType): ModifiableComponent<*>

    operator fun Component.unaryPlus(): ModifiableComponent<*> {
        return add(this)
    }

    @OptIn(ExperimentalStdlibApi::class)
    inline operator fun <reified T> Handler<T>.unaryPlus(): ModifiableComponent<*> {
        return add(this, typeOf<T>())
    }
}