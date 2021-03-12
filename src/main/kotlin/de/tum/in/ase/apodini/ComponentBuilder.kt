package de.tum.`in`.ase.apodini

import kotlin.reflect.KType
import kotlin.reflect.typeOf

@ComponentDsl
abstract class ComponentBuilder {
    protected abstract fun add(component: Component)

    @PublishedApi
    internal abstract fun <T> add(handler: Handler<T>, returnType: KType)

    operator fun Component.unaryPlus() {
        add(this)
    }

    @OptIn(ExperimentalStdlibApi::class)
    inline operator fun <reified T> Handler<T>.unaryPlus() {
        add(this, typeOf<T>())
    }
}