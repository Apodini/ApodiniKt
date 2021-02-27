package de.tum.`in`.ase.apodini.properties

import de.tum.`in`.ase.apodini.internal.PropertyCollector
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import de.tum.`in`.ase.apodini.properties.options.OptionsBuilder
import de.tum.`in`.ase.apodini.request.Request
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> parameter(
        name: String? = null,
        noinline init: OptionsBuilder<Parameter<T>>.() -> Unit = {}
): Parameter<T> {
    return parameter(name, typeOf<T>(), init)
}

@PublishedApi
internal fun <T : Any> parameter(
        name: String? = null,
        type: KType,
        init: OptionsBuilder<Parameter<T>>.() -> Unit = {}
): Parameter<T> {
    return Parameter(name, type, OptionSet(init))
}

class Parameter<T : Any> internal constructor(
        private val name: String?,
        private val type: KType,
        private val options: OptionSet<Parameter<T>>
): RequestInjectable {
    private val id = UUID.randomUUID()
    lateinit var value: T

    override fun PropertyCollector.collect(property: KProperty<*>) {
        @Suppress("UNCHECKED_CAST")
        registerParameter(id, name ?: property.name, type, options as OptionSet<Parameter<*>>)
    }

    override fun inject(request: Request) {
        value = request.parameter(id)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}